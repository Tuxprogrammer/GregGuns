package com.gregguns.icbm.warhead;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Holds the inert warhead items, one per casing ({@code inert_warhead_t1} to {@code inert_warhead_t5}), and reads and
 * writes their stacks (D282). The only NBT is the optional {@code payload}, a blast id of the casing's tier other than
 * condensed. A stack with no tag, an unknown payload or a payload of another tier reads as bare. The Assembler's fill
 * recipes match the tag exactly, so nothing else is ever written to it. Both sides.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/warheads-and-fill.md (The warhead chain)
public final class InertWarheads {

    /** The bare registry name prefix; the casing's number follows ({@code inert_warhead_t1}). */
    public static final String NAME_PREFIX = "inert_warhead_t";
    /** NBT key: the fitted payload's blast id (string). */
    public static final String K_PAYLOAD = "payload";

    private static final ItemInertWarhead[] ITEMS = new ItemInertWarhead[WarheadCasing.values().length];

    private InertWarheads() {}

    static void set(WarheadCasing casing, ItemInertWarhead registered) {
        ITEMS[casing.ordinal()] = registered;
    }

    /**
     * @param casing a casing
     * @return its bare registry name, such as {@code inert_warhead_t1}
     */
    public static String name(WarheadCasing casing) {
        return NAME_PREFIX + casing.number();
    }

    /**
     * @param casing a casing
     * @return its inert warhead item; null before registration
     */
    public static ItemInertWarhead item(WarheadCasing casing) {
        return ITEMS[casing.ordinal()];
    }

    /**
     * @param casing  the casing
     * @param payload a blast id, or null for a bare inert warhead
     * @return a new stack of one; a payload that does not fit the casing is left off
     */
    public static ItemStack stack(WarheadCasing casing, String payload) {
        ItemStack stack = new ItemStack(item(casing), 1, 0);
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
        return stack != null && stack.getItem() instanceof ItemInertWarhead;
    }

    /**
     * @param stack an inert warhead stack
     * @return its casing; anything else reads as T1
     */
    public static WarheadCasing casing(ItemStack stack) {
        return is(stack) ? ((ItemInertWarhead) stack.getItem()).casing() : WarheadCasing.T1;
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
