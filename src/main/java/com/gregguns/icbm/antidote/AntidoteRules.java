package com.gregguns.icbm.antidote;

/**
 * The pure numbers of the antidote (D151): how long the protection lasts and whether it still holds at a given time.
 * No Minecraft types, both sides.
 */
// Spec: docs/gregguns-icbm-spec/06-blocks-items/antidote.md
public final class AntidoteRules {

    /** Protection after a dose, in ticks (3 minutes). */
    public static final long PROTECTION_TICKS = 3600L;
    /** Ticks between guard sweeps of one player. */
    public static final int SWEEP_PERIOD = 10;
    /** Ticks the player holds the syringe before the dose goes in. */
    public static final int USE_TICKS = 32;
    /** Antidotes one Assembler run makes. */
    public static final int PER_RUN = 2;

    private AntidoteRules() {}

    /**
     * @param now the world time a dose is taken
     * @return the world time the protection ends
     */
    public static long protectedUntil(long now) {
        return now + PROTECTION_TICKS;
    }

    /**
     * @param until the stored end time, 0 for none
     * @param now   the world time
     * @return whether the protection still holds
     */
    public static boolean isProtected(long until, long now) {
        return until > now;
    }

    /**
     * @param until the stored end time
     * @param now   the world time
     * @return the seconds left, never negative
     */
    public static int secondsLeft(long until, long now) {
        return until > now ? (int) ((until - now + 19L) / 20L) : 0;
    }

    /**
     * @param now the world time
     * @return whether a sweep runs this tick
     */
    public static boolean sweepNow(long now) {
        return now % SWEEP_PERIOD == 0;
    }
}
