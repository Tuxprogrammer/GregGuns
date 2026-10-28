package com.gregguns.icbm.missile;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * The missile body items, for recipes, NEI and the NASA Workbench page. Set once at registration; null before it runs.
 * Both sides.
 */
// Spec: docs/gregguns-icbm-spec/03-missiles/missile-bodies.md
public final class MissileBodies {

    /** NBT key on a body stack: guidance tiers above the body's tier, 0 to {@link #MAX_GUIDANCE} (D24). */
    public static final String K_GUIDANCE = "guidance";
    /** The most guidance tiers above the body's own that count. */
    public static final int MAX_GUIDANCE = 3;

    private static final Item[] ITEMS = new Item[MissileTier.values().length];

    private MissileBodies() {}

    static void set(MissileTier tier, Item item) {
        ITEMS[tier.ordinal()] = item;
    }

    /** @return the body item of a tier; null before registration */
    public static Item item(MissileTier tier) {
        return ITEMS[tier.ordinal()];
    }

    /**
     * @param tier a tier
     * @return a new body stack with default guidance 0
     */
    public static ItemStack body(MissileTier tier) {
        return body(tier, 0);
    }

    /**
     * @param tier     a tier
     * @param guidance guidance tiers above the body's tier, clamped to 0..3
     * @return a new body stack
     */
    public static ItemStack body(MissileTier tier, int guidance) {
        ItemStack s = new ItemStack(item(tier));
        if (guidance > 0) {
            s.setTagCompound(new net.minecraft.nbt.NBTTagCompound());
            s.getTagCompound()
                .setInteger(K_GUIDANCE, Math.min(MAX_GUIDANCE, guidance));
        }
        return s;
    }

    /**
     * @param stack any stack
     * @return the tier if it is a missile body, else null; a damaged body (a complex's salvage) is no body
     */
    public static MissileTier tierOf(ItemStack stack) {
        if (stack == null || ItemBlockMissileBody.isSalvage(stack)) {
            return null;
        }
        for (MissileTier t : MissileTier.values()) {
            if (stack.getItem() == ITEMS[t.ordinal()] && ITEMS[t.ordinal()] != null) {
                return t;
            }
        }
        return null;
    }

    /**
     * @param stack a body stack
     * @return its guidance tiers above the body's tier, 0 to 3
     */
    public static int guidanceOf(ItemStack stack) {
        if (stack == null || !stack.hasTagCompound()) {
            return 0;
        }
        return Math.max(
            0,
            Math.min(
                MAX_GUIDANCE,
                stack.getTagCompound()
                    .getInteger(K_GUIDANCE)));
    }
}
