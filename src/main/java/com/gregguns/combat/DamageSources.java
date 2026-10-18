package com.gregguns.combat;

import net.minecraft.entity.Entity;

/**
 * The factory for GregGuns damage. Projectiles, melee and explosions build their sources here and deal damage through
 * {@link #hit}; the rest of the model runs in the {@code LivingHurtEvent} handler ({@link CombatEvents}). Server side.
 *
 * <pre>
 * {@code
 * // in a bullet's onImpact, server side:
 * DamageSources.hit(target, DamageType.PROJECTILE, this, shooter, damage, 0.15);
 * }
 * </pre>
 */
// Spec: docs/gregguns-spec/02-guns/damage-interop.md; docs/design/0009 decision 3
public final class DamageSources {

    private DamageSources() {}

    /**
     * Builds a source for a projectile hit.
     *
     * @param type        the damage type
     * @param projectile  the projectile entity (the direct cause), may be {@code null} for a beam
     * @param shooter     the shooter (the attacker and killer), may be {@code null}
     * @param penetration the share of the damage that skips armour, 0..1
     * @return a fresh source; use it for one hit
     */
    public static RangedDamageSource ranged(DamageType type, Entity projectile, Entity shooter, double penetration) {
        return new RangedDamageSource(type, projectile, shooter, penetration);
    }

    /**
     * Builds a source for melee, an explosion or an area effect, where the attacker is the direct cause.
     *
     * @param type        the damage type
     * @param attacker    the attacker, may be {@code null}
     * @param penetration the share of the damage that skips armour, 0..1
     * @return a fresh source; use it for one hit
     */
    public static DirectDamageSource direct(DamageType type, Entity attacker, double penetration) {
        return new DirectDamageSource(type, attacker, penetration);
    }

    /**
     * The one call wave-2 projectiles make: builds a {@linkplain #ranged ranged source} and deals the damage through
     * vanilla's {@code attackEntityFrom}, so every listener sees the hit.
     *
     * @param target      the entity hit
     * @param type        the damage type
     * @param projectile  the projectile entity, may be {@code null}
     * @param shooter     the shooter, may be {@code null}
     * @param damage      raw damage before armour, after drop-off and any damage factor
     * @param penetration the share of the damage that skips armour, 0..1
     * @return what {@code attackEntityFrom} returned: true when the target took the hit
     */
    public static boolean hit(Entity target, DamageType type, Entity projectile, Entity shooter, float damage,
        double penetration) {
        return target.attackEntityFrom(ranged(type, projectile, shooter, penetration), damage);
    }
}
