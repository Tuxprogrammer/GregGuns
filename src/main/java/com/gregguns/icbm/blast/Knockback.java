package com.gregguns.icbm.blast;

/**
 * The push a conventional ICBM blast gives an entity: away from the center, strongest in the core and falling to zero
 * at the blast radius, on the same ramp as the damage ({@link DamageRamp}). Pure math. No Minecraft types, both sides.
 *
 * <pre>
 * 
 * {
 *     &#64;code
 *     double speed = Knockback.speed(20.0, 0.0); // MAX_SPEED at the center
 *     double[] v = Knockback.velocity(20.0, 5, 64, 0, 0, 0); // pushed along the offset
 * }
 * </pre>
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/blast-engine.md (knockback)
public final class Knockback {

    /** Push speed at the center, blocks per tick. Large enough to throw a player clear, small enough not to clip. */
    public static final double MAX_SPEED = 2.0;

    private Knockback() {}

    /**
     * @param radius   the blast radius, blocks
     * @param distance distance from the center, blocks
     * @return the push speed there, blocks per tick; zero at and beyond the radius
     */
    public static double speed(double radius, double distance) {
        return DamageRamp.at(MAX_SPEED, radius, distance);
    }

    /**
     * @param radius the blast radius, blocks
     * @param dx     entity offset from the center, X
     * @param dy     entity offset from the center, Y
     * @param dz     entity offset from the center, Z
     * @return the velocity to add: along the offset (straight up when the entity is exactly at the center), or all
     *         zeros outside the radius
     */
    public static double[] velocity(double radius, double dx, double dy, double dz) {
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double speed = speed(radius, length);
        if (speed <= 0.0) {
            return new double[] { 0.0, 0.0, 0.0 };
        }
        if (length < 1e-6) {
            return new double[] { 0.0, speed, 0.0 };
        }
        return new double[] { dx / length * speed, dy / length * speed, dz / length * speed };
    }
}
