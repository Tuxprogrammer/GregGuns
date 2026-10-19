package com.gregguns.api;

import net.minecraft.item.ItemStack;

/**
 * An item whose camouflage variant the Camo Bench can cycle. The index is stored in the stack, so cycling never
 * touches any other NBT. Both sides.
 *
 * <p>
 * Armor and guns implement it. A gun stores its camo index next to its rounds ({@code GunNbt.KEY_CAMO}); the count is
 * the gun definition's {@code camoCount}. The bench picks an item up through this interface with no further change.
 */
// Spec: docs/techguns-spec/09-machines-utility/camo-bench.md (ICamoChangeable)
public interface CamoChangeable {

    /**
     * @param stack a stack of this item
     * @return how many camos it has; 1 or less means there is nothing to cycle
     */
    int camoCount(ItemStack stack);

    /**
     * @param stack a stack of this item
     * @return the stored camo index, always inside {@code [0, camoCount)}
     */
    int getCamo(ItemStack stack);

    /**
     * @param stack a stack of this item
     * @param camo  the new index; an out-of-range value stores 0
     */
    void setCamo(ItemStack stack, int camo);

    /**
     * @param stack a stack of this item
     * @return the lang key of the current camo's name
     */
    String camoNameKey(ItemStack stack);
}
