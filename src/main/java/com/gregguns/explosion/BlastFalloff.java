package com.gregguns.explosion;

/**
 * The damage of an explosion at a distance from its centre, as pure logic: flat inside the inner radius, falling
 * linearly to zero at the outer radius, nothing beyond. No Minecraft types, both sides.
 *
 * <pre>
 * {@code
 * BlastFalloff.damage(20.0, 2.5, 7.5, 5.0); // 10.0, halfway down the ramp
 * }
 * </pre>
 */
// Spec: docs/techguns-spec/04-projectiles-fx/explosions.md (Part A, step 5)
public final class BlastFalloff {

    private BlastFalloff() {}

    /**
     * @param maxDamage   the damage at the centre, half hearts
     * @param innerRadius the radius up to which damage is full, blocks
     * @param outerRadius the radius at which damage reaches zero, blocks
     * @param distance    the distance from the centre, blocks
     * @return the damage at that distance, never negative
     */
    public static double damage(double maxDamage, double innerRadius, double outerRadius, double distance) {
        if (Double.isNaN(distance) || distance > outerRadius || maxDamage <= 0.0) {
            return 0.0;
        }
        if (distance <= innerRadius || outerRadius <= innerRadius) {
            return maxDamage;
        }
        return maxDamage * (1.0 - (distance - innerRadius) / (outerRadius - innerRadius));
    }
}
