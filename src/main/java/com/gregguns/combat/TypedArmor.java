package com.gregguns.combat;

import net.minecraft.item.ItemStack;

/**
 * The hook for GregGuns armor (M3). An armor item implements this to give per-type armor and penetration
 * resistance. The damage handler reads {@link #penetrationResistance} from every worn piece, once per hit. The armor
 * item still implements Forge's {@code ISpecialArmor} itself: its {@code getProperties} should return
 * {@link DamageModel#pieceAbsorbRatio} of {@link #armorValue} for the hit's type, and zero for an unblockable
 * source. Side: both.
 */
// Spec: docs/techguns-spec/02-guns/damage-system.md section 6 (TGArmorMaterial)
public interface TypedArmor {

    /**
     * @param stack the worn piece
     * @param type  the damage type of the hit
     * @return this piece's armor points against the type; 0 when nearly broken or not protective
     */
    double armorValue(ItemStack stack, DamageType type);

    /**
     * @param stack the worn piece
     * @param type  the damage type of the hit
     * @return this piece's contribution to penetration resistance, 0..1; the pieces' values are summed
     */
    double penetrationResistance(ItemStack stack, DamageType type);
}
