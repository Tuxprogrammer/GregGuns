package com.gregguns.compat.questbook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;

/** Index parsing, path resolution, the upsert plan, idempotency and tab-order insertion over a 3-quest fixture. */
class QuestbookInstallerTest {

    private static final String FIXTURE_ROOT = "fixture/questbook/";
    private static final UUID LINE = new UUID(1, 2);
    private static final UUID Q1 = new UUID(7, 1);
    private static final UUID Q2 = new UUID(7, 2);
    private static final UUID Q3 = new UUID(7, 3);
    private static final UUID PACK_QUEST = new UUID(0, 88);
    private static final UUID PACK_LINE = new UUID(0, 5);

    /** An in-memory database that records what was written. */
    private static final class FakeTarget implements QuestbookTarget {

        final Map<UUID, JsonObject> quests = new HashMap<>();
        final Map<UUID, JsonObject> lines = new HashMap<>();
        final Map<UUID, Map<UUID, JsonObject>> linePositions = new HashMap<>();
        List<UUID> order = new ArrayList<>();
        int saves;
        int syncs;
        int orderSets;

        @Override
        public Set<UUID> questIds() {
            return new HashSet<>(quests.keySet());
        }

        @Override
        public Set<UUID> lineIds() {
            return new HashSet<>(lines.keySet());
        }

        @Override
        public List<UUID> lineOrder() {
            return new ArrayList<>(order);
        }

        @Override
        public void putQuest(UUID id, JsonObject definition) {
            quests.put(id, definition);
        }

        @Override
        public void putLine(UUID id, JsonObject definition, Map<UUID, JsonObject> positions) {
            lines.put(id, definition);
            linePositions.put(id, positions);
        }

        @Override
        public void setLineOrder(List<UUID> newOrder) {
            order = new ArrayList<>(newOrder);
            orderSets++;
        }

        @Override
        public void save() {
            saves++;
        }

        @Override
        public void sync() {
            syncs++;
        }
    }

    private static QuestbookBundle fixture() throws QuestbookException {
        QuestbookBundle bundle = QuestbookBundle
            .load(new QuestbookSource.Classpath(QuestbookInstallerTest.class.getClassLoader(), FIXTURE_ROOT));
        assertNotNull(bundle);
        return bundle;
    }

    private static FakeTarget packWithOtherContent() {
        FakeTarget target = new FakeTarget();
        target.quests.put(PACK_QUEST, new JsonObject());
        target.lines.put(PACK_LINE, new JsonObject());
        target.order.add(PACK_LINE);
        return target;
    }

    @Test
    void indexParsesAndNamesTheFiles() throws Exception {
        QuestbookIndex index = fixture().index;
        assertEquals("QuestLines/Fixture-AAAA/QuestLine.json", index.line);
        assertEquals(3, index.linePositions.size());
        assertEquals(3, index.quests.size());
        assertEquals("AAAAAAAAAAEAAAAAAAAAAg==", index.orderLineToken());
    }

    @Test
    void bundleReadsFixedIdsFromTheTypedLongKeys() throws Exception {
        QuestbookBundle bundle = fixture();
        assertEquals(LINE, bundle.lineId);
        assertEquals(Arrays.asList(Q1, Q2, Q3), new ArrayList<>(bundle.quests.keySet()));
        assertEquals(new ArrayList<>(bundle.quests.keySet()), new ArrayList<>(bundle.positions.keySet()));
        assertEquals("AAAAAAAAAAEAAAAAAAAAAg==", QuestbookBundle.token(LINE));
    }

    @Test
    void badIndexesAreRejected() {
        assertThrows(QuestbookException.class, () -> QuestbookIndex.parse("not json"));
        assertThrows(QuestbookException.class, () -> QuestbookIndex.parse("[]"));
        assertThrows(QuestbookException.class, () -> QuestbookIndex.parse("{\"format\":2,\"line\":\"x\"}"));
        assertThrows(QuestbookException.class, () -> QuestbookIndex.parse("{\"format\":1}"));
    }

    @Test
    void pathsResolveUnderTheRootAndCannotEscape() {
        assertEquals("a/b/Quests/x.json", QuestbookSource.Classpath.resolve("a/b/", "Quests/x.json"));
        for (String bad : new String[] { "../x.json", "Quests/../../x", "/abs.json", "a\\b.json", "" }) {
            assertThrows(IllegalArgumentException.class, () -> QuestbookSource.Classpath.resolve("r/", bad), bad);
        }
    }

