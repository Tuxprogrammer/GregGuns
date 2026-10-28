package com.gregguns.icbm.defense;

/**
 * How far a radar dish has turned (D125) as plain data. The dish turns at a fixed rate while the radar scans and
 * holds still otherwise, so its angle is a function of the world's game time and two numbers: the angle it had when
 * the state last changed and the tick that change happened. The server sends those two numbers (with the spinning
 * flag) when the state changes and about once a second; every client then draws the same angle at the same tick with
 * no per-tick traffic. No Minecraft types, both sides.
 */
// Spec: docs/gregguns-icbm-spec/05-detection-defense/radar.md (The dish)
public final class DishSpin {

    /** Degrees a spinning dish turns each tick: one turn in three seconds. */
    public static final float DEGREES_PER_TICK = 6.0F;

    /** Ticks in one full turn at {@link #DEGREES_PER_TICK}; the elapsed time is reduced by it so floats stay exact. */
    private static final long TICKS_PER_TURN = 60L;

    private boolean spinning;
    private float baseAngle;
    private long since;

    /**
     * @param now the game time
     * @return the angle in degrees, 0 up to 360, at that tick
     */
    public float angle(long now) {
        float a = spinning ? baseAngle + ((now - since) % TICKS_PER_TURN) * DEGREES_PER_TICK : baseAngle;
        a %= 360.0F;
        return a < 0.0F ? a + 360.0F : a;
    }

    /**
     * Server side: switches the dish on or off. Does nothing when the flag already has that value, so the angle never
     * jumps.
     *
     * @param on  whether the dish should turn
     * @param now the game time
     * @return true when the state changed
     */
    public boolean set(boolean on, long now) {
        if (on == spinning) {
            return false;
        }
        baseAngle = angle(now);
        since = now;
        spinning = on;
        return true;
    }

    /**
     * Client side: takes the state the server sent.
     *
     * @param on    whether the dish turns
     * @param angle the angle at the change
     * @param when  the game time of the change
     */
    public void restore(boolean on, float angle, long when) {
        spinning = on;
        baseAngle = angle;
        since = when;
    }

    /** @return whether the dish is turning */
    public boolean spinning() {
        return spinning;
    }

    /** @return the angle the dish had when the state last changed */
    public float baseAngle() {
        return baseAngle;
    }

    /** @return the game time of the last change */
    public long since() {
        return since;
    }
}
