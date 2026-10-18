package com.gregguns.armor.slots;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;

/**
 * An item that gives night vision while worn, switched on and powered: the Night Vision Goggles and the Tactical Mask.
 * {@link SlotEquipment#tick} asks each worn one once per server tick and feeds the answer into the armor module's
 * single night-vision decision ({@link com.gregguns.armor.logic.NightVisionPolicy}), so a potion's night vision is
 * never
 * removed (CORE-15, ARM-29).
 *
 * <p>
 * Server side.
 */
// Spec: docs/gregguns-spec/05-armor/baubles-slots.md (Night vision); docs/techguns-spec/05-armor/special-equipment.md
// 3.2
public interface NightVisionGear {

    /**
     * @param stack the worn stack
     * @return the switch state; a stack that never had one is on
     */
    boolean nightVisionEnabled(ItemStack stack);

    /**
     * @param stack the worn stack
     * @param on    the new switch state
     */
    void setNightVisionEnabled(ItemStack stack, boolean on);

    /**
     * Pays one tick of upkeep when the gear is on and has the charge.
     *
     * @param stack  the worn stack
     * @param wearer the wearer
     * @return true when night vision works this tick (it was on and the EU was paid)
     */
    boolean tickNightVision(ItemStack stack, EntityLivingBase wearer);
}