    @Test
    void missingIndexMeansNoQuestbook() throws Exception {
        QuestbookSource empty = path -> null;
        assertNull(QuestbookBundle.load(empty));
    }

    @Test
    void aMissingNamedFileFailsTheWholeLoad() {
        QuestbookSource missingOne = new QuestbookSource() {

            private final QuestbookSource real = new Classpath(
                QuestbookInstallerTest.class.getClassLoader(),
                FIXTURE_ROOT);

            @Override
            public String read(String relativePath) throws IOException {
                return relativePath.contains("Beta") ? null : real.read(relativePath);
            }
        };
        QuestbookException e = assertThrows(QuestbookException.class, () -> QuestbookBundle.load(missingOne));
        assertTrue(
            e.getMessage()
                .contains("Beta"));
    }

    @Test
    void firstInstallAddsEverything() throws Exception {
        QuestbookBundle bundle = fixture();
        FakeTarget target = packWithOtherContent();
        QuestbookInstaller.Plan plan = QuestbookInstaller.plan(bundle, target);
        assertEquals(Arrays.asList(Q1, Q2, Q3), plan.addedQuests);
        assertTrue(plan.replacedQuests.isEmpty());
        assertTrue(plan.lineAdded);
        assertEquals(Arrays.asList(PACK_LINE, LINE), plan.newOrder);
    }

    @Test
    void installWritesSavesAndSyncsAndLeavesOtherContentAlone() throws Exception {
        QuestbookBundle bundle = fixture();
        FakeTarget target = packWithOtherContent();
        JsonObject packQuest = target.quests.get(PACK_QUEST);
        QuestbookInstaller.Plan plan = QuestbookInstaller.install(bundle, target);
        assertEquals(3, plan.questCount());
        assertEquals(4, target.quests.size());
        assertTrue(target.quests.containsKey(Q2));
        assertEquals(bundle.positions, target.linePositions.get(LINE));
        assertEquals(Arrays.asList(PACK_LINE, LINE), target.order);
        assertEquals(1, target.saves);
        assertEquals(1, target.syncs);
        assertEquals(packQuest, target.quests.get(PACK_QUEST));
        assertTrue(target.lines.containsKey(PACK_LINE));
    }

    @Test
    void secondInstallReplacesAndChangesNothingElse() throws Exception {
        QuestbookBundle bundle = fixture();
        FakeTarget target = packWithOtherContent();
        QuestbookInstaller.install(bundle, target);
        Set<UUID> questsAfterFirst = target.questIds();
        List<UUID> orderAfterFirst = target.lineOrder();

        QuestbookInstaller.Plan again = QuestbookInstaller.install(bundle, target);
        assertTrue(again.addedQuests.isEmpty());
        assertEquals(Arrays.asList(Q1, Q2, Q3), again.replacedQuests);
        assertFalse(again.lineAdded);
        assertNull(again.newOrder);
        assertEquals(questsAfterFirst, target.questIds());
        assertEquals(orderAfterFirst, target.lineOrder());
        assertEquals(1, target.orderSets, "the order is set once, not on every start");
        assertEquals(2, target.saves);
    }

    @Test
    void orderInsertionAppendsOnlyWhenMissing() {
        List<UUID> existing = Arrays.asList(PACK_LINE, new UUID(0, 6));
        assertEquals(Arrays.asList(PACK_LINE, new UUID(0, 6), LINE), QuestbookInstaller.insertOrder(existing, LINE));
        List<UUID> withLine = Arrays.asList(LINE, PACK_LINE);
        assertEquals(withLine, QuestbookInstaller.insertOrder(withLine, LINE));
        assertEquals(Collections.singletonList(LINE), QuestbookInstaller.insertOrder(Collections.emptyList(), LINE));
    }

    @Test
    void anAdminMovedTabKeepsItsPlace() throws Exception {
        QuestbookBundle bundle = fixture();
        FakeTarget target = packWithOtherContent();
        target.lines.put(LINE, new JsonObject());
        target.order = new ArrayList<>(Arrays.asList(LINE, PACK_LINE));
        QuestbookInstaller.install(bundle, target);
        assertEquals(Arrays.asList(LINE, PACK_LINE), target.order);
    }
}
