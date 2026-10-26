package com.gregguns.icbm.missile;

import java.util.Random;

/**
 * Guidance scatter (D24). Each axis is the difference of two uniform rolls (triangular, after ICBM 2) times a
 * half-width. The half-width is the tier's base scatter, shrunk for each guidance tier above the missile's own, plus
 * the baseline's distance term {@code 10 * (d / range)^2}. There is no failure roll and no dud. No Minecraft types,
 * both sides.
 */
// Spec: docs/gregguns-icbm-spec/03-missiles/guidance.md
public final class Scatter {

    /** Share of the base scatter removed by each guidance tier. */
    public static final double SHRINK_PER_TIER = 0.25;
    /** The base scatter never shrinks below this share. */
    public static final double MIN_SHARE = 0.1;
    /** Coefficient of the distance term, blocks. */
    public static final double DISTANCE_TERM = 10.0;

    private Scatter() {}

    /**
     * @param baseScatter the tier's base scatter, blocks
     * @param guidance    guidance tiers above the body's tier, 0 or more
     * @param distance    flat distance to the target
     * @param maxRange    the tier's range
     * @return the per-axis half-width in blocks, at least 0
     */
    public static double halfWidth(double baseScatter, int guidance, double distance, double maxRange) {
        double share = Math.max(MIN_SHARE, 1.0 - SHRINK_PER_TIER * Math.max(0, guidance));
        double ratio = maxRange > 0.0 ? Math.max(0.0, Math.min(1.0, distance / maxRange)) : 0.0;
        return Math.max(0.0, baseScatter) * share + DISTANCE_TERM * ratio * ratio;
    }

    /**
     * @param rng       the random source
     * @param halfWidth see {@link #halfWidth}
     * @return a signed offset, triangular, in {@code (-halfWidth, halfWidth)}
     */
    public static double offset(Random rng, double halfWidth) {
        return (rng.nextDouble() - rng.nextDouble()) * halfWidth;
    }
}
