package com.gregguns.worldgen;

import java.util.ArrayList;
import java.util.List;

/**
 * Placement tests for a rotated {@link PlacedFootprint}, read through a {@link HeightProbe}. Only columns inside the
 * probe's window are sampled: with a multi-window structure the first populate that overlaps decides from its own
 * columns and records the verdict (PLAN-M8 Q3), so a window with no sampled column answers {@link Reason#NO_SAMPLES}
 * and the caller must obey the stored record instead.
 */
public final class TerrainCheck {

    public enum Reason {
        OK,
        /** No sample column of the footprint lies in the window. */
        NO_SAMPLES,
        /** A land sample hit liquid, or had no ground. */
        LIQUID_OR_NO_GROUND,
        /** Sampled ground heights differ by more than the limit. */
        TOO_STEEP,
        /** A ship sample column does not have a water surface. */
        NOT_WATER,
        /** A ship sample column has less water than the hull needs. */
        TOO_SHALLOW
    }

    /** Verdict. {@code y} is the land surface (land) or the lowest water surface (ships); -1 when not ok. */
    public static final class Result {

        public final boolean ok;
        public final int y;
        public final int sampled;
        public final Reason reason;

        Result(boolean ok, int y, int sampled, Reason reason) {
            this.ok = ok;
            this.y = y;
            this.sampled = sampled;
            this.reason = reason;
        }
    }

    private TerrainCheck() {}

    /**
     * Sample offsets 0, step, 2*step ... that always include the last cell {@code size - 1} (WGEN-15: Techguns'
     * clamp branch never ran, so the far edge went unchecked unless the size was a multiple of the step).
     */
    public static int[] sampleOffsets(int size, int step) {
        if (size < 1 || step < 1) {
            throw new IllegalArgumentException("size and step must be positive");
        }
        List<Integer> out = new ArrayList<>();
        for (int o = 0; o < size - 1; o += step) {
            out.add(o);
        }
        out.add(size - 1);
        int[] a = new int[out.size()];
        for (int i = 0; i < a.length; i++) {
            a[i] = out.get(i);
        }
        return a;
    }

    /**
     * Land check on the rotated footprint (WGEN-05, WGEN-15, GGS-11-13). Fails on any sample that is liquid or has
     * no ground, or when max - min of the sampled ground heights exceeds {@code maxDiff}. Returns the rounded mean.
     */
    public static Result checkLand(HeightProbe probe, PlacedFootprint fp, int maxDiff, int step) {
        int[] xs = sampleOffsets(fp.sizeX(), step);
        int[] zs = sampleOffsets(fp.sizeZ(), step);
        int n = 0;
        long sum = 0;
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int ox : xs) {
            for (int oz : zs) {
                int x = fp.minX() + ox;
                int z = fp.minZ() + oz;
                if (!probe.window()
                    .contains(x, z)) {
                    continue;
                }
                int y = probe.landY(x, z);
                if (y == HeightProbe.NONE) {
                    return new Result(false, -1, n, Reason.LIQUID_OR_NO_GROUND);
                }
                n++;
                sum += y;
                min = Math.min(min, y);
                max = Math.max(max, y);
            }
        }
        if (n == 0) {
            return new Result(false, -1, 0, Reason.NO_SAMPLES);
        }
        if (max - min > maxDiff) {
            return new Result(false, -1, n, Reason.TOO_STEEP);
        }
        return new Result(true, (int) Math.round((double) sum / n), n, Reason.OK);
    }

    /**
     * Ship check on the rotated footprint (WGEN-06, WGEN-08, GGS-11-13): every sampled column must have a water
     * surface and at least {@code minDepth} liquid blocks between surface and floor. A shallow or dry column (an
     * island in an ocean biome) fails, which Techguns' biome-only test missed. {@code y} is the lowest surface.
     */
    public static Result checkWater(HeightProbe probe, PlacedFootprint fp, int minDepth, int step) {
        int[] xs = sampleOffsets(fp.sizeX(), step);
        int[] zs = sampleOffsets(fp.sizeZ(), step);
        int n = 0;
        int lowest = Integer.MAX_VALUE;
        for (int ox : xs) {
            for (int oz : zs) {
                int x = fp.minX() + ox;
                int z = fp.minZ() + oz;
                if (!probe.window()
                    .contains(x, z)) {
                    continue;
                }
                int surface = probe.waterSurfaceY(x, z);
                if (surface == HeightProbe.NONE) {
                    return new Result(false, -1, n, Reason.NOT_WATER);
                }
                int floor = probe.seaFloorY(x, z);
                if (floor == HeightProbe.NONE || surface - floor < minDepth) {
                    return new Result(false, -1, n, Reason.TOO_SHALLOW);
                }
                n++;
                lowest = Math.min(lowest, surface);
            }
        }
        if (n == 0) {
            return new Result(false, -1, 0, Reason.NO_SAMPLES);
        }
        return new Result(true, lowest, n, Reason.OK);
    }
}
