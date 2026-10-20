package com.gregguns.client.anim;

/**
 * The first-person charge shift of a charge gun: while the use button is held the gun slides back along its own axis
 * toward the camera, in proportion to the charge, and eases back to rest when the charge ends or is cancelled. Pure:
 * {@link #tick(double)} once per client tick with the current charge fraction, {@link #value(double)} each frame with
 * the partial tick.
 *
 * <p>
 * The displayed fraction follows a rising charge exactly (the charge itself counts whole ticks; the partial tick
 * smooths the step) and falls at {@link #RETURN_PER_TICK} per tick once the charge drops, so a cancelled charge slides
 * home instead of snapping. A released charge fires, and the fire row takes over the pose at once.
 *
 * <p>
 * Spec: docs/techguns-spec/03-gun-visuals-audio/gun-sheets.md (Bio Gun, FP base) and
 * model-notes/bio_gun.md section 6. Deviation: the notes give no return time on release or cancel; the return takes
 * five ticks, the Bio Gun's recoil window.
 */
public final class ChargeShift {

    /** The slide at full charge, in blocks on screen: "up to 0.25 over the full charge". */
    public static final double MAX_BLOCKS = 0.25;
    /** How much of the displayed fraction is given back per tick once the charge drops (five ticks to rest). */
    public static final double RETURN_PER_TICK = 0.2;

    private double previous;
    private double current;

    /**
     * The slide toward the camera for a charge fraction: 0 with no charge, {@link #MAX_BLOCKS} at full charge, linear
     * between (the notes scale it with the charge).
     *
     * @param fraction the charge, 0 to 1; values outside are clamped
     * @return the slide in blocks, 0 to {@link #MAX_BLOCKS}
     */
    public static double offsetBlocks(double fraction) {
        return MAX_BLOCKS * clamp(fraction);
    }

    /**
     * Advances one client tick.
     *
     * @param target the charge fraction now, 0 when not charging
     */
    public void tick(double target) {
        previous = current;
        double t = clamp(target);
        current = t >= current ? t : Math.max(t, current - RETURN_PER_TICK);
    }

    /** @return the displayed charge fraction for this frame, interpolated between the last two ticks */
    public double value(double partialTick) {
        return previous + (current - previous) * partialTick;
    }

    /** Drops straight to rest (world unload). */
    public void reset() {
        previous = 0.0;
        current = 0.0;
    }

    private static double clamp(double v) {
        return v < 0.0 ? 0.0 : Math.min(v, 1.0);
    }
}
