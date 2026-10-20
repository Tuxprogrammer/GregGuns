package com.gregguns.worldgen;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.world.World;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.MapStorage;

import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;

/**
 * Scenarios of the plan state (M8 G6). Dev environment only. Server side. {@code WorldgenScenarios.registerAll} calls
 * {@link #registerAll}.
 */
// Spec: docs/gregguns-spec/11-worldgen/structure-placement.md (GGS-11-10); PLAN-M8 Q3
public final class StateScenarios {

    /** A name of its own, so the scenario never touches the real structure record. */
    private static final String SCENARIO_DATA = "gregguns_structures_scenario";
    private static final String PLACED = PlanKey.of("test", SizeClass.MEDIUM, 3, -4);
    private static final String REJECTED = PlanKey.of("test", SizeClass.BIG, -1, 2);

    private StateScenarios() {}

    /**
     * @param registry where to add the scenarios
     */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new StatePersistsScenario());
    }

    /** A recorded decision survives a real save to disk and a load through a fresh {@link MapStorage}. */
    private static final class StatePersistsScenario implements Scenario {

        @Override
        public String name() {
            return "worldgen.state_persists";
        }

        @Override
        public String description() {
            return "Placed (with ground Y and flags) and rejected plan decisions are saved by MapStorage and read back"
                + " by a fresh one; the recorded decision wins over a later decider (PLAN-M8 Q3).";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            World world = ctx.world();
            ISaveHandler handler = world.getSaveHandler();
            File file = handler.getMapFileFromName(SCENARIO_DATA);
            if (file == null) {
                return ScenarioResult.fail(name(), "this world has no save file for map data");
            }
            if (file.isFile() && !file.delete()) {
                return ScenarioResult.fail(name(), "could not clear a stale " + file.getName());
            }

            MapStorage writer = new MapStorage(handler);
            StructureState state = new StructureState(SCENARIO_DATA);
            writer.setData(SCENARIO_DATA, state);
            state.decideOnce(PLACED, () -> Decision.placed(67));
            state.decideOnce(REJECTED, () -> Decision.rejected("water"));
            state.addFlag(PLACED, new FlagPos(10, 68, -20));
            if (!state.isDirty()) {
                return ScenarioResult.fail(name(), "recording a decision should mark the data dirty");
            }
            writer.saveAllData();

            // The pack writes map data on another thread (the file appears a moment later), so wait for it, then
            // give the writer a few more ticks to finish before reading.
            final int[] seenAt = { -1 };
            return ScenarioResult.pending(name(), 100, ticks -> {
                if (!file.isFile()) {
                    return null;
                }
                if (seenAt[0] < 0) {
                    seenAt[0] = ticks;
                }
                if (ticks - seenAt[0] < 5) {
                    return null;
                }
                try {
                    return verify(handler, file);
                } finally {
                    if (file.isFile() && !file.delete()) {
                        file.deleteOnExit();
                    }
                }
            });
        }

        /** Loads through a fresh {@link MapStorage}, whose empty cache forces a real read of the file. */
        private ScenarioResult verify(ISaveHandler handler, File file) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            StructureState back = (StructureState) new MapStorage(handler)
                .loadData(StructureState.class, SCENARIO_DATA);
            if (back == null) {
                return ScenarioResult.fail(name(), "the saved data should load back from " + file.getName());
            }
            Decision p = back.get(PLACED);
            Decision r = back.get(REJECTED);
            out.check(p != null && p.isPlaced(), "the placed decision should survive");
            if (p != null) {
                out.checkEquals("ground Y", 67, p.groundY());
                out.checkEquals(
                    "flags",
                    1,
                    p.flags()
                        .size());
                out.check(
                    p.flags()
                        .contains(new FlagPos(10, 68, -20)),
                    "the flag position should survive");
            }
            out.check(r != null && !r.isPlaced(), "the rejected decision should survive");
            if (r != null) {
                out.checkEquals("reason", "water", r.reason());
            }
            out.checkEquals(
                "record count",
                2,
                back.ledger()
                    .size());

            // The record, not a new measurement, decides after a reload.
            AtomicInteger calls = new AtomicInteger();
            Decision again = back.decideOnce(PLACED, () -> {
                calls.incrementAndGet();
                return Decision.rejected("later");
            });
            out.check(again.isPlaced(), "a later window should obey the saved record");
            out.checkEquals("decider calls after reload", 0, calls.get());
            return out.build();
        }
    }
}
