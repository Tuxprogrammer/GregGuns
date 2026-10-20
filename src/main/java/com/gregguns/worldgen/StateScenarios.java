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
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            World world = ctx.world();
            ISaveHandler handler = world.getSaveHandler();
            File file = handler.getMapFileFromName(SCENARIO_DATA);
            if (file == null) {
                return ScenarioResult.fail(name(), "this world has no save file for map data");
            }
            try {
                MapStorage writer = new MapStorage(handler);
                StructureState state = new StructureState(SCENARIO_DATA);
                writer.setData(SCENARIO_DATA, state);

                String placed = PlanKey.of(SizeClass.MEDIUM, 3, -4);
                String rejected = PlanKey.of(SizeClass.BIG, -1, 2);
                AtomicInteger calls = new AtomicInteger();
                state.decideOnce(placed, () -> {
                    calls.incrementAndGet();
                    return Decision.placed(67);
                });
                state.decideOnce(rejected, () -> {
                    calls.incrementAndGet();
                    return Decision.rejected("water");
                });
                state.addFlag(placed, new FlagPos(10, 68, -20));
                out.check(state.isDirty(), "recording a decision should mark the data dirty");

                writer.saveAllData();
                out.check(file.isFile(), "the data file should exist after saveAllData");

                // A fresh storage has an empty cache, so this really reads the file.
                MapStorage reader = new MapStorage(handler);
                StructureState back = (StructureState) reader.loadData(StructureState.class, SCENARIO_DATA);
                if (back == null) {
                    out.check(false, "the saved data should load back");
                    return out.build();
                }
                Decision p = back.get(placed);
                Decision r = back.get(rejected);
                out.check(p != null && p.isPlaced(), "the placed decision should survive");
                if (p != null) {
                    out.checkEquals("ground Y", 67, p.groundY());
                    out.checkEquals("flags", 1, p.flags().size());
                    out.check(
                        p.flags()
                            .contains(new FlagPos(10, 68, -20)),
                        "the flag position should survive");
                }
                out.check(r != null && !r.isPlaced(), "the rejected decision should survive");
                if (r != null) {
                    out.checkEquals("reason", "water", r.reason());
                }
                out.checkEquals("record count", 2, back.ledger().size());

                // The record, not a new measurement, decides after a reload.
                Decision again = back.decideOnce(placed, () -> {
                    calls.incrementAndGet();
                    return Decision.rejected("later");
                });
                out.check(again.isPlaced(), "a later window should obey the saved record");
                out.checkEquals("decider calls", 2, calls.get());
            } finally {
                if (file.isFile() && !file.delete()) {
                    out.note("could not delete the scenario data file " + file.getName());
                }
            }
            return out.build();
        }
    }
}
