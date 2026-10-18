package com.gregguns.combat;

/**
 * The knockback multiplier of a gun, as plain arithmetic.
 *
 * <p>
 * Vanilla 1.7.10 {@code EntityLivingBase.knockBack} ignores its strength argument and always pushes by the same fixed
 * amount, so passing {@code damage x multiplier} (as Techguns does) changes nothing but zero versus non-zero (GUN-38).
 * GregGuns lets vanilla do its push inside {@code attackEntityFrom}, then reads how far the target's velocity moved and
 * scales that change: 0 cancels the push, 1 keeps it, 2 doubles it. This class holds the scaling; the projectile
 * applies it to the target's motion. No Minecraft types, both sides.
 *
 * <p>
 * Spec: docs/techguns-spec/02-guns/damage-system.md section 5.2 step 7; BUGS GUN-38.
 */
public final class Knockback {

    private Knockback() {}

    /**
     * Scales the change one hit made to one velocity component.
     *
     * @param before     the component before the hit, blocks per tick
     * @param after      the component after vanilla's push
     * @param multiplier the gun's knockback multiplier, at least 0
     * @return the component to set: {@code before + (after - before) x multiplier}
     */
    public static double scale(double before, double after, double multiplier) {
        return before + (after - before) * Math.max(0.0, multiplier);
    }

    /**
     * @param multiplier the gun's knockback multiplier
     * @return true if the multiplier differs from vanilla's 1, so the velocity needs adjusting
     */
    public static boolean needsAdjustment(double multiplier) {
        return multiplier != 1.0;
    }
}
