package com.gregguns.questbook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.gregguns.entity.npc.NpcMob;
import com.gregguns.item.NeiOrder;
import com.gregguns.machine.GregGunsMTEIDs;

/**
 * Checks the quest line that ships inside the jar ({@code assets/gregguns/questbook}, built by
 * {@code tools/questbook/build_quests.py}) with plain JSON parsing: nothing here loads BetterQuesting. A broken
 * resource would otherwise only show up when a server owner turns the install on.
 */
// Spec: docs/gregguns-spec/12-questbook/index.md (GGS-12-02, 05, 06, 10, 15, 16)
class QuestbookResourcesTest {

    private static final String ROOT = "assets/gregguns/questbook/";
    private static final UUID NAMESPACE = UUID.fromString("6f1f3c52-9d0a-4b6e-8a53-2c7e0f4b91d8");
    private static final String GT_MACHINES = "gregtech:gt.blockmachines";
    private static final Set<String> BAGS = new HashSet<>(java.util.Arrays.asList("enhancedlootbags:lootbag"));

    /** A quest as the test needs it. */
    private static final class Quest {

        String file;
        long high;
        long low;
        JsonObject json;
        String title;
    }

    private static JsonObject index;
    private static final List<Quest> QUESTS = new ArrayList<>();
    private static final Map<String, JsonObject> POSITIONS = new HashMap<>();

    @BeforeAll
    static void load() throws IOException {
        index = readJson("index.json");
        for (JsonElement e : index.getAsJsonArray("quests")) {
            Quest q = new Quest();
            q.file = e.getAsString();
            q.json = readJson(q.file);
            q.high = q.json.get("questIDHigh:4")
                .getAsLong();
            q.low = q.json.get("questIDLow:4")
                .getAsLong();
            q.title = q.json.getAsJsonObject("properties:10")
                .getAsJsonObject("betterquesting:10")
                .get("name:8")
                .getAsString();
            QUESTS.add(q);
        }
        for (JsonElement e : index.getAsJsonArray("linePositions")) {
            POSITIONS.put(e.getAsString(), readJson(e.getAsString()));
        }
    }

    private static JsonObject readJson(String path) throws IOException {
        try (InputStream in = QuestbookResourcesTest.class.getClassLoader()
            .getResourceAsStream(ROOT + path)) {
            assertNotNull(in, "resource listed but missing: " + path);
            return new JsonParser().parse(new InputStreamReader(in, StandardCharsets.UTF_8))
                .getAsJsonObject();
        }
    }

    /** RFC 4122 version 5, the same as Python's {@code uuid.uuid5}. */
    private static UUID uuid5(UUID namespace, String name) throws Exception {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        sha1.update(
            ByteBuffer.allocate(16)
                .putLong(namespace.getMostSignificantBits())
                .putLong(namespace.getLeastSignificantBits())
                .array());
        sha1.update(name.getBytes(StandardCharsets.UTF_8));
        byte[] h = sha1.digest();
        h[6] = (byte) ((h[6] & 0x0f) | 0x50);
        h[8] = (byte) ((h[8] & 0x3f) | 0x80);
        ByteBuffer b = ByteBuffer.wrap(h, 0, 16);
        return new UUID(b.getLong(), b.getLong());
    }

    private static String b64(UUID id) {
        byte[] b = ByteBuffer.allocate(16)
            .putLong(id.getMostSignificantBits())
            .putLong(id.getLeastSignificantBits())
            .array();
        return Base64.getUrlEncoder()
            .encodeToString(b);
    }

    @Test
    void indexListsEveryFileAndEveryListedFileExists() throws IOException {
        assertEquals(
            1,
            index.get("format")
                .getAsInt());
        assertTrue(
            index.get("orderEntry")
                .getAsString()
                .endsWith(": GregGuns"));
        List<String> all = new ArrayList<>();
        all.add(
            index.get("line")
                .getAsString());
        for (JsonElement e : index.getAsJsonArray("linePositions")) {
            all.add(e.getAsString());
        }
        for (JsonElement e : index.getAsJsonArray("quests")) {
            all.add(e.getAsString());
        }
        for (String path : all) {
            assertNotNull(
                QuestbookResourcesTest.class.getClassLoader()
                    .getResource(ROOT + path),
                "missing: " + path);
        }
        assertEquals(new HashSet<>(all).size(), all.size(), "a file is listed twice");
        // The other direction: nothing on disk under the resource root is unlisted.
        Set<String> listed = new HashSet<>(all);
        listed.add("index.json");
        java.nio.file.Path root = Paths.get("src/main/resources/" + ROOT);
        try (java.util.stream.Stream<java.nio.file.Path> walk = Files.walk(root)) {
            walk.filter(Files::isRegularFile)
                .forEach(p -> {
                    String rel = root.relativize(p)
                        .toString()
                        .replace('\\', '/');
                    assertTrue(listed.contains(rel), "file not listed in index.json: " + rel);
                });
        }
        assertEquals(QUESTS.size(), POSITIONS.size(), "one position file per quest");
    }

    @Test
    void questCountMatchesTheSpec() {
        assertEquals(121, QUESTS.size(), "update docs/gregguns-spec/12-questbook when the count changes");
    }

