package com.gregguns.icbm.worldgen;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;

import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.icbm.complex.ComplexLayout;
import com.gregguns.icbm.complex.ComplexRaster;
import com.gregguns.icbm.complex.gen.ComplexPlacer;
import com.gregguns.worldgen.StructurePlan;

/**
 * {@code icbm.complex.door_light} (D345): a Bravo complex with its blast doors is written in one tick, the pack's
 * lighting engine is then made to drain a full queue that starts at every door piece, and the world ticks on for five
 * seconds. A Carpenter's door piece whose light value was never asked starts a light update of its own when the
 * engine first asks; inside a full-queue drain that is a nested run, which ArchaicFix refuses and never recovers from,
 * and the server crashes on the next tick. The writer asks each piece once as it places it, so the scenario reaches
 * its end with the doors standing. Without Carpenter's Blocks there are no doors and it skips; without ArchaicFix's
 * engine the queue is not padded (vanilla lighting has no queue) and the scenario only writes and ticks.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Blast doors); D335, D345
public final class DoorLightScenarios {

    private DoorLightScenarios() {}

    /** @param registry where to register */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new DoorLight());
    }

    /** ArchaicFix's world-side lighting engine hook, by name (no compile dependency). */
    static final String ENGINE_PROVIDER = "org.embeddedt.archaicfix.lighting.api.ILightingEngineProvider";
    /** ArchaicFix drains a light queue once it holds this many entries (its MAX_SCHEDULED_COUNT). */
    // Source:
    // https://github.com/embeddedt/ArchaicFix/blob/0.8.0/src/main/java/org/embeddedt/archaicfix/lighting/world/lighting/LightingEngine.java#L25
    static final int ENGINE_QUEUE = 1 << 22;

    private static final class DoorLight implements Scenario {

        /** Ticks the world runs after the write: five seconds. */
        static final int RUN_TICKS = 100;

        @Override
        public String name() {
            return "icbm.complex.door_light";
        }

        @Override
        public String description() {
            return "Writes a Bravo complex with its blast doors in one tick, drains a full light queue that starts at"
                + " every door piece, and ticks on for five seconds: the server lives and the doors stand (D345).";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            final ScenarioResult.Builder out = ScenarioResult.builder(name());
            final World world = ctx.world();
            final Block door = FixtureWriter.garageDoor();
            if (door == null) return ScenarioResult.skip(name(), "Carpenter's Blocks is not loaded: no blast doors");
            final StructurePlan plan = ComplexLevelScenarios.plan(world);
            SiloChests.register();
            ComplexLevelScenarios.load(world, plan);
            final List<int[]> doors = new ArrayList<int[]>();
            final int[] step = { 0 };
            return ScenarioResult.pending(name(), 6000, ticks -> {
                try {
                    if (step[0]++ == 0) {
                        ComplexLevelScenarios.slab(world, plan, false);
                        ComplexPlacer placer = ComplexLevelScenarios.placer()
                            .forceLevel(ActivityLevel.PARTLY_ABANDONED)
                            .forceCeiling(SiloCeiling.EV);
                        int[] r = ComplexLevelScenarios.generate(world, plan, placer, "gregguns_complex_door_light");
                        out.checkEquals("writes dropped outside a window", 0, r[0]);
                        doors.addAll(find(world, plan, door));
                        out.check(!doors.isEmpty(), "blast door pieces written: " + doors.size());
                        if (engine(world)) {
                            // Every door piece first in the queue, then one air cell until the engine drains it.
                            int ax = plan.anchorX(), ay = ComplexLevelScenarios.floor(world) + 40;
                            for (int[] d : doors) world.updateLightByType(EnumSkyBlock.Block, d[0], d[1], d[2]);
                            for (int i = 0; i < ENGINE_QUEUE + 16; i++) {
                                world.updateLightByType(EnumSkyBlock.Block, ax, ay, plan.anchorZ());
                            }
                            out.note(
                                "ArchaicFix engine: a full block-light queue drained from " + doors.size()
                                    + " door pieces");
                        } else {
                            out.note("no ArchaicFix lighting engine: the queue is not padded");
                        }
                        return null;
                    }
                    if (step[0] <= RUN_TICKS) return null;
                    out.checkEquals(
                        "door pieces still standing after " + RUN_TICKS + " ticks",
                        doors.size(),
                        standing(world, doors, door));
                    return finish(out, world, plan);
                } catch (RuntimeException e) {
                    com.gregguns.GregGuns.LOG.warn("icbm.complex.door_light threw", e);
                    out.check(false, "threw " + e);
                    return finish(out, world, plan);
                }
            });
        }

        private static ScenarioResult finish(ScenarioResult.Builder out, World world, StructurePlan plan) {
            ComplexLevelScenarios.slab(world, plan, true);
            return out.build();
        }

        /** @return whether the world's light runs through ArchaicFix's queued engine */
        private static boolean engine(World world) {
            for (Class<?> c = world.getClass(); c != null; c = c.getSuperclass()) {
                for (Class<?> i : c.getInterfaces()) if (ENGINE_PROVIDER.equals(i.getName())) return true;
            }
            return false;
        }

        /** @return every door piece over the footprint at the level's height */
        private static List<int[]> find(World world, StructurePlan plan, Block door) {
            List<int[]> l = new ArrayList<int[]>();
            int floor = ComplexLevelScenarios.floor(world);
            for (int x = plan.anchorX(); x < plan.anchorX() + ComplexLayout.SIZE; x++) {
                for (int z = plan.anchorZ(); z < plan.anchorZ() + ComplexLayout.SIZE; z++) {
                    for (int y = floor; y < floor + ComplexRaster.LEVELS; y++) {
                        if (world.getBlock(x, y, z) == door) l.add(new int[] { x, y, z });
                    }
                }
            }
            return l;
        }

        private static int standing(World world, List<int[]> doors, Block door) {
            int n = 0;
            for (int[] d : doors) if (world.getBlock(d[0], d[1], d[2]) == door) n++;
            return n;
        }
    }
}
