package com.gregguns.core.testing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import org.junit.jupiter.api.Test;

/** The multi-tick side of the scenario runner. */
class ScenarioRunTest {

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
            return name;
        }

        @Override
        public ScenarioResult run(ScenarioContext context) {
            return body.apply(context);
        }
    }

    private static final class Stub implements ScenarioContext {

        final List<String> log;
        final String owner;

        Stub(List<String> log, String owner) {
            this.log = log;
            this.owner = owner;
        }

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
        public java.util.Random random() {
            return new java.util.Random(1);
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
            log.add("cleanup " + owner);
        }
    }

    private static ScenarioRunner runner(List<String> log) {
        return new ScenarioRunner(s -> new Stub(log, s.name()));
    }

    private static Scenario waitsUntil(String name, int readyAtTick, int timeout, List<String> log) {
        return new Fake(name, ctx -> {
            log.add("start " + name);
            return ScenarioResult.pending(name, timeout, ticks -> {
                log.add("poll " + name + " " + ticks);
                return ticks >= readyAtTick ? ScenarioResult.pass(name) : null;
            });
        });
    }

    private static Scenario instant(String name, List<String> log) {
        return new Fake(name, ctx -> {
            log.add("start " + name);
            return ScenarioResult.pass(name);
        });
    }

    @Test
    void aScenarioThatLeaksConfigDoesNotAffectTheNextOne() {
        List<String> log = new ArrayList<>();
        int before = com.gregguns.core.config.IcbmConfig.siloComplexChancePercent;
        int[] seen = new int[1];
        Scenario leaker = new Fake("leak", ctx -> {
            com.gregguns.core.config.IcbmConfig.siloComplexChancePercent = before + 17; // never undone
            return ScenarioResult.pass("leak");
        });
        Scenario probe = new Fake("probe", ctx -> {
            seen[0] = com.gregguns.core.config.IcbmConfig.siloComplexChancePercent;
            return ScenarioResult.pass("probe");
        });
        ScenarioRun run = runner(log).begin(Arrays.asList(leaker, probe));
        assertTrue(run.isDone());
        assertEquals(before, seen[0], "the next scenario sees the original value");
        assertEquals(before, com.gregguns.core.config.IcbmConfig.siloComplexChancePercent);
    }

    @Test
    void synchronousScenariosFinishWithoutAnyTicks() {
        List<String> log = new ArrayList<>();
        ScenarioRun run = runner(log).begin(Arrays.asList(instant("a", log), instant("b", log)));
        assertTrue(run.isDone());
        assertEquals(
            2,
            run.summary()
                .passed());
    }

    @Test
    void aWaitingScenarioIsPolledEachTickAndSummaryComesOnlyWhenAllAreDone() {
        List<String> log = new ArrayList<>();
        ScenarioRun run = runner(log).begin(
            Arrays.asList(
                instant("a", log),
                waitsUntil("b", 3, 10, log),
                instant("c", log),
                waitsUntil("d", 1, 10, log)));
        assertFalse(run.isDone());
        assertEquals(
            1,
            run.summary()
                .results()
                .size(),
            "only a is finished at first");
        run.tick();
        run.tick();
        assertFalse(run.isDone());
        run.tick();
        // b finished on its third poll; c ran straight through; d is now waiting.
        assertFalse(run.isDone());
        assertEquals(
            3,
            run.summary()
                .results()
                .size());
        run.tick();
        assertTrue(run.isDone());
        assertEquals(
            4,
            run.summary()
                .passed());
        assertEquals(
            Arrays.asList(
                "start a",
                "cleanup a",
                "start b",
                "poll b 1",
                "poll b 2",
                "poll b 3",
                "cleanup b",
                "start c",
                "cleanup c",
                "start d",
                "poll d 1",
                "cleanup d"),
            log);
    }

    @Test
    void theContextLivesUntilThePendingScenarioAnswers() {
        List<String> log = new ArrayList<>();
        ScenarioRun run = runner(log).begin(Arrays.asList(waitsUntil("w", 2, 5, log)));
        assertFalse(log.contains("cleanup w"));
        run.tick();
        assertFalse(log.contains("cleanup w"));
        run.tick();
        assertTrue(log.contains("cleanup w"));
    }

    @Test
    void aScenarioThatNeverAnswersTimesOutAndIsCleanedUp() {
        List<String> log = new ArrayList<>();
        Scenario never = new Fake("never", ctx -> ScenarioResult.pending("never", 3, ticks -> null));
        ScenarioRun run = runner(log).begin(Arrays.asList(never, instant("after", log)));
        run.tick();
        run.tick();
        assertFalse(run.isDone());
        run.tick();
        assertTrue(run.isDone());
        ScenarioResult first = run.summary()
            .results()
            .get(0);
        assertFalse(first.passed());
        assertTrue(
            first.messages()
                .get(0)
                .contains("timed out after 3 ticks"));
        assertTrue(
            run.summary()
                .results()
                .get(1)
                .passed(),
            "the next scenario still runs");
        assertTrue(log.contains("cleanup never"));
    }

    @Test
    void aPollerThatThrowsFailsOnlyItsScenario() {
        List<String> log = new ArrayList<>();
        Scenario boom = new Fake(
            "boom",
            ctx -> ScenarioResult.pending("boom", 5, ticks -> { throw new IllegalStateException("bad"); }));
        ScenarioRun run = runner(log).begin(Arrays.asList(boom, instant("ok", log)));
        run.tick();
        assertTrue(run.isDone());
        assertFalse(
            run.summary()
                .results()
                .get(0)
                .passed());
        assertTrue(
            run.summary()
                .results()
                .get(1)
                .passed());
        assertTrue(log.contains("cleanup boom"));
    }

    @Test
    void aPollerReturningPendingFails() {
        Scenario odd = new Fake(
            "odd",
            ctx -> ScenarioResult.pending("odd", 5, ticks -> ScenarioResult.pending("odd", 5, t -> null)));
        ScenarioRun run = runner(new ArrayList<>()).begin(Arrays.asList(odd));
        run.tick();
        assertTrue(run.isDone());
        assertFalse(
            run.summary()
                .allPassed());
    }

    @Test
    void theSynchronousRunFailsAScenarioThatWantsTicksAndKeepsGoing() {
        List<String> log = new ArrayList<>();
        ScenarioSummary summary = runner(log).run(Arrays.asList(waitsUntil("slow", 3, 10, log), instant("fast", log)));
        assertEquals(1, summary.passed());
        assertEquals(1, summary.failed());
        assertTrue(
            summary.results()
                .get(0)
                .messages()
                .get(0)
                .contains("needs server ticks"));
        assertTrue(log.contains("cleanup slow"));
    }

    @Test
    void tickingADoneRunDoesNothing() {
        ScenarioRun run = runner(new ArrayList<>()).begin(new ArrayList<Scenario>());
        assertTrue(run.isDone());
        run.tick();
        assertTrue(
            run.summary()
                .allPassed());
    }

    @Test
    void pendingResultsValidateTheirArguments() {
        assertThrows(IllegalArgumentException.class, () -> ScenarioResult.pending("x", 0, t -> null));
        assertThrows(NullPointerException.class, () -> ScenarioResult.pending("x", 1, null));
        assertTrue(
            ScenarioResult.pending("x", 1, t -> null)
                .isPending());
        assertFalse(
            ScenarioResult.pass("x")
                .isPending());
    }
}
