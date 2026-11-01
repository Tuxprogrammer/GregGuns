package com.gregguns.worldgen.loot;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.WeightedRandomChestContent;
import net.minecraftforge.common.ChestGenHooks;

import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.gun.def.GunDefinition;
import com.gregguns.gun.def.GunDefinitions;
import com.gregguns.item.part.ItemTank;
import com.gregguns.worldgen.ChestTags;
import com.gregguns.worldgen.StructureId;
import com.gregguns.worldgen.StructurePopulator;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.registry.GameRegistry;

/**
 * Scenarios of the structure loot categories (M8 G7). Dev environment only; needs {@code worldgen} in
 * {@code GG_MODULES}. Server side.
 */
// Spec: docs/gregguns-spec/11-worldgen/structure-loot.md (GGS-11-19..23, GGS-11-26)
public final class LootScenarios {

    private LootScenarios() {}

    /** @param registry where to register */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new CategoriesScenario());
        registry.register(new TierCeilingScenario());
        registry.register(new MixScenario());
        registry.register(new PackItemsScenario());
        registry.register(new RebuiltContentsScenario());
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
            return "After postInit the 22 gregguns:* categories exist, hold items and fill a chest; none of their"
                + " content is in DUNGEON_CHEST (GGS-06-02, GGS-11-19).";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            out.checkEquals(
                "category count",
                22,
                LootCategoryDefs.all()
                    .size());
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
                                .ordinal()
                                > cat.ceiling()
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

    /** @return the modid of a line reference: {@code gt} for a GT part, the part before the colon otherwise */
    private static String modOf(String ref) {
        return ref.substring(0, ref.indexOf(':'));
    }

    private static boolean modLoaded(String modid) {
        return "minecraft".equals(modid) || "gt".equals(modid) || Loader.isModLoaded(modid);
    }

    private static final class MixScenario implements Scenario {

        @Override
        public String name() {
            return "worldgen.loot_mix";
        }

        @Override
        public String description() {
            return "Fills 200 chests of every category a structure can use (each structure's own tables at its"
                + " ceiling and richness, plus the Ross128 tables): every kind holds pack items, supply and weapon"
                + " chests also hold GregGuns ammo, guns stay rare, and every gun comes with its ammo (GGS-11-26).";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            // Category -> richness: every structure's tables at its configured ceiling, then tables 4 to 7.
            Map<String, Integer> plan = new LinkedHashMap<>();
            for (StructureId id : StructureId.values()) {
                int ceiling = Math.max(1, Math.min(3, StructurePopulator.CONFIG_RULES.lootCeiling(id)));
                int richness = StructurePopulator.CONFIG_RULES.richness(id);
                boolean camp = id == StructureId.MILITARY_BASE;
                for (String kind : ChestTags.KINDS) {
                    plan.putIfAbsent(ChestTags.category(kind, camp, ceiling), richness);
                }
            }
            for (int t = 4; t <= Arsenal.TABLES; t++) {
                plan.putIfAbsent(LootCategoryDefs.weaponChest(t), 1);
                plan.putIfAbsent(LootCategoryDefs.supplyChest(t), 1);
                plan.putIfAbsent(LootCategoryDefs.reinforcedChest(t), 1);
            }
            for (Map.Entry<String, Integer> e : plan.entrySet()) {
                String cat = e.getKey();
                Random rand = new Random(cat.hashCode() * 31L + e.getValue());
                int chests = 200;
                int withPack = 0;
                int withGregGuns = 0;
                int withGun = 0;
                int withAmmo = 0;
                int gunsWithoutAmmo = 0;
                int emptyTanks = 0;
                Set<String> packMods = new LinkedHashSet<>();
                for (int c = 0; c < chests; c++) {
                    ItemStack[] stacks = drawChest(cat, e.getValue(), rand);
                    boolean pack = false;
                    boolean gg = false;
                    boolean gun = false;
                    boolean ammo = false;
                    Set<Item> items = new LinkedHashSet<>();
                    for (ItemStack s : stacks) {
                        items.add(s.getItem());
                    }
                    for (ItemStack s : stacks) {
                        String mod = GameRegistry.findUniqueIdentifierFor(s.getItem()).modId;
                        if ("gregguns".equals(mod)) {
                            gg = true;
                        } else {
                            pack = true;
                            packMods.add(mod);
                        }
                        String name = GameRegistry.findUniqueIdentifierFor(s.getItem()).name;
                        if (Arsenal.gunName("gregguns:" + name) != null && "gregguns".equals(mod)) {
                            gun = true;
                            GunDefinition def = GunDefinitions.byName(name);
                            String want = def == null ? null : Arsenal.ammoItem(def);
                            if (want != null) {
                                Item wantItem = GameRegistry.findItem("gregguns", want);
                                if (wantItem != null && !items.contains(wantItem)) {
                                    gunsWithoutAmmo++;
                                }
                            }
                        }
                        if ("gregguns".equals(mod)
                            && (name.startsWith("ammo_") || name.startsWith("mag_") || name.startsWith("tank_"))) {
                            ammo = true;
                        }
                        if (s.getItem() instanceof ItemTank && ((ItemTank) s.getItem()).getFluid(s) == null) {
                            emptyTanks++;
                        }
                    }
                    withPack += pack ? 1 : 0;
                    withGregGuns += gg ? 1 : 0;
                    withGun += gun ? 1 : 0;
                    withAmmo += ammo ? 1 : 0;
                }
                String where = cat + " (richness " + e.getValue() + ")";
                out.note(
                    where + ": pack items in "
                        + withPack
                        + ", GregGuns in "
                        + withGregGuns
                        + ", a gun in "
                        + withGun
                        + ", ammo in "
                        + withAmmo
                        + " of "
                        + chests
                        + "; pack mods "
                        + packMods);
                // Weapon chests and barracks lean on GregGuns; supply and reinforced chests are mostly pack loot.
                // The floors hold without any optional pack mod (a dev server has none, so GregGuns lines weigh
                // more there); the exact odds per table are ArsenalTest's.
                int packFloor = cat.contains("weaponchest") || cat.equals(LootCategoryDefs.BARRACKS) ? chests * 3 / 4
                    : chests * 9 / 10;
                out.check(withPack >= packFloor, where + " pack items in only " + withPack + " chests");
                out.check(packMods.contains("minecraft"), where + " holds no vanilla item");
                out.checkEquals(where + " guns without their ammo", 0, gunsWithoutAmmo);
                out.checkEquals(where + " empty fluid tanks", 0, emptyTanks);
                boolean arms = cat.contains("weaponchest") || cat.contains("supplychest");
                if (arms) {
                    out.check(withGregGuns >= chests / 2, where + " GregGuns items in only " + withGregGuns);
                    out.check(withAmmo > withGun * 2, where + " ammo in " + withAmmo + ", guns in " + withGun);
                    out.check(withGun > 0, where + " never drew a gun");
                    out.check(withGun < chests / 2, where + " a gun in " + withGun + " chests");
                }
                if (cat.contains("supplychest")) {
                    out.check(packMods.size() >= 2, where + " only " + packMods + " supply pack items");
                }
            }
            return out.build();
        }
    }

    /** TooMuchLoot's {@code /loot reload} rebuilds every content object; guns must keep their ammo after it. */
    private static final class RebuiltContentsScenario implements Scenario {

        @Override
        public String name() {
            return "worldgen.loot_rebuilt";
        }

        @Override
        public String description() {
            return "Replaces every content of a weapon chest category with a fresh copy, as TooMuchLoot's /loot reload"
                + " does, then fills 200 chests: guns still come with their ammo (GGS-11-26).";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            String cat = LootCategoryDefs.weaponChest(4);
            ChestGenHooks info = ChestGenHooks.getInfo(cat);
            WeightedRandomChestContent[] originals = info.getItems(new Random(1));
            List<WeightedRandomChestContent> copies = new ArrayList<>();
            try {
                for (WeightedRandomChestContent c : originals) {
                    info.removeItem(c.theItemId);
                }
                for (WeightedRandomChestContent c : originals) {
                    WeightedRandomChestContent copy = new WeightedRandomChestContent(
                        c.theItemId.copy(),
                        c.theMinimumChanceToGenerateItem,
                        c.theMaximumChanceToGenerateItem,
                        c.itemWeight);
                    copies.add(copy);
                    info.addItem(copy);
                }
                Random rand = new Random(7);
                int guns = 0;
                int gunsWithoutAmmo = 0;
                for (int c = 0; c < 200; c++) {
                    ItemStack[] stacks = drawChest(cat, 1, rand);
                    Set<Item> items = new LinkedHashSet<>();
                    for (ItemStack s : stacks) {
                        items.add(s.getItem());
                    }
                    for (ItemStack s : stacks) {
                        String name = GameRegistry.findUniqueIdentifierFor(s.getItem()).name;
                        if (!"gregguns".equals(GameRegistry.findUniqueIdentifierFor(s.getItem()).modId)
                            || Arsenal.gunName("gregguns:" + name) == null) {
                            continue;
                        }
                        guns++;
                        GunDefinition def = GunDefinitions.byName(name);
                        String want = def == null ? null : Arsenal.ammoItem(def);
                        Item wantItem = want == null ? null : GameRegistry.findItem("gregguns", want);
                        if (wantItem != null && !items.contains(wantItem)) {
                            gunsWithoutAmmo++;
                        }
                    }
                }
                out.check(guns > 0, "the rebuilt " + cat + " never drew a gun");
                out.checkEquals("guns without their ammo after a rebuild", 0, gunsWithoutAmmo);
            } finally {
                for (WeightedRandomChestContent c : copies) {
                    info.removeItem(c.theItemId);
                }
                for (WeightedRandomChestContent c : originals) {
                    info.addItem(c);
                }
            }
            return out.build();
        }
    }

    private static final class PackItemsScenario implements Scenario {

        @Override
        public String name() {
            return "worldgen.loot_pack_items";
        }

        @Override
        public String description() {
            return "Every structure loot line whose mod is installed resolves to an item (a wrong id fails); lines"
                + " of mods that are not installed are skipped, and the scenario lists them.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            Set<String> refs = new LinkedHashSet<>();
            for (LootCategory cat : LootCategoryDefs.all()) {
                for (LootEntry e : cat.entries()) {
                    refs.add(e.ref());
                    if (e.companion() != null) {
                        refs.add(
                            e.companion()
                                .ref());
                    }
                }
            }
            Set<String> absentMods = new LinkedHashSet<>();
            int resolved = 0;
            for (String ref : refs) {
                String mod = modOf(ref);
                if (!modLoaded(mod)) {
                    absentMods.add(mod);
                    continue;
                }
                boolean ok = LootCategories.stack(ref, 1) != null;
                out.check(ok, ref + " does not resolve although " + mod + " is installed");
                resolved += ok ? 1 : 0;
            }
            out.note(resolved + " of " + refs.size() + " references resolve; mods not installed: " + absentMods);
            out.check(resolved > 0, "nothing resolved");
            return out.build();
        }
    }
}
