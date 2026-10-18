package com.gregguns.combat;

/**
 * The default secondary effect of each damage type. Plain Java. The values are placeholders for the M9 balance pass.
 */
// Spec: docs/gregguns-spec/02-guns/damage-interop.md (hazmat mapping)
// Deviation: Techguns sets these per projectile; GregGuns keeps a per-type default that a projectile can switch off
// with GunDamageSource.withoutSecondaryEffects(). Electric shock has no Techguns equivalent.
public final class SecondaryEffects {

    private static final SecondaryEffect FIRE = new SecondaryEffect(SecondaryEffect.Kind.IGNITE, 100, 0);
    private static final SecondaryEffect ICE = new SecondaryEffect(SecondaryEffect.Kind.SLOWNESS, 100, 1);
    private static final SecondaryEffect POISON = new SecondaryEffect(SecondaryEffect.Kind.POISON, 100, 0);
    private static final SecondaryEffect RADIATION = new SecondaryEffect(
        SecondaryEffect.Kind.RADIATION_SICKNESS,
        200,
        0);
    private static final SecondaryEffect SHOCK = new SecondaryEffect(SecondaryEffect.Kind.SHOCK, 60, 0);

    private SecondaryEffects() {}

    /**
     * @param type a damage type
     * @return its default secondary effect, or {@code null} when it has none
     */
    public static SecondaryEffect forType(DamageType type) {
        switch (type) {
            case FIRE:
                return FIRE;
            case ICE:
                return ICE;
            case POISON:
                return POISON;
            case RADIATION:
                return RADIATION;
            case LIGHTNING:
                return SHOCK;
            default:
                return null;
        }
    }
}
