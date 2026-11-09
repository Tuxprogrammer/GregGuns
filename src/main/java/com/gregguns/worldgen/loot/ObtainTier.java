package com.gregguns.worldgen.loot;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import com.gregguns.api.Tiered;
import com.gregguns.core.config.LootConfig;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTETieredMachineBlock;
import gregtech.api.objects.ItemData;
import gregtech.api.util.GTOreDictUnificator;

/**
 * What GT tier a player needs to obtain an item at all: the whole crafting chain, never the voltage of the recipe
 * that outputs it (D363). Every world loot source asks {@link #allowed} before it hands a player a stack (D364).
 *
 * <p>
 * The rules, first match wins:
 * <ol>
 * <li>a GregGuns item (or block) declares its {@link Tiered} tier;</li>
 * <li>a GT material item (prefix and material) takes the higher of the form and the curated material tier; a circuit
 * takes its circuit tier;</li>
 * <li>an item with a {@code circuit<Tier>} ore name takes that circuit tier;</li>
 * <li>a GT item list entry takes its curated tier or the voltage in its name ({@code Electric_Motor_EV});</li>
 * <li>a GT machine block takes its machine tier;</li>
 * <li>a vanilla item is ULV (save the few a player cannot make); another mod's item needs an allow-list entry.</li>
 * </ol>
 * GT's ghost circuits ({@link ObtainTierRules#neverLoot}) are refused before any rule. Anything else is
 * {@link ObtainTierRules#UNKNOWN} and never loot. Server side; answers are cached per item and meta.
 */
// Spec: docs/gregguns-spec/11-worldgen/structure-loot.md (Loot tier cap); D363, D364
public final class ObtainTier {

    private static final Map<Long, Integer> CACHE = new ConcurrentHashMap<>();
    private static volatile Map<Long, String> itemListNames;

    private ObtainTier() {}

    /**
     * @param stack any stack
     * @return its obtain tier as a GT voltage index, or {@link ObtainTierRules#UNKNOWN}
     */
    public static int of(@Nullable ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return ObtainTierRules.UNKNOWN;
        }
        long key = key(stack.getItem(), stack.getItemDamage());
        Integer cached = CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        int tier = compute(stack);
        CACHE.put(key, tier);
        return tier;
    }

    /**
     * @param stack   a stack a world source is about to hand out
     * @param ceiling the source's own ceiling (voltage index)
     * @return whether the stack is known and at most the lower of the ceiling and {@code loot.maxTier}
     */
    public static boolean allowed(@Nullable ItemStack stack, int ceiling) {
        return ObtainTierRules.allowed(of(stack), ceiling, LootConfig.maxTier);
    }

    /**
     * @param stack a stack a world source is about to hand out
     * @return whether the stack is known and at most {@code loot.maxTier}
     */
    public static boolean allowed(@Nullable ItemStack stack) {
        return allowed(stack, Integer.MAX_VALUE - 1);
    }

    /**
     * @param ceiling a source's ceiling (voltage index)
     * @return the ceiling clamped to {@code loot.maxTier}
     */
    public static int cap(int ceiling) {
        return Math.min(ceiling, LootConfig.maxTier);
    }

    private static long key(Item item, int meta) {
        return ((long) Item.getIdFromItem(item) << 32) | (meta & 0xFFFFFFFFL);
    }

    private static int compute(ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof Tiered && ((Tiered) item).getTier() != null) {
            return ((Tiered) item).getTier()
                .voltageIndex();
        }
        Block block = item instanceof ItemBlock ? Block.getBlockFromItem(item) : null;
        if (block instanceof Tiered && ((Tiered) block).getTier() != null) {
            return ((Tiered) block).getTier()
                .voltageIndex();
        }
        String listed = itemListNames().get(key(item, stack.getItemDamage()));
        // Source: GT registers its two ghost circuits under circuitBasic and circuitAdvanced with NO_UNIFICATION, so
        // the ore name alone would call them LV and HV; they are never loot:
        // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/items/MetaGeneratedItem03.java#L1882-L1896
        if (listed != null && ObtainTierRules.neverLoot(listed)) {
            return ObtainTierRules.UNKNOWN;
        }
        int t = gtAssociation(stack);
        if (t != ObtainTierRules.UNKNOWN) {
            return t;
        }
        for (int id : OreDictionary.getOreIDs(stack)) {
            t = ObtainTierRules.circuit(OreDictionary.getOreName(id));
            if (t != ObtainTierRules.UNKNOWN) {
                return t;
            }
        }
        if (listed != null) {
            t = ObtainTierRules.itemList(listed);
            if (t != ObtainTierRules.UNKNOWN) {
                return t;
            }
        }
        if (block != null && block == GregTechAPI.sBlockMachines) {
            return machineTier(stack.getItemDamage());
        }
        String name = Item.itemRegistry.getNameForObject(item);
        if (name == null || name.startsWith("gregtech:") || name.startsWith("gregguns:")) {
            return ObtainTierRules.UNKNOWN;
        }
        if (name.startsWith("minecraft:")) {
            return ObtainTierRules.vanilla(name);
        }
        return ObtainTierRules.allowed(name, stack.getItemDamage());
    }

    private static int gtAssociation(ItemStack stack) {
        // Source: getAssociation answers the prefix and material GT unified the stack under (null for none):
        // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTOreDictUnificator.java#L504
        ItemData data = GTOreDictUnificator.getAssociation(stack);
        if (data == null || data.mPrefix == null || data.mMaterial == null || data.mMaterial.mMaterial == null) {
            return ObtainTierRules.UNKNOWN;
        }
        return ObtainTierRules.materialItem(data.mPrefix.name(), data.mMaterial.mMaterial.mName);
    }

    private static int machineTier(int meta) {
        // Source: a GT machine item's damage is its meta tile entity id; a tiered machine keeps its tier in mTier:
        // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/GregTechAPI.java#L87
        // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/metatileentity/implementations/MTETieredMachineBlock.java#L27
        if (meta < 0 || meta >= GregTechAPI.METATILEENTITIES.length) {
            return ObtainTierRules.UNKNOWN;
        }
        IMetaTileEntity mte = GregTechAPI.METATILEENTITIES[meta];
        return mte instanceof MTETieredMachineBlock ? ((MTETieredMachineBlock) mte).mTier : ObtainTierRules.UNKNOWN;
    }

    private static Map<Long, String> itemListNames() {
        Map<Long, String> names = itemListNames;
        if (names != null) {
            return names;
        }
        names = new HashMap<>();
        // Source: hasBeenSet and getInternalStack_unsafe read an ItemList entry's stack without a copy or a warning:
        // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/enums/ItemList.java#L3459
        for (ItemList entry : ItemList.values()) {
            if (!entry.hasBeenSet()) {
                continue;
            }
            ItemStack s = entry.getInternalStack_unsafe();
            if (s != null && s.getItem() != null) {
                Long k = key(s.getItem(), s.getItemDamage());
                if (!names.containsKey(k)) {
                    names.put(k, entry.name());
                }
            }
        }
        itemListNames = names;
        return names;
    }

    /** Forgets every answer, for a scenario that needs a fresh read. */
    public static void clearCache() {
        CACHE.clear();
    }
}
