package com.gregguns.armor.logic;

/**
 * The HEV's swim boost, as pure math. Vanilla swimming ignores the movement speed attribute (it accelerates by a fixed
 * 0.02 per tick), so the old "speed attribute plus SPEED_WATER" only warped the FOV (ARM-05). This adds extra
 * acceleration along the player's input direction instead, and caps the horizontal speed.
 *
 * <p>
 * Plain Java, no Minecraft types. Runs on the client, where the local player's movement is computed.
 */
// Spec: docs/techguns-spec/05-armor/index.md ARM-05
public final class SwimBoost {

    /** Swim speed multiplier per point of SPEED_WATER: multiplier = 1 + this * SPEED_WATER (HEV 5.0 gives x2.0). */
    public static final double MULTIPLIER_PER_POINT = 0.2;

    /** Vanilla's swim acceleration per tick (EntityLivingBase.moveEntityWithHeading water branch). */
    public static final double VANILLA_ACCEL = 0.02;

    /**
     * Horizontal speed cap in blocks per tick (vanilla terminal swim speed is about 0.1), so the boost cannot run away.
     */
    public static final double MAX_SPEED = 0.25;

    private SwimBoost() {}

    /** @return the swim speed multiplier for a SPEED_WATER bonus; 1.0 for none */
    public static double multiplier(double speedWater) {
        return speedWater > 0.0 ? 1.0 + MULTIPLIER_PER_POINT * speedWater : 1.0;
    }

    /**
     * The extra horizontal motion to add this tick, mirroring {@code Entity.moveFlying}: the input is normalized, then
     * rotated by the yaw. Zero without input, so letting go of the keys adds nothing.
     *
     * @param strafe     left-right input
     * @param forward    forward-back input
     * @param yawDegrees the player's yaw
     * @param multiplier see {@link #multiplier}
     * @return {x, z} to add to the motion
     */
    public static double[] extraMotion(double strafe, double forward, double yawDegrees, double multiplier) {
        double dist = strafe * strafe + forward * forward;
        if (dist < 1.0E-4 || multiplier <= 1.0) {
            return new double[] { 0.0, 0.0 };
        }
        dist = Math.sqrt(dist);
        if (dist < 1.0) {
            dist = 1.0;
        }
        double accel = VANILLA_ACCEL * (multiplier - 1.0) / dist;
        double s = strafe * accel;
        double f = forward * accel;
        double rad = Math.toRadians(yawDegrees);
        double sin = Math.sin(rad);
        double cos = Math.cos(rad);
        return new double[] { s * cos - f * sin, f * cos + s * sin };
    }

    /**
     * Scales a horizontal motion down to {@link #MAX_SPEED} when it is faster; slower motion is returned unchanged.
     *
     * @return {x, z}
     */
    public static double[] cap(double x, double z) {
        double speed = Math.sqrt(x * x + z * z);
        if (speed <= MAX_SPEED) {
            return new double[] { x, z };
        }
        double k = MAX_SPEED / speed;
        return new double[] { x * k, z * k };
    }
}
