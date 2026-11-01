package com.gregguns.worldgen.loot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.gregguns.api.Tier;
import com.gregguns.entity.npc.loot.LootDrop;

/**
 * The pack-side pools a structure chest mixes from, modeled on the top floors (levels 0 and 1) of Roguelike
 * Dungeons as GTNH 2.9.0-RC-2 configures them: food, everyday junk, ores and ingots, tools, potions, melee weapons and
 * bows, armor and trinkets, enchanting, and GT materials. Pure data; {@link LootMix} weighs the pools into a category,
 * and drops every line above the category's ceiling, which is how a pool scales with the tier.
 *
 * <p>
 * Weights inside a pool are relative to that pool only. Item ids are the pack's own: most are the ids the pack's
 * Roguelike loot files use (config/roguelike_dungeons/settings/loot_*.json). A line whose mod is not installed does
 * not resolve and is skipped at registration ({@link LootCategories#register}), so every pool works in a pack
 * without HarvestCraft, Forestry, TConstruct or the others.
 *
 * <p>
 * GT tools and named enchanted weapons, which Roguelike hands out with NBT, are left out: a line here has no NBT. A
 * plain {@code minecraft:enchanted_book} line is a random enchanted book per chest (Forge's
 * {@code Item.getChestGenBase}).
 */
// Spec: docs/gregguns-spec/06-items/pack-loot.md (Structure chest mix); research: Roguelike-Dungeons 1.6.6-GTNH,
// Source:
// https://github.com/GTNewHorizons/Roguelike-Dungeons/blob/1.6.6-GTNH/src/main/java/greymerk/roguelike/treasure/loot/LootRuleManager.java
public final class LootPools {

    /** Pool names, also the {@link LootEntry#pool()} of every line a pool adds. */
    public static final String FOOD = "food";
    /** Everyday materials (Roguelike's junk rule, the useful half). */
    public static final String JUNK = "junk";
    /** Ores, ingots and gems, with the GT material ladder per tier. */
    public static final String ORE = "ore";
    /** Tools and supplies. */
    public static final String TOOLS = "tools";
    /** Potions and medical items. */
    public static final String POTIONS = "potions";
    /** Melee weapons, bows and arrows. */
    public static final String MELEE = "melee";
    /** Armor and trinkets. */
    public static final String ARMOR = "armor";
    /** Books, enchanted books, lapis and redstone. */
    public static final String ENCHANTING = "enchanting";
    /** GT materials of the reinforced chests. */
    public static final String MATERIALS = "materials";
    /** GregGuns combat armor (barracks). */
    public static final String COMBAT_ARMOR = "combat_armor";

    private static final String MC = "minecraft:";
    private static final String HC = "harvestcraft:";
    private static final String G = "gregguns:";

    /** The GT material each tier adds to the ore pool and the reinforced chests, index = GT voltage index. */
    private static final String[] LADDER = { null, "Steel", "Aluminium", "StainlessSteel", "Titanium", "TungstenSteel",
        "HSSG", "Naquadah" };

    private LootPools() {}

    /** A named list of lines with in-pool weights. Immutable. */
    public static final class Pool {

        private final String name;
        private final List<LootEntry> lines;

        Pool(String name, List<LootEntry> lines) {
            this.name = name;
            this.lines = Collections.unmodifiableList(new ArrayList<>(lines));
        }

        /**
         * A pool built outside this package (the silo complex's room pools, D316).
         *
         * @param name  the pool name, also each line's {@link LootEntry#pool()} once a {@link LootMix} takes it
         * @param lines the lines, weights relative to this pool
         * @return the pool
         */
        public static Pool of(String name, List<LootEntry> lines) {
            return new Pool(name, lines);
        }

        /** @return the pool name */
        public String name() {
            return name;
        }

        /** @return the lines, weights relative to this pool */
        public List<LootEntry> lines() {
            return lines;
        }
    }

    /** Builds a pool line by line. */
    static final class PoolBuilder {

        private final String name;
        private final List<LootEntry> lines = new ArrayList<>();

        PoolBuilder(String name) {
            this.name = name;
        }

        PoolBuilder add(String ref, int min, int max, int weight, Tier tier) {
            return add(ref, min, max, weight, tier, null);
        }

