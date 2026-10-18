package com.gregguns.client.anim;

/**
 * One timed animation window (recoil, muzzle flash or reload). Started with a duration in ticks, read back as a
 * progress from 0 to 1. Pure: the caller supplies {@code now} as {@code tick + partialTick}.
 *
 * <p>
 * Spec: rendering-framework.md section 3: {@code progress = 1 - (end - now) / total}; an expired window is cleared.
 */
public final class AnimWindow {

    /** Returned by {@link #progress} when the window is not running. */
    public static final double NONE = -1.0;

    private double end;
    private double total;
    private boolean active;

    /** Starts (or restarts) the window. A non-positive duration clears it. */
    public void begin(double now, double durationTicks) {
        if (durationTicks <= 0.0) {
            clear();
            return;
        }
        total = durationTicks;
        end = now + durationTicks;
        active = true;
    }

    /** Stops the window. */
    public void clear() {
        active = false;
    }

    /**
     * @return progress in [0, 1), or {@link #NONE} when not running. A window whose remaining time is not positive,
     *         or exceeds its length (the clock went backwards), is expired and cleared.
     */
    public double progress(double now) {
        if (!active) {
            return NONE;
        }
        double remaining = end - now;
        if (remaining <= 0.0 || remaining > total) {
            active = false;
            return NONE;
        }
        return 1.0 - remaining / total;
    }

    /** @return true while running at {@code now}; clears the window if it has expired */
    public boolean isActive(double now) {
        return progress(now) != NONE;
    }
}
