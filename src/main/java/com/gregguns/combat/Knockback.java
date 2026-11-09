package com.gregguns.combat;

/**
 * The knockback multiplier of a gun, as plain arithmetic.
 *
 * <p>
 * Vanilla 1.7.10 {@code EntityLivingBase.knockBack} ignores its strength argument and always pushes by the same fixed
 * amount, so passing {@code damage x multiplier} (as Techguns does) changes nothing but zero versus non-zero (GUN-38).
 * GregGuns lets vanilla do its push inside {@code attackEntityFrom}, then reads how far the target's velocity moved and
 * scales that change: 0 cancels the push, 1 keeps it, 2 doubles it. This class holds the scaling;
 * {@code DamageSources.hit} applies it to the target's motion. No Minecraft types, both sides.
 *
 * <p>
 * Knockback is a server switch, {@code combat.knockback}, off by default (D357): off, every gun and projectile hit
 * leaves the target's velocity exactly as it was. On, the gun's multiplier is scaled by
 * {@code combat.knockbackMultiplier}. Explosion pushes are not hit knockback and stay on either way.
 *
 * <p>
 * Spec: docs/techguns-spec/02-guns/damage-system.md section 5.2 step 7; BUGS GUN-38. Design:
 * docs/design/0027-combat-difficulty-knobs.md.
 */
public final class Knockback {

    private Knockback() {}

    /**
     * Scales the change one hit made to one velocity component.
     *
     * @param before     the component before the hit, blocks per tick
     * @param after      the component after vanilla's push
     * @param multiplier the gun's knockback multiplier, at least 0
     * @return the component to set: {@code before + (after - before) x multiplier}; never NaN or infinite. If the
     *         scaled value is not finite (a mod left NaN in a velocity), the vanilla result {@code after} stands, or 0
     *         when that is not finite either
     */
    public static double scale(double before, double after, double multiplier) {
        double scaled = before + (after - before) * Math.max(0.0, multiplier);
        if (Double.isNaN(scaled) || Double.isInfinite(scaled)) {
            return Double.isNaN(after) || Double.isInfinite(after) ? 0.0 : after;
        }
        return scaled;
    }

    /**
     * The multiplier one hit uses.
     *
     * @param enabled          {@code combat.knockback}
     * @param configMultiplier {@code combat.knockbackMultiplier}
     * @param gunMultiplier    the gun's own knockback multiplier (1 for a hit with no gun stats)
     * @return 0 when knockback is off, otherwise {@code configMultiplier x gunMultiplier}, never negative or NaN
     */
    public static double effective(boolean enabled, double configMultiplier, double gunMultiplier) {
        if (!enabled) {
            return 0.0;
        }
        double m = configMultiplier * gunMultiplier;
        return Double.isNaN(m) || m < 0.0 ? 0.0 : m;
    }

    /**
     * @param multiplier the gun's knockback multiplier
     * @return true if the multiplier differs from vanilla's 1, so the velocity needs adjusting
     */
    public static boolean needsAdjustment(double multiplier) {
        return multiplier != 1.0;
    }
}
