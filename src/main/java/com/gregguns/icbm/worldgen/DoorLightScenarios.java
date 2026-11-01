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
 * {@code icbm.complex.door_light} and {@code icbm.complex.door_reload} (D345). door_light: a Bravo complex with its
 * blast doors is written in one tick, the pack's
 * lighting engine is then made to drain a full queue that starts at every door piece, and the world ticks on for five
 * seconds. A Carpenter's door piece whose light value was never asked starts a light update of its own when the
 * engine first asks; inside a full-queue drain that is a nested run, which ArchaicFix refuses and never recovers from,
 * and the server crashes on the next tick. The writer asks each piece once as it places it, so the scenario reaches
 * its end with the doors standing. Without Carpenter's Blocks there are no doors and it skips; without ArchaicFix's
 * engine the queue is not padded (vanilla lighting has no queue) and the scenario only writes and ticks.
 *
 * <p>
 * door_reload: the same complex, its door chunks saved, unloaded and loaded back from disk, where Carpenter's cache
 * starts unknown again. Every piece read back must have its light value already (the chunk-load primer), and the same
 * drain must leave the server alive.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Blast doors); D335, D345
public final class DoorLightScenarios {

    private DoorLightScenarios() {}

    /** @param registry where to register */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new DoorLight());
        registry.register(new DoorReload());
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
            if (!com.gregguns.core.module.ModuleState.isActive(com.gregguns.core.module.ModuleId.ICBM)) {
                return ScenarioResult.skip(name(), com.gregguns.icbm.IcbmAvailability.offReason());
            }
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
                        drainFrom(world, plan, doors, out);
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

    }

    private static final class DoorReload implements Scenario {

        @Override
        public String name() {
            return "icbm.complex.door_reload";
        }

        @Override
        public String description() {
            return "Writes a Bravo complex with its blast doors, saves and unloads their chunks and loads them back from"
                + " disk; every door piece read back has its light value worked out already, a full light queue drained"
                + " from them leaves the server alive, and the doors stand after five seconds (D345).";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            if (!com.gregguns.core.module.ModuleState.isActive(com.gregguns.core.module.ModuleId.ICBM)) {
                return ScenarioResult.skip(name(), com.gregguns.icbm.IcbmAvailability.offReason());
            }
            final ScenarioResult.Builder out = ScenarioResult.builder(name());
            final World world = ctx.world();
            final Block door = FixtureWriter.garageDoor();
            if (door == null) return ScenarioResult.skip(name(), "Carpenter's Blocks is not loaded: no blast doors");
            final com.gregguns.compat.carpenters.CarpentersLightPrimer installed = com.gregguns.compat.carpenters.CarpentersLightPrimer
                .installed();
            final com.gregguns.compat.carpenters.CarpentersLightPrimer reader = com.gregguns.compat.carpenters.CarpentersLightPrimer
                .reader();
            out.check(installed != null, "the Carpenter's light primer is installed");
            if (reader == null) return out.build();
            final StructurePlan plan = ComplexLevelScenarios.plan(world);
            SiloChests.register();
            ComplexLevelScenarios.load(world, plan);
            final List<int[]> doors = new ArrayList<int[]>();
            final java.util.Set<Long> chunks = new java.util.LinkedHashSet<Long>();
            final int[] step = { 0 }, phase = { 0 };
            return ScenarioResult.pending(name(), 6000, ticks -> {
                try {
                    int s = step[0]++;
                    if (s == 0) {
                        ComplexLevelScenarios.slab(world, plan, false);
                        ComplexPlacer placer = ComplexLevelScenarios.placer()
                            .forceLevel(ActivityLevel.PARTLY_ABANDONED)
                            .forceCeiling(SiloCeiling.EV);
                        ComplexLevelScenarios.generate(world, plan, placer, "gregguns_complex_door_reload");
                        doors.addAll(find(world, plan, door));
                        out.check(!doors.isEmpty(), "blast door pieces written: " + doors.size());
                        return null;
                    }
                    if (s == 1) {
                        // Save, then queue the door chunks to unload; the server unloads them on its own ticks.
                        queueUnload(world, doors, chunks, true);
                        return null;
                    }
                    if (phase[0] == 0) {
                        int gone = gone(world, chunks);
                        if (gone < chunks.size() && s < UNLOAD_TICKS) {
                            queueUnload(world, doors, chunks, false);
                            return null;
                        }
                        out.check(
                            !chunks.isEmpty() && gone == chunks.size(),
                            "door chunks unloaded within " + s + " ticks: " + gone + "/" + chunks.size());
                        long[] before = installed == null ? new long[3] : installed.stats();
                        for (long c : chunks) world.getChunkProvider()
                            .loadChunk((int) (c >> 32), (int) c);
                        long[] after = installed == null ? new long[3] : installed.stats();
                        int unknown = 0, tiles = 0;
                        for (int[] d : doors) {
                            net.minecraft.tileentity.TileEntity te = world.getTileEntity(d[0], d[1], d[2]);
                            if (te == null) continue;
                            tiles++;
                            if (reader.unknown(te)) unknown++;
                        }
                        out.checkEquals("door pieces read back with their tiles", doors.size(), tiles);
                        out.checkEquals("door pieces read back with their light value still unknown", 0, unknown);
                        out.note(
                            "primer: " + (after[0] - before[0])
                                + " chunk loads, "
                                + (after[1] - before[1])
                                + " pieces primed, about "
                                + after[2] / 1000
                                + " µs per chunk load overall");
                        drainFrom(world, plan, doors, out);
                        phase[0] = s;
                        return null;
                    }
                    if (s <= phase[0] + DoorLight.RUN_TICKS) return null;
                    out.checkEquals(
                        "door pieces still standing after " + DoorLight.RUN_TICKS + " ticks",
                        doors.size(),
                        standing(world, doors, door));
                    return finish(out, world, plan);
                } catch (RuntimeException e) {
                    com.gregguns.GregGuns.LOG.warn("icbm.complex.door_reload threw", e);
                    out.check(false, "threw " + e);
                    return finish(out, world, plan);
                }
            });
        }

        /** Ticks the server gets to unload the door chunks (the pack's chunk unloading is paced). */
        static final int UNLOAD_TICKS = 600;

        /** Queues every chunk that holds a door piece to unload, saving the world's chunks first when asked. */
        private static void queueUnload(World world, List<int[]> doors, java.util.Set<Long> chunks, boolean save) {
            net.minecraft.world.gen.ChunkProviderServer cps = (net.minecraft.world.gen.ChunkProviderServer) world
                .getChunkProvider();
            for (int[] d : doors) chunks.add(((long) (d[0] >> 4) << 32) | ((d[2] >> 4) & 0xFFFFFFFFL));
            if (chunks.isEmpty()) return;
            if (save) {
                cps.saveChunks(true, null);
                cps.currentChunkLoader.saveExtraData();
            }
            for (long c : chunks) cps.unloadChunksIfNotNearSpawn((int) (c >> 32), (int) c);
            cps.unloadQueuedChunks();
        }

        /** @return how many of the chunks are no longer loaded */
        private static int gone(World world, java.util.Set<Long> chunks) {
            int n = 0;
            for (long c : chunks) if (!world.getChunkProvider()
                .chunkExists((int) (c >> 32), (int) c)) n++;
            return n;
        }
    }

    static ScenarioResult finish(ScenarioResult.Builder out, World world, StructurePlan plan) {
        ComplexLevelScenarios.slab(world, plan, true);
        return out.build();
    }

    /** @return whether the world's light runs through ArchaicFix's queued engine */
    static boolean engine(World world) {
        for (Class<?> c = world.getClass(); c != null; c = c.getSuperclass()) {
            for (Class<?> i : c.getInterfaces()) if (ENGINE_PROVIDER.equals(i.getName())) return true;
        }
        return false;
    }

    /** @return every door piece over the footprint at the level's height */
    static List<int[]> find(World world, StructurePlan plan, Block door) {
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

    static int standing(World world, List<int[]> doors, Block door) {
        int n = 0;
        for (int[] d : doors) if (world.getBlock(d[0], d[1], d[2]) == door) n++;
        return n;
    }

    /**
     * Queues a block-light update at every door piece first, then at one air cell until ArchaicFix's engine drains the
     * full queue; the drain reaches the door pieces at once. Without the engine nothing is padded.
     */
    static void drainFrom(World world, StructurePlan plan, List<int[]> doors, ScenarioResult.Builder out) {
        if (!engine(world)) {
            out.note("no ArchaicFix lighting engine: the queue is not padded");
            return;
        }
        int ax = plan.anchorX(), ay = ComplexLevelScenarios.floor(world) + 40;
        for (int[] d : doors) world.updateLightByType(EnumSkyBlock.Block, d[0], d[1], d[2]);
        for (int i = 0; i < ENGINE_QUEUE + 16; i++) world.updateLightByType(EnumSkyBlock.Block, ax, ay, plan.anchorZ());
        out.note("ArchaicFix engine: a full block-light queue drained from " + doors.size() + " door pieces");
    }
}
