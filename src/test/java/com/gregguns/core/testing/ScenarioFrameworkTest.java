package com.gregguns.core.testing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import org.junit.jupiter.api.Test;

class ScenarioFrameworkTest {

    /** A scenario whose behaviour is a lambda. */
    private static final class Fake implements Scenario {

        private final String name;
        private final Function<ScenarioContext, ScenarioResult> body;

        Fake(String name, Function<ScenarioContext, ScenarioResult> body) {
            this.name = name;
            this.body = body;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public String description() {
            return "fake " + name;
        }

        @Override
        public ScenarioResult run(ScenarioContext context) {
            return body.apply(context);
        }
    }

    /** A context with no world that counts cleanups. */
    private static final class StubContext implements ScenarioContext {

        int cleanups;
        boolean cleanupThrows;

        @Override
        public World world() {
            return null;
        }

        @Override
        public int originX() {
            return 0;
        }

        @Override
        public int originY() {
            return 0;
        }

        @Override
        public int originZ() {
            return 0;
        }

        @Override
        public EntityPlayer player() {
            return null;
        }

        @Override
        public Random random() {
            return new Random(1);
        }

        @Override
        public <T extends Entity> T spawn(T entity) {
            return entity;
        }

        @Override
        public void despawn(Entity entity) {}

        @Override
        public void clearArea(int radius, int height) {}

        @Override
        public void setBlock(int dx, int dy, int dz, Block block, int meta) {}

        @Override
        public void cleanup() {
            cleanups++;
            if (cleanupThrows) {
                throw new IllegalStateException("boom");
            }
        }
    }

    private static Scenario passing(String name) {
        return new Fake(name, c -> ScenarioResult.pass(name));
    }

    private static Scenario failing(String name) {
        return new Fake(name, c -> ScenarioResult.fail(name, "nope"));
    }

    @Test
    void builderFailsOnFirstFailedCheckAndKeepsEveryMessage() {
        ScenarioResult r = ScenarioResult.builder("x")
            .check(true, "unused")
            .check(false, "first")
            .checkEquals("health", 10, 9)
            .note("fyi")
            .build();
        assertFalse(r.passed());
        assertEquals(Arrays.asList("first", "health: expected <10> but was <9>", "fyi"), r.messages());
    }

    @Test
    void builderWithOnlyPassingChecksPasses() {
        ScenarioResult r = ScenarioResult.builder("x")
            .checkEquals("a", "b", "b")
            .build();
        assertTrue(r.passed());
        assertTrue(
            r.messages()
                .isEmpty());
    }

    @Test
    void registryRejectsDuplicatesReservedAndInvalidNames() {
        ScenarioRegistry reg = new ScenarioRegistry();
        reg.register(passing("a.b"));
        assertThrows(IllegalArgumentException.class, () -> reg.register(passing("a.b")));
        assertThrows(IllegalArgumentException.class, () -> reg.register(passing("all")));
        assertThrows(IllegalArgumentException.class, () -> reg.register(passing("list")));
        assertThrows(IllegalArgumentException.class, () -> reg.register(passing("Bad Name")));
        assertEquals(1, reg.size());
    }

    @Test
    void registryListsInNameOrder() {
        ScenarioRegistry reg = new ScenarioRegistry();
        reg.register(passing("zeta"));
        reg.register(passing("alpha"));
        assertEquals(
            "alpha",
            reg.all()
                .get(0)
                .name());
        assertEquals(
            "zeta",
            reg.all()
                .get(1)
                .name());
    }

