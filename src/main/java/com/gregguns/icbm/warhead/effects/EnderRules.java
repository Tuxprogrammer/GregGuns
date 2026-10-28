package com.gregguns.icbm.warhead.effects;

/**
 * The pure arithmetic of the ender warhead (D80, D10): how hard the blast pulls an entity toward the center, how near
 * the center an entity must be to be teleported, and where the one teleport target lies. No Minecraft types, both
 * sides.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/blast-types.md (Ender)
public final class EnderRules {

    /** The blast runs this long, ticks (8 s). */
    public static final int DURATION_TICKS = 160;
    /** An entity this close to the center is teleported, blocks. */
    public static final double TELEPORT_RANGE = 4.0;
    /** The target lies at most this far from the center on each horizontal axis, blocks. */
    public static final int TARGET_SPREAD = 128;
    /** The target lies at least this many blocks from the center on one axis, so the landing is outside the pull. */
    public static final int TARGET_MIN = 40;
    /** Most entities the pull handles in one tick; a crowd beyond it is left alone that tick. */
    public static final int MAX_PULLED = 128;
    /** Attempts at finding a safe landing before the blast gives up and only pulls. */
    public static final int TARGET_ATTEMPTS = 16;
    /** Vertical pull relative to the horizontal one (ICBM's 0.0011 over 0.0006, rounded). */
    public static final double VERTICAL_FACTOR = 1.8;

    private EnderRules() {}

    /** Highest Y a landing scan starts at in a dimension with a bedrock roof (the Nether's ceiling is at 127). */
    public static final int ROOFED_SCAN_TOP = 120;

    /**
     * @param hasNoSky    whether the dimension has a ceiling and no sky (the Nether)
     * @param heightValue the world's height value for the column, which in such a dimension is the roof
     * @return the Y the downward search for a landing floor starts at: never on or above the roof
     */
    public static int scanTop(boolean hasNoSky, int heightValue) {
        int top = Math.min(250, heightValue);
        return hasNoSky ? Math.min(top, ROOFED_SCAN_TOP) : top;
    }

    /**
     * The change of one velocity component for an entity at {@code delta} blocks from the center on that axis. Zero at
     * the center and at the radius, strongest halfway; it always points at the center. With radius 20 this is ICBM's
     * {@code 0.0006} curve (peak 0.06 blocks per tick); other radii scale it so the peak stays the same.
     *
     * @param delta  entity minus center on the axis
     * @param radius the blast radius
     * @return the velocity change, blocks per tick
     */
    public static double pull(double delta, double radius) {
        double a = Math.abs(delta);
        if (!(radius > 0.0) || a >= radius) {
            return 0.0;
        }
        double sign = delta < 0.0 ? -1.0 : 1.0;
        return -(sign * radius - delta) * a * 0.24 / (radius * radius);
    }

    /**
     * One axis of the teleport target offset.
     *
     * @param u uniform random in [0, 1)
     * @return an offset whose magnitude is between {@link #TARGET_MIN} and {@link #TARGET_SPREAD}
     */
    public static int offset(double u) {
        double clamped = Math.max(0.0, Math.min(0.999999, u));
        boolean negative = clamped < 0.5;
        double frac = negative ? (0.5 - clamped) * 2.0 : (clamped - 0.5) * 2.0;
        int magnitude = TARGET_MIN + (int) Math.floor(frac * (TARGET_SPREAD - TARGET_MIN));
        return negative ? -magnitude : magnitude;
    }

    /**
     * @param dx entity minus center X
     * @param dy entity minus center Y
     * @param dz entity minus center Z
     * @return true when the entity is close enough to be teleported
     */
    public static boolean inTeleportRange(double dx, double dy, double dz) {
        return dx * dx + dy * dy + dz * dz <= TELEPORT_RANGE * TELEPORT_RANGE;
    }
}
