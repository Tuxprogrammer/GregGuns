package com.gregguns.icbm.soak;

import java.util.Locale;

import com.gregguns.entity.npc.SoakStats;

/**
 * The tick-time record of one soak phase: every tick time in order, read as a whole ({@link #stats()}) or as
 * one-second buckets (the MSPT and TPS curve the soak logs). Pure: no Minecraft types, no clock. Values are server tick
 * times in milliseconds.
 */
// Spec: docs/gregguns-icbm-spec/01-core/soak.md
public final class SoakCurve {

    /** Ticks per bucket: one second at 20 TPS. */
    public static final int BUCKET = 20;
    /** The vanilla tick length, ms: a mean at or under it is 20 TPS. */
    public static final double TICK_MS = 50.0;

    private double[] samples;
    private int count;

    /** @param capacity the expected number of ticks; the record grows past it */
    public SoakCurve(int capacity) {
        samples = new double[Math.max(16, capacity)];
    }

    /** @param ms one tick time */
    public void add(double ms) {
        if (count == samples.length) {
            double[] grown = new double[samples.length * 2];
            System.arraycopy(samples, 0, grown, 0, count);
            samples = grown;
        }
        samples[count++] = ms;
    }

    /** @return the ticks recorded */
    public int size() {
        return count;
    }

    /**
     * @param n how many of the newest ticks
     * @return the newest {@code n} tick times, oldest first (fewer when fewer are recorded)
     */
    public double[] tail(int n) {
        int k = Math.max(0, Math.min(n, count));
        double[] out = new double[k];
        System.arraycopy(samples, count - k, out, 0, k);
        return out;
    }

    /** @return the statistics of every tick recorded */
    public SoakStats stats() {
        double[] out = new double[count];
        System.arraycopy(samples, 0, out, 0, count);
        return SoakStats.of(out);
    }

    /**
     * The mean with the slowest ticks left out. A world save every 45 seconds costs one long tick that has nothing to
     * do with the soak; one such tick in a 30-second window moves the plain mean by several milliseconds.
     *
     * @param dropFraction the share of the slowest ticks to leave out, 0 to below 1
     * @return the mean of the rest, ms; 0 when nothing is recorded
     */
    public double trimmedMean(double dropFraction) {
        if (count == 0) {
            return 0.0;
        }
        double[] sorted = new double[count];
        System.arraycopy(samples, 0, sorted, 0, count);
        java.util.Arrays.sort(sorted);
        int keep = Math.max(1, count - (int) Math.floor(count * Math.max(0.0, Math.min(0.99, dropFraction))));
        double sum = 0.0;
        for (int i = 0; i < keep; i++) {
            sum += sorted[i];
        }
        return sum / keep;
    }

    /** @return the sum of every tick time recorded, ms (the wall time the phase took on the server thread) */
    public double totalMs() {
        double sum = 0.0;
        for (int i = 0; i < count; i++) {
            sum += samples[i];
        }
        return sum;
    }

    /**
     * @param meanMs a mean tick time
     * @return the ticks per second it allows: 20 when the server keeps up, fewer when it falls behind
     */
    public static double tps(double meanMs) {
        return meanMs <= TICK_MS ? 20.0 : 1000.0 / meanMs;
    }

    /**
     * @return the index of the first tick after which every one-second bucket has a mean at or under 50 ms, that is
     *         when the server is back at 20 TPS for good; 0 when it never fell behind, {@link #size()} when the last
     *         bucket is still behind
     */
    public int recoveredAt() {
        int at = 0;
        for (int start = 0; start < count; start += BUCKET) {
            int end = Math.min(count, start + BUCKET);
            double sum = 0.0;
            for (int i = start; i < end; i++) {
                sum += samples[i];
            }
            if (sum / (end - start) > TICK_MS) {
                at = end;
            }
        }
        return at;
    }

    /**
     * The one-second buckets, for the log: {@code mean/max} ms per bucket, space separated.
     *
     * @param maxBuckets the most buckets to print; longer curves keep their first and last ones
     * @return the curve, or "-" when empty
     */
    public String buckets(int maxBuckets) {
        int n = (count + BUCKET - 1) / BUCKET;
        if (n == 0) {
            return "-";
        }
        StringBuilder sb = new StringBuilder();
        int head = n <= maxBuckets ? n : maxBuckets / 2;
        for (int b = 0; b < n; b++) {
            if (b == head && n > maxBuckets) {
                sb.append(" ...");
                b = n - (maxBuckets - head);
            }
            int start = b * BUCKET;
            int end = Math.min(count, start + BUCKET);
            double sum = 0.0;
            double max = 0.0;
            for (int i = start; i < end; i++) {
                sum += samples[i];
                max = Math.max(max, samples[i]);
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(String.format(Locale.ROOT, "%.0f/%.0f", sum / (end - start), max));
        }
        return sb.toString();
    }
}
