package com.gregguns.client.anim;

/**
 * Eases the field-of-view multiplier toward its target so entering and leaving a scope is smooth rather than a
 * snap. Pure: {@link #tick()} once per client tick, {@link #value(double)} each frame with the partial tick.
 *
 * <p>
 * The value is a plain FOV multiplier: 1.0 is no zoom, smaller is more zoomed in (the gun's zoom factor, for
 * example 0.35). Deviation: Techguns applies the factor instantly; the GregGuns gun-logic spec says the client eases
 * it itself.
 */
public final class ZoomAnimator {

    private static final double EASE = 0.55;
    private static final double SNAP = 0.002;

    private double target = 1.0;
    private double previous = 1.0;
    private double current = 1.0;

    /** Sets the multiplier to ease toward. Values at or below 0 are ignored. */
    public void setTarget(double multiplier) {
        if (multiplier > 0.0) {
            target = multiplier;
        }
    }

    /** Eases back to no zoom. */
    public void reset() {
        target = 1.0;
    }

    /** @return the multiplier being eased toward */
    public double target() {
        return target;
    }

    /** Advances one client tick. */
    public void tick() {
        previous = current;
        double diff = target - current;
        current = Math.abs(diff) < SNAP ? target : current + diff * EASE;
    }

    /** @return the multiplier for this frame, interpolated between the last two ticks */
    public double value(double partialTick) {
        return previous + (current - previous) * partialTick;
    }

    /** @return true when the view is at or easing toward a zoom (target below 1) */
    public boolean isZoomed() {
        return target < 1.0;
    }
}
