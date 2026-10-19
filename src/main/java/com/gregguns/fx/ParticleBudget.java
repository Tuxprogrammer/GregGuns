package com.gregguns.fx;

import java.util.function.IntSupplier;

/**
 * The global cap on live particles (GGS-04-08). Bursts keep their full size while the count is low, shrink linearly
 * as it nears the ceiling, and are cut to the room left, so the ceiling is never passed. Pure; touched only from the
 * client thread (GGS-04-09).
 *
 * <p>
 * The soft limit, where shrinking starts, is half the ceiling.
 */
// Spec: docs/gregguns-spec/04-projectiles-fx/particle-rendering.md (GGS-04-08, GGS-04-09)
public final class ParticleBudget implements EmitterSim.Budget {

    /** Lowest accepted ceiling (the config range is 100 to 8000). */
    public static final int MIN_CEILING = 100;
    /** Highest accepted ceiling. */
    public static final int MAX_CEILING = 8000;

    private final IntSupplier ceiling;
    private int live;
    private int peak;
    private long granted;
    private long trimmed;

    /**
     * @param ceiling supplies the ceiling each time it is needed, so a config change applies at once
     */
    public ParticleBudget(IntSupplier ceiling) {
        this.ceiling = ceiling;
    }

    /**
     * The burst size allowed at a given load. Pure function; the instance methods use it.
     *
     * @param requested what the burst wants
     * @param live      particles alive now
     * @param ceiling   the hard cap, clamped to the accepted range
     * @return 0 to {@code requested}; at most {@code ceiling - live}
     */
    public static int allowed(int requested, int live, int ceiling) {
        int cap = Math.max(MIN_CEILING, Math.min(MAX_CEILING, ceiling));
        int room = cap - live;
        if (requested <= 0 || room <= 0) {
            return 0;
        }
        int soft = cap / 2;
        double scale = live <= soft ? 1.0 : (double) room / (cap - soft);
        int scaled = (int) Math.ceil(requested * scale);
        return Math.min(room, Math.min(requested, scaled));
    }

    @Override
    public int grant(int requested) {
        int n = allowed(requested, live, ceiling.getAsInt());
        live += n;
        peak = Math.max(peak, live);
        granted += n;
        if (n < requested) {
            trimmed += requested - n;
        }
        return n;
    }

    /**
     * Records that particles died or were dropped.
     *
     * @param count how many left; never takes the count below zero
     */
    public void released(int count) {
        live = Math.max(0, live - count);
    }

    /**
     * Sets the live count from an authoritative recount.
     *
     * @param count particles actually alive
     */
    public void resync(int count) {
        live = Math.max(0, count);
    }

    /** @return particles alive now */
    public int live() {
        return live;
    }

    /** @return the highest live count seen */
    public int peak() {
        return peak;
    }

    /** @return particles created in total */
    public long granted() {
        return granted;
    }

    /** @return particles requested but not created, because of the budget */
    public long trimmed() {
        return trimmed;
    }
}
