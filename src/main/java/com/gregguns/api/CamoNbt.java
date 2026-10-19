package com.gregguns.api;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Null-safe reading and writing of a camo index in a stack's NBT, shared by the camo items. A null stack or a stack
 * without a tag reads as the default camo (0) and a write to a null stack is ignored, so callers such as the Camo Bench
 * never throw on an empty slot.
 */
public final class CamoNbt {

    private CamoNbt() {}

    /**
     * @param stack the stack, may be null
     * @param key   the NBT key
     * @param count how many camos the item has
     * @return the stored index, or 0 for a null stack, no tag, or an out-of-range value
     */
    public static int get(ItemStack stack, String key, int count) {
        if (stack == null) {
            return 0;
        }
        NBTTagCompound tag = stack.getTagCompound();
        int camo = tag == null ? 0 : tag.getInteger(key);
        return camo >= 0 && camo < count ? camo : 0;
    }

    /**
     * Stores a camo index; out-of-range values store 0. Does nothing for a null stack.
     *
     * @param stack the stack, may be null
     * @param key   the NBT key
     * @param count how many camos the item has
     * @param camo  the index to store
     */
    public static void set(ItemStack stack, String key, int count, int camo) {
        if (stack == null) {
            return;
        }
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        tag.setInteger(key, camo >= 0 && camo < count ? camo : 0);
    }
}
