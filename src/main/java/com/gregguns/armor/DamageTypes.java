package com.gregguns.armor;

import net.minecraft.util.DamageSource;

import com.gregguns.combat.DamageType;
import com.gregguns.combat.GunDamageSource;

/**
 * Maps any damage source to the GregGuns {@link DamageType} whose armor value applies. GregGuns sources carry their
 * type; vanilla and other mods' sources are classified by their flags. Server and client.
 *
 * <p>
 * Deviation: Techguns converted unknown sources to a TG source with a per-config guess. Here the rule is fixed:
 * explosion, then fire, then projectile, then magic (as Energy), else Physical. Unblockable sources never reach armor
 * values (the pieces answer zero for them).
 */
// Spec: docs/techguns-spec/05-armor/armor-catalog.md section 3 (getProperties, step 1)
public final class DamageTypes {

    private DamageTypes() {}

    /**
     * @param source any damage source
     * @return the damage type to read armor values for
     */
    public static DamageType of(DamageSource source) {
        if (source instanceof GunDamageSource) {
            return ((GunDamageSource) source).hit()
                .type();
        }
        if (source.isExplosion()) {
            return DamageType.EXPLOSION;
        }
        if (source.isFireDamage()) {
            return DamageType.FIRE;
        }
        if (source.isProjectile()) {
            return DamageType.PROJECTILE;
        }
        if (source.isMagicDamage()) {
            return DamageType.ENERGY;
        }
        return DamageType.PHYSICAL;
    }
}
