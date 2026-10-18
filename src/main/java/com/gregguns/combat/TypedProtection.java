package com.gregguns.combat;

/**
 * The hook for a living entity (an NPC, M7) that supplies its own per-type armour. When the target implements it, the
 * damage handler uses these values instead of vanilla's {@code getTotalArmorValue()}. Side: both.
 */
// Spec: docs/techguns-spec/02-guns/damage-system.md section 3 (INpcTGDamageSystem)
public interface TypedProtection {

    /**
     * @param type the damage type of the hit
     * @return armour points against the type (clamped to 0..{@link DamageModel#MAX_ARMOUR} by the model)
     */
    double armourValue(DamageType type);

    /**
     * @param type the damage type of the hit
     * @return penetration resistance against the type, 0..1
     */
    double penetrationResistance(DamageType type);
}
