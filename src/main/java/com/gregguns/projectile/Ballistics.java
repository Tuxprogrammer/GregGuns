package com.gregguns.projectile;

/**
 * The per-tick flight maths of a bullet: drag, gravity, distance traveled and the visual rotation. Plain Java with no
 * Minecraft types and no allocation, so the entity can call it every tick and tests can drive it directly. Units:
 * blocks, blocks per tick, degrees.
 */
// Spec: docs/techguns-spec/04-projectiles-fx/projectiles.md (2.3 Per-tick update, steps 7 to 9)
public final class Ballistics {

    /** Motion multiplier per tick in air. */
    public static final double AIR_DRAG = 0.99;
    /** Motion multiplier per tick in water. */
    public static final double WATER_DRAG = 0.8;
    /** How far the displayed rotation moves towards the flight direction each tick. */
    public static final float ROTATION_SMOOTHING = 0.2F;

    private Ballistics() {}

    /**
     * @param inWater whether the projectile is in water
     * @return the motion multiplier for this tick
     */
    public static double drag(boolean inWater) {
        return inWater ? WATER_DRAG : AIR_DRAG;
    }

    /**
     * The vertical motion after drag and gravity, in that order.
     *
     * @param motionY current vertical motion
     * @param drag    from {@link #drag(boolean)}
     * @param gravity blocks per tick squared, 0 for a straight bullet
     * @return the motion for the next tick
     */
    public static double nextMotionY(double motionY, double drag, double gravity) {
        return motionY * drag - gravity;
    }

    /**
     * @param ax first point X
     * @param ay first point Y
     * @param az first point Z
     * @param bx second point X
     * @param by second point Y
     * @param bz second point Z
     * @return the straight-line distance between two points, in blocks
     */
    public static double distance(double ax, double ay, double az, double bx, double by, double bz) {
        double dx = bx - ax;
        double dy = by - ay;
        double dz = bz - az;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * @param motionX motion X
     * @param motionZ motion Z
     * @return the yaw in degrees of a motion vector, using the projectile convention {@code atan2(x, z)}
     */
    public static float yawOf(double motionX, double motionZ) {
        return (float) Math.toDegrees(Math.atan2(motionX, motionZ));
    }

    /**
     * @param motionX motion X
     * @param motionY motion Y
     * @param motionZ motion Z
     * @return the pitch in degrees of a motion vector, positive upwards, using {@code atan2(y, horizontal)}
     */
    public static float pitchOf(double motionX, double motionY, double motionZ) {
        return (float) Math.toDegrees(Math.atan2(motionY, Math.sqrt(motionX * motionX + motionZ * motionZ)));
    }

    /**
     * Moves a displayed angle a fifth of the way towards the target, taking the short way round the circle.
     *
     * @param previous the angle shown last tick, degrees
     * @param target   the angle of the flight direction, degrees
     * @return the new displayed angle; it may leave -180..180, which keeps interpolation between ticks smooth
     */
    public static float smoothAngle(float previous, float target) {
        float prev = previous;
        while (target - prev < -180.0F) {
            prev -= 360.0F;
        }
        while (target - prev >= 180.0F) {
            prev += 360.0F;
        }
        return prev + (target - prev) * ROTATION_SMOOTHING;
    }
}
