package com.gregguns.armor.flight.logic;

/**
 * The motion formulas of the three back devices, in blocks per tick. Pure functions: callers pass the player's motion
 * in and add the answer to it, on whichever side owns the motion (the client for a real player, the server for a
 * scenario's fake one). No Minecraft types.
 */
// Spec: docs/techguns-spec/05-armor/special-equipment.md 4.2 (glider), 4.4 (jump pack), 4.5 (anti gravity device);
// docs/gregguns-spec/05-armor/powered-flight.md
// Deviation: the anti gravity device is motion-based (GGS-05-17), not vanilla creative flight; its numbers are ours.
public final class FlightPhysics {

    /** Vertical speed below which the glider starts to bite (Techguns 4.2). */
    public static final double GLIDE_START = -0.1;
    /** Most fall speed the glider converts per tick (Techguns 4.2). */
    public static final double GLIDE_MAX_LIFT = 0.08;
    /** Extra air acceleration while gliding (Techguns: jumpMovementFactor + 0.03). */
    public static final double GLIDE_AIR_CONTROL = 0.03;
    /** Upward motion the Jump Pack adds to a jump (Techguns 4.4). */
    public static final double JUMP_BOOST = 0.8;
    /** Top climb speed of the anti gravity device, blocks per tick. */
    public static final double FLY_RISE = 0.3;
    /** Top descent speed of the anti gravity device, blocks per tick. */
    public static final double FLY_SINK = 0.3;
    /** How fast vertical flight speed approaches its target, blocks per tick per tick. */
    public static final double FLY_VERTICAL_ACCEL = 0.5;
    /** Vanilla airborne acceleration ({@code jumpMovementFactor}). */
    public static final double AIR_ACCEL = 0.02;
    /** Vanilla creative fly speed. */
    public static final double CREATIVE_FLY_SPEED = 0.05;
    /** Fly-speed bonus of the anti gravity device (Techguns 4.5: +25%). */
    public static final double FLY_SPEED_BONUS = 0.25;
    /** Vanilla gravity per tick, for simulations. */
    public static final double GRAVITY = 0.08;
    /** Vanilla vertical drag per tick, for simulations. */
    public static final double DRAG = 0.98;

    private FlightPhysics() {}

    /** An amount to add to a player's motion. */
    public static final class Impulse {

        /** No change. */
        public static final Impulse NONE = new Impulse(0.0, 0.0, 0.0);

        /** East-west component. */
        public final double x;
        /** Vertical component. */
        public final double y;
        /** North-south component. */
        public final double z;

        /**
         * @param x east-west
         * @param y vertical
         * @param z north-south
         */
        public Impulse(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    /**
     * The glider's per-tick change: part of the fall speed becomes forward speed along the horizontal look direction,
     * capping the descent near 0.18 blocks per tick once vanilla gravity is counted.
     *
     * @param motionY the player's vertical motion
     * @param lookX   look vector x
     * @param lookZ   look vector z
     * @return the impulse; {@link Impulse#NONE} unless falling faster than {@link #GLIDE_START}
     */
    public static Impulse glide(double motionY, double lookX, double lookZ) {
        if (motionY >= GLIDE_START) {
            return Impulse.NONE;
        }
        double m = Math.min(GLIDE_MAX_LIFT, GLIDE_START - motionY);
        double len = Math.sqrt(lookX * lookX + lookZ * lookZ);
        if (len < 1.0e-6) {
            return new Impulse(0.0, m, 0.0);
        }
        return new Impulse(lookX / len * m, m, lookZ / len * m);
    }

    /**
     * The anti gravity device's vertical motion for one tick: it eases toward a climb, a descent or a hover.
     *
     * @param motionY the player's vertical motion
     * @param up      the jump key is held
     * @param down    the sneak key is held; holding both hovers
     * @return the new vertical motion
     */
    public static double verticalFlight(double motionY, boolean up, boolean down) {
        double target = up == down ? 0.0 : up ? FLY_RISE : -FLY_SINK;
        return approach(motionY, target, FLY_VERTICAL_ACCEL);
    }

    /**
     * Horizontal acceleration from the movement keys, the way {@code Entity.moveFlying} does it, for flight and glide
     * air control.
     *
     * @param forward  movement input forward, -1 to 1
     * @param strafe   movement input to the left, -1 to 1
     * @param yawDeg   the player's yaw in degrees
     * @param strength acceleration at full input, blocks per tick per tick
     * @return the impulse (x and z only)
     */
    public static Impulse steer(double forward, double strafe, double yawDeg, double strength) {
        double f = strafe * strafe + forward * forward;
        if (f < 1.0e-4 || strength <= 0.0) {
            return Impulse.NONE;
        }
        f = Math.sqrt(f);
        if (f < 1.0) {
            f = 1.0;
        }
        f = strength / f;
        double s = strafe * f;
        double fw = forward * f;
        double rad = Math.toRadians(yawDeg);
        double sin = Math.sin(rad);
        double cos = Math.cos(rad);
        return new Impulse(s * cos - fw * sin, 0.0, fw * cos + s * sin);
    }

    /** @return the extra air acceleration of the anti gravity device over vanilla's {@link #AIR_ACCEL}, per tick */
    public static double flyAcceleration() {
        return CREATIVE_FLY_SPEED * (1.0 + FLY_SPEED_BONUS) - AIR_ACCEL;
    }

    /**
     * @param value  the current value
     * @param target the value to approach
     * @param step   the most it may move
     * @return {@code value} moved toward {@code target} by at most {@code step}
     */
    static double approach(double value, double target, double step) {
        if (value < target) {
            return Math.min(target, value + step);
        }
        return Math.max(target, value - step);
    }
}
