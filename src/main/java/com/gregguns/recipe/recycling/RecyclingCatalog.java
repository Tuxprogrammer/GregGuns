package com.gregguns.recipe.recycling;

import java.util.Map;
import java.util.TreeMap;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.gregguns.GregGuns;
import com.gregguns.block.GregGunsCasings;
import com.gregguns.icbm.missile.ItemBlockMissileBody;
import com.gregguns.machine.GregGunsMTEIDs;
import com.gregguns.utility.charge.ChargeType;
import com.gregguns.utility.charge.ItemBlockCharge;
import com.gregguns.worldgen.nest.BlockBugNest;
import com.gregguns.worldgen.nest.ItemBlockBugNest;

import gregtech.api.GregTechAPI;

/**
 * Every stack GregGuns adds, by a stable key: {@code <content name>} for an item or block at damage 0,
 * {@code <content name>@<damage>} for another variant, and {@code mte:<id>} for a GregGuns machine (a GT machine block
 * stack). Server safe: {@code Item.getSubItems} is client-only in 1.7.10, so the variants of an item with subtypes are
 * listed here by hand; {@link #listedSubtypes()} names those items so the coverage scenario can catch a new one.
 * Common side.
 */
// Spec: docs/gregguns-spec/06-items/recycling.md (GGS-06-51)
public final class RecyclingCatalog {

    private static final String PREFIX = GregGuns.MODID + ":";
    private static final String MTE = "mte:";

    private RecyclingCatalog() {}

    /** @return every GregGuns stack by key, in key order */
    public static Map<String, ItemStack> stacks() {
        Map<String, ItemStack> out = new TreeMap<>();
        for (Object key : Item.itemRegistry.getKeys()) {
            String name = (String) key;
            if (!name.startsWith(PREFIX)) {
                continue;
            }
            Item item = (Item) Item.itemRegistry.getObject(name);
            for (int damage : damages(item)) {
                ItemStack stack = new ItemStack(item, 1, damage);
                out.put(keyOf(stack), stack);
            }
        }
        Item machines = GregTechAPI.sBlockMachines == null ? null : Item.getItemFromBlock(GregTechAPI.sBlockMachines);
        if (machines != null) {
            for (int id : GregGunsMTEIDs.all()) {
                if (GregTechAPI.METATILEENTITIES[id] != null) {
                    out.put(MTE + id, new ItemStack(machines, 1, id));
                }
            }
        }
        return out;
    }

    /** @return the catalog key of a GregGuns stack, or null for anything else */
    public static String keyOf(ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return null;
        }
        if (GregTechAPI.sBlockMachines != null
            && stack.getItem() == Item.getItemFromBlock(GregTechAPI.sBlockMachines)) {
            int id = stack.getItemDamage();
            for (int ours : GregGunsMTEIDs.all()) {
                if (ours == id) {
                    return MTE + id;
                }
            }
            return null;
        }
        String name = Item.itemRegistry.getNameForObject(stack.getItem());
        if (name == null || !name.startsWith(PREFIX)) {
            return null;
        }
        String content = name.substring(PREFIX.length());
        // Without subtypes, damage is wear or a crafted gun's "rounds missing", not a variant: the item is the same.
        Item item = stack.getItem();
        int damage = item.getHasSubtypes() || item instanceof ItemBlockMissileBody ? stack.getItemDamage() : 0;
        return damage == 0 ? content : content + "@" + damage;
    }

    /**
     * @return the item classes with subtypes whose variants this class lists; any other item with subtypes is
     *         listed at damage 0 only, which suits an item excluded as a whole
     */
    public static java.util.Set<Class<? extends Item>> listedSubtypes() {
        java.util.Set<Class<? extends Item>> out = new java.util.HashSet<>();
        out.add(gregtech.common.blocks.ItemCasings.class);
        out.add(ItemBlockCharge.class);
        out.add(ItemBlockBugNest.class);
        return out;
    }

    private static int[] damages(Item item) {
        if (net.minecraft.block.Block.getBlockFromItem(item) instanceof GregGunsCasings) {
            int[] metas = new int[GregGunsCasings.META_COUNT];
            for (int i = 0; i < metas.length; i++) {
                metas[i] = i;
            }
            return metas;
        }
        if (item instanceof ItemBlockCharge) {
            ChargeType[] types = ChargeType.values();
            int[] metas = new int[types.length];
            for (int i = 0; i < types.length; i++) {
                metas[i] = types[i].meta();
            }
            return metas;
        }
        if (item instanceof ItemBlockBugNest) {
            return new int[] { BlockBugNest.META_RESIN, BlockBugNest.META_EGGS };
        }
        if (item instanceof ItemBlockMissileBody) {
            // The salvaged, damaged body found in silo complexes (D186) is its own stack.
            return new int[] { 0, ItemBlockMissileBody.SALVAGE_DAMAGE };
        }
        return new int[] { 0 };
    }
}
