package com.gregguns.combat;

/**
 * The hook for a living entity (an NPC, M7) that supplies its own per-type armor. When the target implements it, the
 * damage handler uses these values instead of vanilla's {@code getTotalArmorValue()}. Side: both.
 */
// Spec: docs/techguns-spec/02-guns/damage-system.md section 3 (INpcTGDamageSystem)
public interface TypedProtection {

    /**
     * @param type the damage type of the hit
     * @return armor points against the type (clamped to 0..{@link DamageModel#MAX_ARMOR} by the model)
     */
    double armorValue(DamageType type);

    /**
     * @param type the damage type of the hit
     * @return penetration resistance against the type, 0..1
     */
    double penetrationResistance(DamageType type);
}
