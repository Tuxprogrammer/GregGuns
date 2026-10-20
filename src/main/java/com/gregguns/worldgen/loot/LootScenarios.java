package com.gregguns.worldgen.loot;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.WeightedRandomChestContent;
import net.minecraftforge.common.ChestGenHooks;

import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;

import cpw.mods.fml.common.registry.GameRegistry;

/**
 * Scenarios of the structure loot categories (M8 G7). Dev environment only; needs {@code worldgen} in
 * {@code GG_MODULES}. Server side.
 */
// Spec: docs/gregguns-spec/11-worldgen/structure-loot.md (GGS-11-19..23)
public final class LootScenarios {

    private LootScenarios() {}

    /** @param registry where to register */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new CategoriesScenario());
        registry.register(new TierCeilingScenario());
    }

    private static ItemStack[] drawChest(String category, int richness, Random rand) {
        InventoryBasic chest = new InventoryBasic("loot", false, 54);
        LootCategories.fill(chest, category, richness, rand);
        List<ItemStack> out = new ArrayList<>();
        for (int i = 0; i < chest.getSizeInventory(); i++) {
            ItemStack s = chest.getStackInSlot(i);
            if (s != null) {
                out.add(s);
            }
        }
        return out.toArray(new ItemStack[0]);
    }

    private static final class CategoriesScenario implements Scenario {

        @Override
        public String name() {
            return "worldgen.loot_categories";
        }

        @Override
        public String description() {
            return "After postInit the seven gregguns:* categories exist, hold items and fill a chest; none of their"
                + " content is in DUNGEON_CHEST (GGS-06-02, GGS-11-19).";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            out.checkEquals("category count", 7, LootCategoryDefs.all().size());
            for (LootCategory cat : LootCategoryDefs.all()) {
                ChestGenHooks info = ChestGenHooks.getInfo(cat.name());
                WeightedRandomChestContent[] items = info.getItems(ctx.random());
                out.check(items.length > 0, cat.name() + " has no registered content");
                out.checkEquals(cat.name() + " min", cat.minItems(), info.getMin());
                out.checkEquals(cat.name() + " max", cat.maxItems(), info.getMax());
                int filled = 0;
                for (int i = 0; i < 20; i++) {
                    if (drawChest(cat.name(), 1, ctx.random()).length > 0) {
                        filled++;
                    }
                }
                out.checkEquals(cat.name() + " chests that drew items out of 20", 20, filled);
            }
            for (WeightedRandomChestContent c : ChestGenHooks.getInfo(ChestGenHooks.DUNGEON_CHEST)
                .getItems(ctx.random())) {
                Item item = c.theItemId.getItem();
                String id = item == null ? "" : GameRegistry.findUniqueIdentifierFor(item).modId;
                out.check(!"gregguns".equals(id), "DUNGEON_CHEST holds a GregGuns item");
            }
            return out.build();
        }
    }

    private static final class TierCeilingScenario implements Scenario {

        @Override
        public String name() {
            return "worldgen.loot_tier_ceiling";
        }

        @Override
        public String description() {
            return "1000 chests per category at richness 0, 1 and 2: every stack is a known line, at or below the"
                + " category ceiling, and no GregGuns item is above its declared tier (GGS-11-21..23).";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            for (LootCategory cat : LootCategoryDefs.all()) {
                for (String problem : LootTierCheck.violations(cat, LootCategories::itemTier)) {
                    out.check(false, problem);
                }
                for (int richness = 0; richness <= LootDraw.MAX_RICHNESS; richness++) {
                    Random rand = new Random(1234L + richness);
                    int stacks = 0;
                    int unknown = 0;
                    int above = 0;
                    for (int c = 0; c < 1000; c++) {
                        for (ItemStack s : drawChest(cat.name(), richness, rand)) {
                            stacks++;
                            LootEntry line = lineOf(cat, s);
                            if (line == null) {
                                unknown++;
                            } else if (line.tier()
                                .ordinal() > cat.ceiling()
                                    .ordinal()) {
                                above++;
                            }
                        }
                    }
                    String where = cat.name() + " richness " + richness;
                    out.check(stacks > 0, where + " drew nothing");
                    out.checkEquals(where + " stacks not from a category line", 0, unknown);
                    out.checkEquals(where + " stacks above " + cat.ceiling(), 0, above);
                }
            }
            return out.build();
        }

        private static LootEntry lineOf(LootCategory cat, ItemStack stack) {
            for (LootEntry e : cat.entries()) {
                ItemStack ref = LootCategories.stack(e.ref(), 1);
                if (ref != null && ref.getItem() == stack.getItem() && ref.getItemDamage() == stack.getItemDamage()) {
                    return e;
                }
            }
            return null;
        }
    }
}