        PoolBuilder add(String ref, int min, int max, int weight, Tier tier, @Nullable LootDrop companion) {
            lines.add(new LootEntry(LootDrop.range(ref, min, max), weight, tier, name, companion));
            return this;
        }

        PoolBuilder one(String ref, int weight, Tier tier) {
            return add(ref, 1, 1, weight, tier);
        }

        Pool build() {
            return new Pool(name, lines);
        }
    }

    /**
     * Food: vanilla staples and HarvestCraft meals, after Roguelike's starter, supplies and theme food rules (cooked
     * meats, baked dishes, sandwiches, yogurts, sweets). Not tiered.
     *
     * @return the pool
     */
    public static Pool food() {
        PoolBuilder b = new PoolBuilder(FOOD);
        b.add(MC + "bread", 2, 6, 5, Tier.STEAM);
        b.add(MC + "cooked_beef", 2, 5, 4, Tier.STEAM);
        b.add(MC + "cooked_porkchop", 2, 5, 3, Tier.STEAM);
        b.add(MC + "cooked_chicken", 2, 5, 2, Tier.STEAM);
        b.add(MC + "cooked_fished", 1, 4, 2, Tier.STEAM);
        b.add(MC + "baked_potato", 2, 6, 2, Tier.STEAM);
        b.add(MC + "apple", 1, 4, 2, Tier.STEAM);
        b.add(MC + "golden_apple", 1, 2, 1, Tier.STEAM);
        // HarvestCraft (Pam's): ids checked against the pack's lang (tools/questbook/pack_items.txt).
        b.add(HC + "beefjerkyItem", 2, 6, 3, Tier.STEAM);
        b.add(HC + "trailmixItem", 1, 4, 2, Tier.STEAM);
        b.add(HC + "muttoncookedItem", 1, 3, 2, Tier.STEAM);
        b.add(HC + "venisoncookedItem", 1, 3, 2, Tier.STEAM);
        b.add(HC + "rabbitcookedItem", 1, 3, 1, Tier.STEAM);
        b.add(HC + "bakedhamItem", 1, 3, 2, Tier.STEAM);
        b.add(HC + "bakedbeansItem", 1, 3, 2, Tier.STEAM);
        b.add(HC + "loadedbakedpotatoItem", 1, 3, 2, Tier.STEAM);
        b.add(HC + "hamburgerItem", 1, 3, 2, Tier.STEAM);
        b.add(HC + "chickensandwichItem", 1, 3, 2, Tier.STEAM);
        b.add(HC + "friedchickenItem", 1, 3, 1, Tier.STEAM);
        b.add(HC + "honeysandwichItem", 1, 3, 1, Tier.STEAM);
        b.add(HC + "strawberryjellysandwichItem", 1, 3, 1, Tier.STEAM);
        b.add(HC + "hamsweetpicklesandwichItem", 1, 3, 1, Tier.STEAM);
        b.add(HC + "plainyogurtItem", 1, 3, 1, Tier.STEAM);
        b.add(HC + "caramelappleItem", 1, 3, 1, Tier.STEAM);
        b.add(HC + "donutItem", 1, 3, 1, Tier.STEAM);
        b.add(HC + "toastItem", 1, 4, 1, Tier.STEAM);
        b.add(HC + "coffeeItem", 1, 3, 1, Tier.STEAM);
        b.add(HC + "energydrinkItem", 1, 2, 1, Tier.STEAM);
        return b.build();
    }

    /**
     * Everyday materials, after Roguelike's junk rule (every chest gets six): the useful half, without sand, gravel,
     * rotten flesh and reeds.
     *
     * @return the pool
     */
    public static Pool junk() {
        PoolBuilder b = new PoolBuilder(JUNK);
        b.add(MC + "torch", 4, 16, 4, Tier.STEAM);
        b.add(MC + "string", 2, 12, 3, Tier.STEAM);
        b.add(MC + "bone", 2, 10, 3, Tier.STEAM);
        b.add(MC + "leather", 2, 4, 3, Tier.STEAM);
        b.add(MC + "gunpowder", 1, 7, 3, Tier.STEAM);
        b.add(MC + "arrow", 3, 9, 3, Tier.STEAM);
        b.add(MC + "coal", 2, 8, 3, Tier.STEAM);
        b.add(MC + "flint", 3, 7, 2, Tier.STEAM);
        b.add(MC + "paper", 3, 12, 2, Tier.STEAM);
        b.add(MC + "redstone", 3, 16, 2, Tier.STEAM);
        b.add(MC + "clay_ball", 3, 12, 1, Tier.STEAM);
        b.add("Backpack:tannedLeather", 1, 2, 1, Tier.STEAM);
        b.add("Thaumcraft:ItemLootBag", 1, 1, 1, Tier.STEAM);
        b.add(MC + "name_tag", 1, 1, 1, Tier.STEAM);
        return b.build();
    }