    @Test
    void fileNamesCarryTheIdAndIdsAreUuid5OfTheKeys() throws Exception {
        // The data file's keys are not in the resources, so check what the resources do carry: the Base64 suffix is
        // the stored id, and the id is a version-5 UUID in the GregGuns namespace's family.
        Set<String> seen = new HashSet<>();
        for (Quest q : QUESTS) {
            UUID id = new UUID(q.high, q.low);
            assertEquals(5, id.version(), q.file);
            assertTrue(q.file.endsWith("-" + b64(id) + ".json"), "file name does not carry the id: " + q.file);
            assertTrue(seen.add(q.file), "duplicate id " + q.file);
        }
        JsonObject line = readJson(
            index.get("line")
                .getAsString());
        UUID lineId = uuid5(NAMESPACE, "line:gregguns");
        assertEquals(
            lineId.getMostSignificantBits(),
            line.get("questLineIDHigh:4")
                .getAsLong());
        assertEquals(
            lineId.getLeastSignificantBits(),
            line.get("questLineIDLow:4")
                .getAsLong());
        assertEquals("D4B4PFfzWT2Q4DUgPjN9oQ==", b64(lineId));
        // Keys never change (GGS-12-02): a sample of the keys pinned in authoring.md must still hash to its ids.
        Map<String, String> pinned = new HashMap<>();
        pinned.put("gg.lv.ammo_9mm", "Mz-uO4DjXsKUCga3SdfpiQ==");
        pinned.put("gg.lv.pistol", "mJR67SnRUAyj7d-tQP9low==");
        Set<String> names = new HashSet<>();
        for (Quest q : QUESTS) {
            names.add(q.file.substring(q.file.length() - 5 - 24, q.file.length() - 5));
        }
        for (Map.Entry<String, String> e : pinned.entrySet()) {
            assertEquals(e.getValue(), b64(uuid5(NAMESPACE, e.getKey())));
            assertTrue(names.contains(e.getValue()), "pinned quest missing: " + e.getKey());
        }
    }

    @Test
    void idsAreUuid5OfTheDataKeys() throws Exception {
        JsonObject data = new JsonParser()
            .parse(new String(Files.readAllBytes(Paths.get("tools/questbook/quests.json")), StandardCharsets.UTF_8))
            .getAsJsonObject();
        assertEquals(
            NAMESPACE.toString(),
            data.get("namespace")
                .getAsString());
        Set<String> expected = new HashSet<>();
        for (JsonElement e : data.getAsJsonArray("quests")) {
            UUID id = uuid5(
                NAMESPACE,
                e.getAsJsonObject()
                    .get("key")
                    .getAsString());
            expected.add(id.getMostSignificantBits() + "/" + id.getLeastSignificantBits());
        }
        Set<String> actual = new HashSet<>();
        for (Quest q : QUESTS) {
            actual.add(q.high + "/" + q.low);
        }
        assertEquals(expected, actual, "resources are stale: rerun tools/questbook/build_quests.py");
    }

    /** The ids of the pack quests that index.md says GregGuns may depend on. */
    private static Set<Long> packQuestIds() throws IOException {
        String md = new String(
            Files.readAllBytes(Paths.get("docs/gregguns-spec/12-questbook/index.md")),
            StandardCharsets.UTF_8);
        String section = md.substring(md.indexOf("## Referenced pack quests"));
        section = section.substring(0, section.indexOf("## Integration map"));
        Set<Long> ids = new HashSet<>();
        Matcher m = Pattern.compile("(?m)^\\| (\\d+) \\|")
            .matcher(section);
        while (m.find()) {
            ids.add(Long.parseLong(m.group(1)));
        }
        assertFalse(ids.isEmpty());
        return ids;
    }

    @Test
    void everyPrerequisiteResolvesAndThereIsNoCycle() throws IOException {
        Map<String, Quest> byId = new HashMap<>();
        for (Quest q : QUESTS) {
            byId.put(q.high + "/" + q.low, q);
        }
        Set<Long> pack = packQuestIds();
        Map<Quest, List<Quest>> graph = new HashMap<>();
        for (Quest q : QUESTS) {
            List<Quest> deps = new ArrayList<>();
            for (Map.Entry<String, JsonElement> e : q.json.getAsJsonObject("preRequisites:9")
                .entrySet()) {
                JsonObject p = e.getValue()
                    .getAsJsonObject();
                long hi = p.get("questIDHigh:4")
                    .getAsLong();
                long lo = p.get("questIDLow:4")
                    .getAsLong();
                if (hi == 0) {
                    assertTrue(pack.contains(lo), q.title + ": pack quest " + lo + " is not in the index.md table");
                } else {
                    Quest dep = byId.get(hi + "/" + lo);
                    assertNotNull(dep, q.title + ": prerequisite is not a GregGuns quest in the set");
                    deps.add(dep);
                }
            }
            graph.put(q, deps);
        }
        Map<Quest, Integer> state = new HashMap<>();
        for (Quest q : QUESTS) {
            visit(q, graph, state);
        }
    }

