package com.gregguns.icbm.warhead;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Holds the inert warhead item and reads and writes its stacks (D282). The damage value is the casing (0 for T1 to 4
 * for T5); the only NBT is the optional {@code payload}, a blast id of the casing's tier other than condensed. A stack
 * with no tag, an unknown payload or a payload of another tier reads as bare. The Assembler's fill recipes match the
 * tag exactly, so nothing else is ever written to it. Both sides.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/warheads-and-fill.md (The warhead chain)
public final class InertWarheads {

    /** The bare registry name. */
    public static final String NAME = "inert_warhead";
    /** NBT key: the fitted payload's blast id (string). */
    public static final String K_PAYLOAD = "payload";

    private static ItemInertWarhead item;

    private InertWarheads() {}

    static void set(ItemInertWarhead registered) {
        item = registered;
    }

    /** @return the item; null before registration */
    public static ItemInertWarhead item() {
        return item;
    }

    /**
     * @param casing  the casing
     * @param payload a blast id, or null for a bare inert warhead
     * @return a new stack of one; a payload that does not fit the casing is left off
     */
    public static ItemStack stack(WarheadCasing casing, String payload) {
        ItemStack stack = new ItemStack(item, 1, casing.ordinal());
        String fitted = validPayload(casing, payload);
        if (fitted != null) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString(K_PAYLOAD, fitted);
            stack.setTagCompound(tag);
        }
        return stack;
    }

    /**
     * @param stack a stack
     * @return true when it is an inert warhead
     */
    public static boolean is(ItemStack stack) {
        return stack != null && item != null && stack.getItem() == item;
    }

    /**
     * @param stack an inert warhead stack
     * @return its casing; an out-of-range damage reads as T1
     */
    public static WarheadCasing casing(ItemStack stack) {
        int d = stack == null ? 0 : stack.getItemDamage();
        WarheadCasing[] all = WarheadCasing.values();
        return d >= 0 && d < all.length ? all[d] : WarheadCasing.T1;
    }

    /**
     * @param stack an inert warhead stack
     * @return the fitted payload's blast id, or null when bare
     */
    public static String payload(ItemStack stack) {
        if (stack == null || !stack.hasTagCompound()) {
            return null;
        }
        return validPayload(
            casing(stack),
            stack.getTagCompound()
                .getString(K_PAYLOAD));
    }

    /**
     * @param casing a casing
     * @param id     a blast id
     * @return the id when it is a payload of that casing's tier (not condensed, which has none), else null
     */
    static String validPayload(WarheadCasing casing, String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        WarheadBlast blast = WarheadBlast.byId(id);
        if (blast == null || blast == WarheadBlast.CONDENSED || blast.tier() != casing.tier()) {
            return null;
        }
        return id;
    }
}