    /**
     * Ores, ingots and gems: Roguelike's ORE rules for levels 0 and 1 (GT ingots by the pack's own metaitem ids,
     * fetched here through the unificator), then one GT material per tier from MV up (Aluminium, Stainless Steel,
     * Titanium, Tungstensteel, HSS-G, Naquadah), so a richer location hands out
     * its own tier's metal.
     *
     * @return the pool; lines run up to ZPM, {@link LootMix} keeps those at or below the category ceiling
     */
    public static Pool ore() {
        PoolBuilder b = new PoolBuilder(ORE);
        // Level 0 and 1: Copper, iron, coal, Cast Iron, Zinc, gold, Tin, Silver, Bronze, Steel, diamond, emerald.
        b.add("gt:ingot.Copper", 5, 15, 30, Tier.STEAM);
        b.add("gt:ingot.Iron", 5, 15, 30, Tier.STEAM);
        b.add("gt:ingot.Tin", 5, 15, 30, Tier.STEAM);
        b.add("gt:ingot.CastIron", 3, 10, 20, Tier.STEAM);
        b.add(MC + "coal", 5, 15, 20, Tier.STEAM);
        b.add("gt:ingot.Zinc", 4, 10, 15, Tier.STEAM);
        b.add("gt:ingot.Gold", 5, 8, 15, Tier.STEAM);
        b.add("gt:ingot.Bronze", 4, 10, 15, Tier.STEAM);
        b.add("gt:ingot.Silver", 4, 10, 15, Tier.LV);
        b.add("gt:ingot.Steel", 2, 4, 15, Tier.LV);
        b.add("gt:gem.Diamond", 1, 3, 10, Tier.LV);
        b.add("gt:gem.Emerald", 1, 3, 10, Tier.LV);
        // Level 1's two steels are EBF alloys, so they wait for MV.
        b.add("gt:ingot.DamascusSteel", 3, 5, 10, Tier.MV);
        b.add("gt:ingot.VanadiumSteel", 3, 5, 10, Tier.MV);
        for (Tier t : Tier.values()) {
            if (t.ordinal() >= Tier.MV.ordinal()) {
                b.add("gt:ingot." + ladder(t), 2, 8, 10, t);
            }
        }
        return b.build();
    }

    /**
     * Tools and supplies: Roguelike's TOOLS, SUPPLIES and STARTER rules without the NBT tools.
     *
     * @return the pool
     */
    public static Pool tools() {
        PoolBuilder b = new PoolBuilder(TOOLS);
        b.add("IC2:itemTreetap", 1, 1, 6, Tier.STEAM);
        b.add(MC + "lead", 1, 2, 4, Tier.STEAM);
        b.add(MC + "bucket", 1, 1, 5, Tier.STEAM);
        b.add(MC + "flint_and_steel", 1, 1, 4, Tier.STEAM);
        b.add(MC + "shears", 1, 1, 3, Tier.STEAM);
        b.add(MC + "fishing_rod", 1, 1, 2, Tier.STEAM);
        b.add(MC + "clock", 1, 1, 2, Tier.STEAM);
        b.add(MC + "iron_pickaxe", 1, 1, 2, Tier.STEAM);
        b.add(MC + "iron_shovel", 1, 1, 1, Tier.STEAM);
        b.add("Forestry:scoop", 1, 1, 4, Tier.STEAM);
        b.add(HC + "potItem", 1, 1, 3, Tier.STEAM);
        b.add(HC + "skilletItem", 1, 1, 3, Tier.STEAM);
        b.add(HC + "saucepanItem", 1, 1, 3, Tier.STEAM);
        b.add(HC + "mixingbowlItem", 1, 1, 3, Tier.STEAM);
        b.add("SpiceOfLife:lunchbag", 1, 1, 2, Tier.STEAM);
        b.add("Backpack:backpack", 1, 1, 1, Tier.STEAM);
        for (String bag : new String[] { "builderBag", "diggerBag", "foresterBag", "hunterBag", "minerBag" }) {
            b.add("Forestry:" + bag, 1, 1, 1, Tier.STEAM);
        }
        // TConstruct tool parts, iron (meta 2), as Roguelike's level 1.
        for (String part : new String[] { "toolRod", "binding", "hatchetHead", "shovelHead" }) {
            b.add("TConstruct:" + part + "#2", 1, 1, 1, Tier.STEAM);
        }
        return b.build();
    }

