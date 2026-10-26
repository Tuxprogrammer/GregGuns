package com.gregguns.worldgen.loot;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.WeightedRandomChestContent;
import net.minecraftforge.common.ChestGenHooks;

import org.jetbrains.annotations.Nullable;

import com.gregguns.GregGuns;
import com.gregguns.api.Tier;
import com.gregguns.api.Tiered;
import com.gregguns.entity.npc.NpcItems;

import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.util.GTOreDictUnificator;

/**
 * Registers the fifteen {@code gregguns:*} structure categories with Forge's {@link ChestGenHooks} and fills
 * inventories from them. Common side, server thread, after every mod's items exist (postInit).
 *
 * <p>
 * The categories are GregGuns' own because TooMuchLoot loads every vanilla category with {@code OVERRIDE}, which
 * wipes anything added from code (GGS-11-19). This class never reads or writes {@link ChestGenHooks#DUNGEON_CHEST}
 * or any other vanilla category. A pack author can still retune ours through TML, so {@link #fill} reads the
 * registered contents, not the {@link LootCategoryDefs} data.
 *
 * <p>
 * Usage: {@code LootCategories.register()} once in postInit; {@code LootCategories.fill(chest, name, richness, rand)}
 * with the structure's seeded {@code Random} (GGS-11-20).
 */
// Spec: docs/gregguns-spec/11-worldgen/structure-loot.md (GGS-11-19..23); docs/gregguns-spec/06-items/pack-loot.md
// (GGS-06-02, GGS-06-03)
public final class LootCategories {

    private static final String GT = "gt:";

    /** Registered content to the line it came from, so {@link #fill} can apply richness weights. */
    private static final Map<WeightedRandomChestContent, LootEntry> SOURCE = new IdentityHashMap<>();
    private static final Map<String, Tier> TOP = new java.util.HashMap<>();
    private static boolean registered;

    private LootCategories() {}

    /**
     * Registers every category: count range, then one content per line whose item resolves. A line whose item is
     * missing (GT absent, or an item not built yet) is skipped, never added as null (GGS-06-03). Idempotent.
     */
    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        for (LootCategory cat : LootCategoryDefs.all()) {
            ChestGenHooks info = ChestGenHooks.getInfo(cat.name());
            info.setMin(cat.minItems());
            info.setMax(cat.maxItems());
            TOP.put(cat.name(), LootDraw.topTier(cat));
            for (LootEntry line : cat.entries()) {
                ItemStack stack = stack(line.ref(), 1);
                if (stack == null) {
                    GregGuns.LOG.debug("Loot category {} skips {}: not registered", cat.name(), line.ref());
                    continue;
                }
                WeightedRandomChestContent content = new WeightedRandomChestContent(
                    stack,
                    line.drop()
                        .min(),
                    line.drop()
                        .max(),
                    line.weight());
                info.addItem(content);
                SOURCE.put(content, line);
            }
        }
    }

    /**
     * Fills an inventory from a category. Same seed, same contents. Richness changes only the pick count and the
     * weight of the category's top-tier lines (GGS-11-23); a content added by someone else (a TML file) keeps its
     * own weight.
     *
     * @param inv      the chest
     * @param category a {@code gregguns:*} name
     * @param richness 0..2
     * @param rand     the structure's seeded random
     */
    public static void fill(IInventory inv, String category, int richness, Random rand) {
        ChestGenHooks info = ChestGenHooks.getInfo(category);
        WeightedRandomChestContent[] base = info.getItems(rand);
        WeightedRandomChestContent[] scaled = new WeightedRandomChestContent[base.length];
        Tier top = TOP.get(category);
        for (int i = 0; i < base.length; i++) {
            WeightedRandomChestContent c = base[i];
            LootEntry line = SOURCE.get(c);
            int weight = line == null || top == null ? c.itemWeight
                : LootDraw.weight(c.itemWeight, line.tier(), top, richness);
            scaled[i] = new WeightedRandomChestContent(
                c.theItemId,
                c.theMinimumChanceToGenerateItem,
                c.theMaximumChanceToGenerateItem,
                weight);
        }
        int n = LootDraw.pickCount(info.getMin(), info.getMax(), richness, rand);
        WeightedRandomChestContent.generateChestContents(rand, scaled, inv, n);
    }

    /**
     * @param ref   a {@link LootEntry} reference
     * @param count the stack size
     * @return the stack, or null when the item is not there
     */
    public static @Nullable ItemStack stack(String ref, int count) {
        if (ref.startsWith(GT)) {
            String spec = ref.substring(GT.length());
            int dot = spec.indexOf('.');
            OrePrefixes prefix = OrePrefixes.getPrefix(spec.substring(0, dot));
            Materials material = Materials.get(spec.substring(dot + 1));
            if (prefix == null || material == null) {
                return null;
            }
            // Source:
            // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTOreDictUnificator.java#L284
            return GTOreDictUnificator.get(prefix, material, count);
        }
        return NpcItems.stack(ref, count);
    }

    /**
     * @param ref a {@link LootEntry} reference
     * @return the item's own declared tier when it is a GregGuns {@link Tiered}, else null
     */
    public static @Nullable Tier itemTier(String ref) {
        if (ref.startsWith(GT)) {
            return null;
        }
        Item item = NpcItems.item(ref);
        return item instanceof Tiered ? ((Tiered) item).getTier() : null;
    }

    /** @return the category names that were registered, for tests */
    public static List<String> names() {
        List<String> out = new ArrayList<>();
        for (LootCategory c : LootCategoryDefs.all()) {
            out.add(c.name());
        }
        return out;
    }
}
