package com.gregguns.client.anim;

/**
 * The transition curve of aiming down the sights: a 0 to 1 blend that rises while the player aims and falls when they
 * stop, eased with a smoothstep so the gun leaves and reaches the center gently. Pure: {@link #tick} once per client
 * tick, {@link #value} each frame with the partial tick.
 *
 * <p>
 * 0 is the normal hold and 1 is fully aligned. The blend moves {@code 1 / ticks} per tick toward its target, so a
 * half-finished move can reverse from where it is instead of jumping. The value between ticks is interpolated, so the
 * motion is smooth at any frame rate.
 *
 * <p>
 * Spec: docs/gregguns-spec/03-gun-visuals-audio/ads.md.
 */
public final class AdsBlend {

    private double previous;
    private double current;
    private boolean aiming;

    /** @param aim true to blend toward the sights, false to blend back to the normal hold */
    public void setAiming(boolean aim) {
        aiming = aim;
    }

    /** @return true while the target is the sights */
    public boolean isAiming() {
        return aiming;
    }

    /** Snaps back to the normal hold with no animation (death, weapon swap, world unload). */
    public void reset() {
        aiming = false;
        previous = 0.0;
        current = 0.0;
    }

    /**
     * Advances one client tick.
     *
     * @param ticks ticks a full move takes, at least 1
     */
    public void tick(int ticks) {
        previous = current;
        double step = 1.0 / Math.max(1, ticks);
        current = aiming ? Math.min(1.0, current + step) : Math.max(0.0, current - step);
    }

    /**
     * @param partialTick the fraction of the tick that has passed
     * @return the eased blend for this frame, in {@code [0, 1]}
     */
    public double value(double partialTick) {
        double p = previous + (current - previous) * Math.max(0.0, Math.min(1.0, partialTick));
        return smooth(p);
    }

    /** @return true when the gun is at the normal hold and not heading to the sights, so nothing needs drawing */
    public boolean isIdle() {
        return !aiming && current == 0.0 && previous == 0.0;
    }

    /** @return the smoothstep of a linear progress in {@code [0, 1]} */
    static double smooth(double p) {
        double x = Math.max(0.0, Math.min(1.0, p));
        return x * x * (3.0 - 2.0 * x);
    }
}