    /**
     * Potions (Roguelike's potion rule: regeneration II, healing I and II, swiftness I and II, invisibility II,
     * strength, splash regeneration II; plus fire resistance) and GregGuns bandages and medkits.
     *
     * @return the pool
     */
    public static Pool potions() {
        PoolBuilder b = new PoolBuilder(POTIONS);
        b.add(MC + "potion#8197", 1, 2, 3, Tier.STEAM);
        b.add(MC + "potion#8225", 1, 2, 2, Tier.STEAM);
        b.add(MC + "potion#8194", 1, 2, 2, Tier.STEAM);
        b.add(MC + "potion#8195", 1, 2, 2, Tier.STEAM);
        b.add(MC + "potion#8229", 1, 1, 1, Tier.STEAM);
        b.add(MC + "potion#8226", 1, 1, 1, Tier.STEAM);
        b.add(MC + "potion#8238", 1, 1, 1, Tier.STEAM);
        b.add(MC + "potion#8265", 1, 1, 1, Tier.STEAM);
        b.add(MC + "potion#16417", 1, 2, 1, Tier.STEAM);
        b.add(G + "bandages", 1, 4, 3, Tier.LV);
        b.add(G + "medkit", 1, 2, 1, Tier.MV);
        return b.build();
    }

    /**
     * Melee weapons, bows and arrows: Roguelike's WEAPONS rule without the NBT names, its TConstruct weapon parts,
     * and GregGuns' combat knife and crowbar.
     *
     * @return the pool
     */
    public static Pool melee() {
        PoolBuilder b = new PoolBuilder(MELEE);
        b.add(MC + "bow", 1, 1, 6, Tier.STEAM);
        b.add(MC + "arrow", 4, 16, 6, Tier.STEAM);
        b.add(MC + "iron_sword", 1, 1, 4, Tier.STEAM);
        b.add(MC + "stone_sword", 1, 1, 2, Tier.STEAM);
        b.add(MC + "iron_axe", 1, 1, 2, Tier.STEAM);
        b.add(MC + "golden_sword", 1, 1, 1, Tier.STEAM);
        b.add("Natura:natura.bow.darkwood", 1, 1, 2, Tier.STEAM);
        b.add("Thaumcraft:ItemBowBone", 1, 1, 1, Tier.STEAM);
        for (String part : new String[] { "swordBlade", "knifeBlade", "arrowhead" }) {
            b.add("TConstruct:" + part + "#2", 1, 1, 1, Tier.STEAM);
        }
        b.add("TConstruct:fletching", 2, 6, 1, Tier.STEAM);
        b.add(G + "combat_knife", 1, 1, 2, Tier.LV);
        b.add(G + "crowbar", 1, 1, 2, Tier.LV);
        b.add(MC + "diamond_sword", 1, 1, 2, Tier.MV);
        return b.build();
    }