    private static void visit(Quest q, Map<Quest, List<Quest>> graph, Map<Quest, Integer> state) {
        Integer s = state.get(q);
        if (s != null) {
            assertFalse(s == 1, "prerequisite cycle through " + q.title);
            return;
        }
        state.put(q, 1);
        for (Quest d : graph.get(q)) {
            visit(d, graph, state);
        }
        state.put(q, 2);
    }

    /** Every {@code id:8} in the quest's tasks and rewards (the icon is checked too). */
    private static void collectIds(JsonElement e, List<JsonObject> stacks) {
        if (e.isJsonObject()) {
            JsonObject o = e.getAsJsonObject();
            if (o.has("id:8") && o.has("Damage:2")) {
                stacks.add(o);
            }
            for (Map.Entry<String, JsonElement> c : o.entrySet()) {
                collectIds(c.getValue(), stacks);
            }
        } else if (e.isJsonArray()) {
            for (JsonElement c : (JsonArray) e) {
                collectIds(c, stacks);
            }
        }
    }

    /** The GT machine ids GregGuns registers: every public int constant of {@link GregGunsMTEIDs} in its block. */
    private static Set<Integer> registeredMachineIds() throws IllegalAccessException {
        Set<Integer> ids = new HashSet<>();
        for (Field f : GregGunsMTEIDs.class.getFields()) {
            if (Modifier.isStatic(f.getModifiers()) && f.getType() == int.class
                && !f.getName()
                    .startsWith("BLOCK_")) {
                ids.add(f.getInt(null));
            }
        }
        return ids;
    }

    @Test
    void everyGregGunsItemEntityAndMachineIsRegistered() throws Exception {
        Set<String> names = new HashSet<>(NeiOrder.names());
        Set<Integer> machines = registeredMachineIds();
        Set<String> entities = new HashSet<>();
        for (NpcMob mob : NpcMob.values()) {
            if (!mob.isDevOnly()) {
                entities.add(mob.fullName());
            }
        }
        for (Quest q : QUESTS) {
            List<JsonObject> stacks = new ArrayList<>();
            collectIds(q.json, stacks);
            assertFalse(stacks.isEmpty(), q.title + " has no item at all (not even an icon)");
            for (JsonObject s : stacks) {
                String id = s.get("id:8")
                    .getAsString();
                if (id.startsWith("gregguns:")) {
                    assertTrue(names.contains(id.substring(9)), q.title + ": not a registered GregGuns name: " + id);
                } else if (id.equals(GT_MACHINES)) {
                    int meta = s.get("Damage:2")
                        .getAsInt();
                    assertTrue(machines.contains(meta), q.title + ": not a GregGuns machine id: " + meta);
                } else {
                    assertTrue(id.equals("minecraft:stick") || BAGS.contains(id), q.title + ": unexpected item " + id);
                }
            }
            for (Map.Entry<String, JsonElement> t : q.json.getAsJsonObject("tasks:9")
                .entrySet()) {
                JsonObject task = t.getValue()
                    .getAsJsonObject();
                if (task.get("taskID:8")
                    .getAsString()
                    .equals("bq_standard:hunt")) {
                    String target = task.get("target:8")
                        .getAsString();
                    assertTrue(
                        target.equals("Zombie") || entities.contains(target),
                        q.title + ": not a registered entity: " + target);
                    assertEquals(
                        0,
                        task.get("subtypes:1")
                            .getAsInt());
                }
            }
        }
    }

    @Test
    void noTwoPositionBoxesOverlap() {
        List<String> files = new ArrayList<>(POSITIONS.keySet());
        for (int i = 0; i < files.size(); i++) {
            JsonObject a = POSITIONS.get(files.get(i));
            for (int j = i + 1; j < files.size(); j++) {
                JsonObject b = POSITIONS.get(files.get(j));
                int ax = a.get("x:3")
                    .getAsInt();
                int ay = a.get("y:3")
                    .getAsInt();
                int bx = b.get("x:3")
                    .getAsInt();
                int by = b.get("y:3")
                    .getAsInt();
                int aw = a.get("sizeX:3")
                    .getAsInt();
                int ah = a.get("sizeY:3")
                    .getAsInt();
                int bw = b.get("sizeX:3")
                    .getAsInt();
                int bh = b.get("sizeY:3")
                    .getAsInt();
                boolean overlap = ax < bx + bw && bx < ax + aw && ay < by + bh && by < ay + ah;
                assertFalse(overlap, "boxes overlap: " + files.get(i) + " / " + files.get(j));
            }
        }
    }

    @Test
    void everyPositionFileBelongsToAQuest() {
        Set<String> questIds = new HashSet<>();
        for (Quest q : QUESTS) {
            questIds.add(q.high + "/" + q.low);
        }
        for (Map.Entry<String, JsonObject> e : POSITIONS.entrySet()) {
            String id = e.getValue()
                .get("questIDHigh:4")
                .getAsLong() + "/"
                + e.getValue()
                    .get("questIDLow:4")
                    .getAsLong();
            assertTrue(questIds.contains(id), "position file for an unknown quest: " + e.getKey());
        }
    }
}
