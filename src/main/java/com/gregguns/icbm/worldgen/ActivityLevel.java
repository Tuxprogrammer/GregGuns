package com.gregguns.icbm.worldgen;

/**
 * How lived-in a silo complex is (D36). Rolled once per complex from its seed, so every populate window that builds a
 * piece of the complex agrees. Pure.
 *
 * <p>
 * Usage: {@code ActivityLevel.roll(seed, ActivityLevel.DEFAULT_WEIGHTS)}, then {@link ActivityProfile#of} for the
 * rates that level uses.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/activity-levels.md (GGI-08-20..22); D36
public enum ActivityLevel {

    /** Heavily damaged, no spawners, sparse loot. */
    ABANDONED,
    /** Moderate damage, guards in the deeper sections, better loot. */
    PARTLY_ABANDONED,
    /** Intact and lit, guards everywhere, the best loot. */
    OPERATIONAL;

    /** Default weights in percent: abandoned, partly abandoned, operational (D36). */
    public static final int[] DEFAULT_WEIGHTS = { 50, 35, 15 };

    /**
     * @param configured the weights from the config file
     * @return them when they are three weights, 0 or more, with at least one above 0; else {@link #DEFAULT_WEIGHTS}
     */
    public static int[] weightsFrom(int[] configured) {
        if (configured == null || configured.length != 3) {
            return DEFAULT_WEIGHTS;
        }
        long sum = 0;
        for (int w : configured) {
            if (w < 0) {
                return DEFAULT_WEIGHTS;
            }
            sum += w;
        }
        return sum > 0 ? configured : DEFAULT_WEIGHTS;
    }

    /**
     * @param seed    the complex's seed
     * @param weights three weights in the enum's order; negatives count as 0; null or all zero gives abandoned
     * @return the rolled level
     */
    public static ActivityLevel roll(long seed, int[] weights) {
        long total = 0;
        for (int i = 0; i < 3; i++) {
            total += at(weights, i);
        }
        if (total <= 0) {
            return ABANDONED;
        }
        long pick = Long.remainderUnsigned(Mix.of(seed, 0x51, 0, 0), total);
        for (int i = 0; i < 3; i++) {
            pick -= at(weights, i);
            if (pick < 0) {
                return values()[i];
            }
        }
        return ABANDONED;
    }

    private static int at(int[] w, int i) {
        return w == null || i >= w.length ? 0 : Math.max(0, w[i]);
    }

    /**
     * @param name a saved name; may be null
     * @return the level, or null when unknown
     */
    public static ActivityLevel byName(String name) {
        for (ActivityLevel l : values()) {
            if (l.name()
                .equals(name)) {
                return l;
            }
        }
        return null;
    }
}
