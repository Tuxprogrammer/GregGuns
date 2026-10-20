package com.gregguns.entity.npc;

import java.util.Arrays;

/**
 * Summary statistics and the pass rule of the NPC soak ({@code npc.soak}). Pure: no Minecraft types, no clock. Values
 * are server tick times in milliseconds.
 *
 * <p>
 * Usage: {@code SoakStats s = SoakStats.of(samplesMs);} then {@code SoakStats.verdict(baseline, load, 50.0, 10.0)}.
 * Server side only in practice.
 */
// Spec: docs/gregguns-spec/10-entities-npcs/m7-exit-gate.md (box 4)
public final class SoakStats {

    private final double mean;
    private final double p95;
    private final double max;
    private final int count;

    private SoakStats(double mean, double p95, double max, int count) {
        this.mean = mean;
        this.p95 = p95;
        this.max = max;
        this.count = count;
    }

    /**
     * @param samplesMs tick times in milliseconds; not modified
     * @return their statistics; all zero when empty. The 95th percentile uses the nearest-rank method
     */
    public static SoakStats of(double[] samplesMs) {
        int n = samplesMs.length;
        if (n == 0) {
            return new SoakStats(0, 0, 0, 0);
        }
        double[] sorted = samplesMs.clone();
        Arrays.sort(sorted);
        double sum = 0;
        for (double v : sorted) {
            sum += v;
        }
        int rank = (int) Math.ceil(0.95 * n);
        return new SoakStats(sum / n, sorted[Math.max(0, rank - 1)], sorted[n - 1], n);
    }

    /**
     * Judges a soak.
     *
     * @param baseline        statistics of the ticks with no NPCs
     * @param load            statistics of the ticks with the NPCs running
     * @param budgetMeanMs    absolute cap on the load mean (50 ms is 20 TPS)
     * @param maxRegressionMs largest allowed increase of the mean over the baseline
     * @return a failure message, or {@code null} when the soak passes
     */
    public static String verdict(SoakStats baseline, SoakStats load, double budgetMeanMs, double maxRegressionMs) {
        if (load.count == 0 || baseline.count == 0) {
            return "no tick samples were taken";
        }
        if (load.mean >= budgetMeanMs) {
            return String.format("mean tick %.2f ms is not under the %.1f ms budget", load.mean, budgetMeanMs);
        }
        double regression = load.mean - baseline.mean;
        if (regression > maxRegressionMs) {
            return String.format(
                "mean tick rose %.2f ms over the baseline, more than the %.1f ms margin",
                regression,
                maxRegressionMs);
        }
        return null;
    }

    /** @return the mean in milliseconds */
    public double mean() {
        return mean;
    }

    /** @return the 95th percentile in milliseconds */
    public double p95() {
        return p95;
    }

    /** @return the largest sample in milliseconds */
    public double max() {
        return max;
    }

    /** @return the number of samples */
    public int count() {
        return count;
    }

    @Override
    public String toString() {
        return String.format("mean %.2f ms, p95 %.2f ms, max %.2f ms over %d ticks", mean, p95, max, count);
    }
}