    /**
     * Armor and trinkets: Roguelike's ARMOUR rules (leather, chainmail, gold, HarvestCraft hardened leather; bauble
     * blanks, charms of keeping, flippers, glove) with iron armor added, and diamond armor from HV.
     *
     * @return the pool
     */
    public static Pool armor() {
        PoolBuilder b = new PoolBuilder(ARMOR);
        String[] slots = { "helmet", "chestplate", "leggings", "boots" };
        for (String s : slots) {
            b.add(MC + "leather_" + s, 1, 1, 2, Tier.STEAM);
            b.add(MC + "chainmail_" + s, 1, 1, 2, Tier.STEAM);
            b.add(MC + "golden_" + s, 1, 1, 1, Tier.STEAM);
            b.add(MC + "iron_" + s, 1, 1, 1, Tier.STEAM);
            b.add(MC + "diamond_" + s, 1, 1, 1, Tier.HV);
        }
        for (String s : new String[] { "helm", "chest", "leggings", "boots" }) {
            b.add(HC + "hardenedleather" + s + "Item", 1, 1, 2, Tier.STEAM);
        }
        b.add("Thaumcraft:ItemBaubleBlanks", 1, 1, 2, Tier.STEAM);
        b.add("TwilightForest:item.charmOfKeeping1", 1, 1, 2, Tier.STEAM);
        b.add("BiomesOPlenty:flippers", 1, 1, 1, Tier.STEAM);
        b.add("ExtraUtilities:glove", 1, 1, 1, Tier.STEAM);
        return b.build();
    }

    /**
     * Enchanting: Roguelike's enchanting rule (books, random enchanted books, lapis, redstone).
     *
     * @return the pool
     */
    public static Pool enchanting() {
        PoolBuilder b = new PoolBuilder(ENCHANTING);
        b.add(MC + "book", 1, 3, 3, Tier.STEAM);
        b.add(MC + "enchanted_book", 1, 1, 3, Tier.STEAM);
        b.add(MC + "dye#4", 8, 24, 2, Tier.STEAM);
        b.add(MC + "redstone", 8, 24, 2, Tier.STEAM);
        return b.build();
    }

    /**
     * The reinforced chests' GT materials (GGS-06-03): the Techguns staples as GT parts, then the tier's alloy and
     * the one below it.
     *
     * @return the pool; lines run up to ZPM
     */
    public static Pool materials() {
        PoolBuilder b = new PoolBuilder(MATERIALS);
        b.add(MC + "coal", 4, 16, 3, Tier.STEAM);
        b.add("gt:ingot.Iron", 4, 16, 3, Tier.STEAM);
        b.add(MC + "stone", 16, 32, 2, Tier.STEAM);
        b.add(MC + "log", 8, 16, 2, Tier.STEAM);
        b.add(MC + "gunpowder", 4, 8, 3, Tier.STEAM);
        b.add("gt:ingot.Tin", 8, 24, 3, Tier.STEAM);
        b.add("gt:ingot.Copper", 8, 24, 3, Tier.STEAM);
        b.add("gt:ingot.Lead", 6, 16, 3, Tier.STEAM);
        b.add("gt:ingot.Gold", 2, 8, 2, Tier.STEAM);
        b.add("gt:plate.Rubber", 2, 8, 3, Tier.LV);
        b.add("gt:ingot.Silver", 2, 8, 2, Tier.LV);
        b.add("gt:ingot.Steel", 2, 12, 3, Tier.LV);
        b.add("gt:plate.Plastic", 2, 8, 3, Tier.MV);
        for (Tier t : Tier.values()) {
            if (t.ordinal() >= Tier.MV.ordinal()) {
                b.add("gt:ingot." + ladder(t), 2, 8, 3, t);
            }
        }
        return b.build();
    }

    /**
     * GregGuns combat armor for the Military Base barracks: T1 (LV) and T2 (MV) combat sets.
     *
     * @return the pool
     */
    public static Pool combatArmor() {
        PoolBuilder b = new PoolBuilder(COMBAT_ARMOR);
        for (String slot : new String[] { "helmet", "chestplate", "leggings", "boots" }) {
            b.add(G + "t1_combat_" + slot, 1, 1, 2, Tier.LV);
            b.add(G + "t2_combat_" + slot, 1, 1, 1, Tier.MV);
        }
        return b.build();
    }

    /**
     * @param tier a tier
     * @return the GT material of that tier's rung (Steel for LV, Naquadah for ZPM), or null for Steam
     */
    public static @Nullable String ladder(Tier tier) {
        int i = tier.ordinal();
        return i < LADDER.length ? LADDER[i] : null;
    }
}
