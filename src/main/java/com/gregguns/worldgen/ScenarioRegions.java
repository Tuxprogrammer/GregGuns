package com.gregguns.worldgen;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiPredicate;

import net.minecraft.world.World;
import net.minecraft.world.gen.ChunkProviderServer;

/**
 * Hands each worldgen scenario its OWN far-away chunk square, so no scenario's blocks, populated chunks or recorded
 * {@link StructureState} decisions leak into another's area. Every scenario name gets a lane of its own (a block of
 * chunk columns nobody else uses); inside the lane, each attempt moves to the next never-loaded square. A scenario
 * cannot un-generate terrain, so the squares stay behind; the "last toucher" map names who left what, for failure
 * messages. Dev environment only; server side.
 */
public final class ScenarioRegions {

    private static final int LANE_START = 3000;
    private static final int LANE_WIDTH = 160;
    private static final int ATTEMPT_STEP = 48;
    private static final int ATTEMPTS = 24;

    private static final Map<String, Integer> LANES = new HashMap<>();
    private static final Map<Long, String> TOUCHED = new HashMap<>();

    private ScenarioRegions() {}

    private static synchronized int lane(String scenario) {
        Integer l = LANES.get(scenario);
        if (l == null) {
            l = LANES.size();
            LANES.put(scenario, l);
        }
        return l;
    }

    /**
     * Finds a square of {@code side} chunks (plus {@code margin} around it) that was never loaded, in the scenario's
     * own lane, and for which {@code extraFresh} (given the minimum chunk) also holds, e.g. no recorded structure
     * decision. Marks it as touched by the scenario. Returns the minimum chunk {x, z}, or null.
     *
     * @param extraFresh may be null
     */
    public static synchronized int[] allocate(World world, String scenario, int side, int margin,
        BiPredicate<Integer, Integer> extraFresh) {
        ChunkProviderServer cps = (ChunkProviderServer) world.getChunkProvider();
        int bx = LANE_START + lane(scenario) * LANE_WIDTH;
        for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
            int bz = LANE_START + attempt * ATTEMPT_STEP;
            if (!isFree(cps, bx, bz, side, margin)) continue;
            if (extraFresh != null && !extraFresh.test(bx, bz)) continue;
            touch(scenario, bx, bz, side);
            return new int[] { bx, bz };
        }
        return null;
    }

    private static boolean isFree(ChunkProviderServer cps, int bx, int bz, int side, int margin) {
        for (int dx = -margin; dx < side + margin; dx++) {
            for (int dz = -margin; dz < side + margin; dz++) {
                if (cps.chunkExists(bx + dx, bz + dz) || TOUCHED.containsKey(key(bx + dx, bz + dz))) return false;
            }
        }
        return true;
    }

    /** Records that the scenario owns the square from now on. */
    public static synchronized void touch(String scenario, int bx, int bz, int side) {
        for (int dx = 0; dx < side; dx++) {
            for (int dz = 0; dz < side; dz++) {
                TOUCHED.put(key(bx + dx, bz + dz), scenario);
            }
        }
    }

    /** The scenario that last claimed the chunk, or "nobody". */
    public static synchronized String lastToucher(int chunkX, int chunkZ) {
        String s = TOUCHED.get(key(chunkX, chunkZ));
        return s == null ? "nobody" : s;
    }

    /** As {@link #lastToucher(int, int)}, for a block position. */
    public static String lastToucherOfBlock(int x, int z) {
        return lastToucher(Math.floorDiv(x, 16), Math.floorDiv(z, 16));
    }

    private static long key(int cx, int cz) {
        return ((long) cx << 32) ^ (cz & 0xFFFFFFFFL);
    }
}
