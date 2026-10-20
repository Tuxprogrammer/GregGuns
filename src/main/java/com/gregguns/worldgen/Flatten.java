package com.gregguns.worldgen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Terrain flattening as a pure plan: heights in, per-column edits out. Replaces the Techguns flatten, whose median
 * was off by one and stayed 0 for two or fewer columns, so a steep site was dug down to y near 0 (WGEN-16). Water is
 * never touched: only columns whose surface is ground are planned, so a lake edge is not drained (WGEN-26).
 */
public final class Flatten {

    /** One planned column change: ground surface moves from {@code fromY} to {@code toY}. */
    public static final class ColumnEdit {

        public final int x;
        public final int z;
        public final int fromY;
        public final int toY;

        ColumnEdit(int x, int z, int fromY, int toY) {
            this.x = x;
            this.z = z;
            this.fromY = fromY;
            this.toY = toY;
        }
    }

    private Flatten() {}

    /** The true median: the middle value, or the mean of the two middle values for an even count. */
    public static double median(int[] values) {
        if (values.length == 0) {
            throw new IllegalArgumentException("no values");
        }
        int[] s = values.clone();
        Arrays.sort(s);
        int n = s.length;
        if (n % 2 == 1) {
            return s[n / 2];
        }
        return (s[n / 2 - 1] + s[n / 2]) / 2.0;
    }

    /**
     * Remaps heights into {@code [median - maxDiff/2, median + maxDiff/2]} linearly. Unchanged when the span is at
     * most {@code maxDiff}. A new array is returned; the input is not modified.
     */
    public static int[] flatten(int[] heights, int maxDiff) {
        if (maxDiff < 0) {
            throw new IllegalArgumentException("maxDiff < 0");
        }
        int[] out = heights.clone();
        if (heights.length == 0) {
            return out;
        }
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int h : heights) {
            min = Math.min(min, h);
            max = Math.max(max, h);
        }
        int span = max - min;
        if (span <= maxDiff) {
            return out;
        }
        double median = median(heights);
        for (int i = 0; i < out.length; i++) {
            out[i] = (int) Math.round((heights[i] - min) / (double) span * maxDiff + median - maxDiff / 2.0);
        }
        return out;
    }

    /**
     * Clamp for a window that cannot see the whole footprint: pulls a column to within {@code maxDiff/2} of the
     * structure's recorded reference height (PLAN-M8 Q3), so every window agrees without reading its neighbors.
     */
    public static int clampToBand(int height, int referenceY, int maxDiff) {
        int lo = referenceY - maxDiff / 2;
        int hi = referenceY + maxDiff / 2;
        return Math.max(lo, Math.min(hi, height));
    }

    /**
     * Plans edits for the footprint columns inside the probe's window. Columns without a ground surface (liquid,
     * outside) are skipped, which is the no-drain rule. The median is of the columns this window can see.
     */
    public static List<ColumnEdit> plan(HeightProbe probe, Footprint fp, int maxDiff) {
        List<int[]> cols = collect(probe, fp);
        int[] heights = new int[cols.size()];
        for (int i = 0; i < heights.length; i++) {
            heights[i] = cols.get(i)[2];
        }
        int[] target = flatten(heights, maxDiff);
        List<ColumnEdit> edits = new ArrayList<>();
        for (int i = 0; i < heights.length; i++) {
            if (target[i] != heights[i]) {
                int[] c = cols.get(i);
                edits.add(new ColumnEdit(c[0], c[1], heights[i], target[i]));
            }
        }
        return edits;
    }

    /** Like {@link #plan} but against a recorded reference height, for windows after the first. */
    public static List<ColumnEdit> planToBand(HeightProbe probe, Footprint fp, int referenceY, int maxDiff) {
        List<ColumnEdit> edits = new ArrayList<>();
        for (int[] c : collect(probe, fp)) {
            int t = clampToBand(c[2], referenceY, maxDiff);
            if (t != c[2]) {
                edits.add(new ColumnEdit(c[0], c[1], c[2], t));
            }
        }
        return edits;
    }

    private static List<int[]> collect(HeightProbe probe, Footprint fp) {
        PopulateWindow w = probe.window();
        List<int[]> cols = new ArrayList<>();
        for (int x = Math.max(fp.minX(), w.minX()); x <= Math.min(fp.maxX(), w.maxX()); x++) {
            for (int z = Math.max(fp.minZ(), w.minZ()); z <= Math.min(fp.maxZ(), w.maxZ()); z++) {
                int y = probe.landY(x, z);
                if (y != HeightProbe.NONE) {
                    cols.add(new int[] { x, z, y });
                }
            }
        }
        return cols;
    }
}