    @Test
    void requestParsesKeywordsNamesAndUsage() {
        ScenarioRegistry reg = new ScenarioRegistry();
        Scenario s = passing("gun.fire");
        reg.register(s);
        assertEquals(
            ScenarioRequest.Kind.USAGE,
            ScenarioRequest.parse(new String[0], reg)
                .kind());
        assertEquals(
            ScenarioRequest.Kind.USAGE,
            ScenarioRequest.parse(new String[] { "a", "b" }, reg)
                .kind());
        assertEquals(
            ScenarioRequest.Kind.LIST,
            ScenarioRequest.parse(new String[] { "LIST" }, reg)
                .kind());
        assertEquals(
            ScenarioRequest.Kind.ALL,
            ScenarioRequest.parse(new String[] { "all" }, reg)
                .kind());
        ScenarioRequest named = ScenarioRequest.parse(new String[] { "gun.fire" }, reg);
        assertEquals(ScenarioRequest.Kind.NAMED, named.kind());
        assertSame(s, named.scenario());
        ScenarioRequest unknown = ScenarioRequest.parse(new String[] { "nope" }, reg);
        assertEquals(ScenarioRequest.Kind.UNKNOWN, unknown.kind());
        assertEquals("nope", unknown.argument());
        assertNull(unknown.scenario());
    }

    @Test
    void completionOffersKeywordsAndNamesByPrefix() {
        ScenarioRegistry reg = new ScenarioRegistry();
        reg.register(passing("armor.set"));
        reg.register(passing("gun.fire"));
        assertEquals(
            Arrays.asList("all", "list", "armor.set", "gun.fire"),
            ScenarioRequest.complete(new String[] { "" }, reg));
        assertEquals(Arrays.asList("all", "armor.set"), ScenarioRequest.complete(new String[] { "A" }, reg));
        assertEquals(Collections.emptyList(), ScenarioRequest.complete(new String[] { "gun.fire", "x" }, reg));
    }

    @Test
    void runnerAggregatesAndFormatsAGreppableSummary() {
        ScenarioRunner runner = new ScenarioRunner(s -> new StubContext());
        ScenarioSummary summary = runner.run(Arrays.asList(passing("a"), failing("b"), passing("c")));
        assertEquals(2, summary.passed());
        assertEquals(1, summary.failed());
        assertFalse(summary.allPassed());
        assertEquals("[GregGuns] Scenarios: 2 passed, 1 failed", summary.summaryLine());
        List<String> lines = summary.reportLines();
        assertEquals(
            Arrays.asList(
                "[GregGuns] PASS a",
                "[GregGuns] FAIL b",
                "[GregGuns]   nope",
                "[GregGuns] PASS c",
                "[GregGuns] Scenarios: 2 passed, 1 failed"),
            lines);
    }

    @Test
    void emptyRunPassesWithZeroCounts() {
        ScenarioSummary summary = new ScenarioRunner(s -> new StubContext()).run(new ArrayList<Scenario>());
        assertTrue(summary.allPassed());
        assertEquals("[GregGuns] Scenarios: 0 passed, 0 failed", summary.summaryLine());
    }

    @Test
    void runnerCleansUpEvenWhenTheScenarioThrowsAndKeepsGoing() {
        List<StubContext> contexts = new ArrayList<>();
        ScenarioRunner runner = new ScenarioRunner(s -> {
            StubContext c = new StubContext();
            contexts.add(c);
            return c;
        });
        Scenario thrower = new Fake("thrower", c -> { throw new IllegalStateException("bad"); });
        ScenarioSummary summary = runner.run(Arrays.asList(thrower, passing("after")));
        assertEquals(1, summary.failed());
        assertTrue(
            summary.results()
                .get(0)
                .messages()
                .get(0)
                .contains("bad"));
        assertTrue(
            summary.results()
                .get(1)
                .passed());
        assertEquals(1, contexts.get(0).cleanups);
        assertEquals(1, contexts.get(1).cleanups);
    }

    @Test
    void runnerFailsAScenarioWhoseCleanupThrows() {
        ScenarioRunner runner = new ScenarioRunner(s -> {
            StubContext c = new StubContext();
            c.cleanupThrows = true;
            return c;
        });
        ScenarioSummary summary = runner.run(Collections.singletonList(passing("a")));
        assertEquals(1, summary.failed());
        assertTrue(
            summary.results()
                .get(0)
                .messages()
                .get(0)
                .contains("cleanup"));
    }

    @Test
    void runnerFailsANullResult() {
        ScenarioRunner runner = new ScenarioRunner(s -> new StubContext());
        ScenarioSummary summary = runner.run(Collections.<Scenario>singletonList(new Fake("n", c -> null)));
        assertEquals(1, summary.failed());
    }
}
