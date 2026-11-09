package com.gregguns.icbm.worldgen;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.gregguns.api.Tier;
import com.gregguns.entity.npc.loot.LootDrop;
import com.gregguns.worldgen.loot.LootEntry;
import com.gregguns.worldgen.loot.LootPools;

/**
 * The shared pools the silo complex's room tables mix from (D316): food, drinks and ingredients, crew effects, medical
 * and hazmat gear, electronics, GT components and hardware, missile body parts, warhead parts, explosives, fuel, power,
 * HVAC, pumps, chemicals, bulk stock, incinerator ash, lounge extras, brig evidence, vehicle parts and junk. Pure data.
 *
 * <p>
 * Each line declares the tier of what it holds; a table drops lines above its ceiling, so an Overworld complex (EV)
 * holds nothing above EV. References follow {@link LootEntry}: {@code mod:name}, {@code mod:name#meta}, and
 * {@code gt:prefix.Material} through GT's unificator. Item ids are the pack's own (GTNH 2.9.0-RC-2): every
 * non-GregGuns,
 * non-GT reference is in {@code tools/questbook/pack_items.txt} or the checked allowlist of {@code SiloPoolsRefsTest}.
 * No line carries NBT, and no line is a live warhead, a missile chip or a schematic (D17; those stay one-off bonuses).
 *
 * <p>
 * GT metaitems are {@code gregtech:gt.metaitem.01} and {@code .03} at their {@code IDMetaItem} id plus 32000.
 * Source:
 * https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/items/IDMetaItem01.java
 * Galacticraft metas follow {@code ItemBasic.names}, {@code ItemBasicAsteroids.names} and {@code ItemCanister.names}.
 * Source:
 * https://github.com/GTNewHorizons/Galacticraft/blob/3.4.34-GTNH/src/main/java/micdoodle8/mods/galacticraft/core/items/ItemBasic.java
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/loot-and-ceilings.md (Room tables, Pools); D316
public final class SiloPools {

    private static final Tier S = Tier.STEAM, LV = Tier.LV, MV = Tier.MV, HV = Tier.HV, EV = Tier.EV, IV = Tier.IV,
        LUV = Tier.LUV, ZPM = Tier.ZPM;
    private static final String MC = "minecraft:";
    private static final String HC = "harvestcraft:";
    private static final String G = "gregguns:";
    private static final String GT1 = "gregtech:gt.metaitem.01#";
    private static final String GT3 = "gregtech:gt.metaitem.03#";
    private static final String GCC = "GalacticraftCore:item.";

    /** The spoil items (D317). */
    public static final String ROTTEN = MC + "rotten_flesh", BOWL = MC + "bowl", BOTTLE = MC + "glass_bottle",
        SCRAP = "IC2:itemScrap", DEAD_BATTERY = "IC2:itemBatREDischarged";

    /** Pool names; also the {@link LootEntry#pool()} of each line a table takes from them. */
    public static final String COMMON = "common", MEALS = "meals", SNACKS = "snacks", DRINKS = "drinks",
        INGREDIENTS = "ingredients", PRODUCE = "produce", RAWMEAT = "rawmeat", PRESERVED = "preserved",
        COOKWARE = "cookware", SEEDS = "seeds", FARMING = "farming", MUSHROOM = "mushroom", BUNKARMOR = "bunkarmor",
        PERSONAL = "personal", LINENS = "linens", MEDICAL = "medical", HAZMAT = "hazmat", ELECTRONICS = "electronics",
        CONTROL = "control", OFFICE = "office", COMPONENTS = "components", HARDWARE = "hardware", TOOLS = "silo_tools",
        MISSILEBODY = "missilebody", WARHEADPARTS = "warheadparts", EXPLOSIVES = "explosives", FUEL = "fuel",
        POWER = "power", HVAC = "hvac", PUMPS = "pumps", CHEM = "chem", BULK = "bulk", INCINERATOR = "incinerator",
        LOUNGE = "lounge", EVIDENCE = "evidence", VEHICLE = "vehicle", TARGETS = "targets", JUNK = "silo_junk";

    private SiloPools() {}

    /** Builds one pool. */
    private static final class B {

        private final String name;
        private final List<LootEntry> lines = new ArrayList<>();

        B(String name) {
            this.name = name;
        }

        B add(String ref, int min, int max, int weight, Tier tier) {
            lines.add(new LootEntry(LootDrop.range(ref, min, max), weight, tier, name, null));
            return this;
        }

        B one(String ref, int weight, Tier tier) {
            return add(ref, 1, 1, weight, tier);
        }

        LootPools.Pool build() {
            return LootPools.Pool.of(name, lines);
        }
    }

    /** @return everyday clutter of a lived-in base, mixed into every table at 10 to 20% */
    public static LootPools.Pool common() {
        return new B(COMMON).add(MC + "torch", 4, 16, 4, S)
            .add(MC + "coal", 2, 8, 3, S)
            .add(MC + "paper", 2, 8, 2, S)
            .add(MC + "string", 2, 8, 2, S)
            .add(MC + "bread", 1, 4, 2, S)
            .add(MC + "apple", 1, 4, 1, S)
            .one(MC + "bucket", 1, S)
            .add(MC + "flint", 1, 4, 1, S)
            .add("gt:ingot.Iron", 2, 8, 2, S)
            .add("gt:ingot.Copper", 2, 8, 1, S)
            .add(G + "bandages", 1, 2, 1, LV)
            .add(HC + "coffeeItem", 1, 2, 1, S)
            .build();
    }

    /** @return cooked meals; they spoil to rotten flesh, stews and soups to a bowl */
    public static LootPools.Pool meals() {
        return new B(MEALS).add(MC + "bread", 2, 6, 5, S)
            .add(MC + "cooked_beef", 2, 6, 4, S)
            .add(MC + "cooked_porkchop", 2, 6, 3, S)
            .add(MC + "cooked_chicken", 2, 6, 3, S)
            .add(MC + "baked_potato", 2, 8, 3, S)
            .add(MC + "cooked_fished", 2, 5, 2, S)
            .one(MC + "mushroom_stew", 2, S)
            .add(HC + "meatystewItem", 1, 3, 2, S)
            .add(HC + "spagettiandmeatballsItem", 1, 3, 2, S)
            .add(HC + "hamburgerItem", 1, 4, 2, S)
            .add(HC + "cheeseburgerItem", 1, 3, 2, S)
            .add(HC + "pizzaItem", 1, 2, 2, S)
            .add(HC + "friedchickenItem", 1, 4, 2, S)
            .add(HC + "roastchickenItem", 1, 2, 1, S)
            .add(HC + "steakandchipsItem", 1, 3, 1, S)
            .add(HC + "fishandchipsItem", 1, 3, 1, S)
            .add(HC + "mashedpotatoesItem", 1, 3, 2, S)
            .add(HC + "scrambledeggItem", 1, 4, 2, S)
            .add(HC + "pancakesItem", 1, 4, 2, S)
            .add(HC + "vegetablesoupItem", 1, 3, 2, S)
            .add(HC + "chickennoodlesoupItem", 1, 3, 2, S)
            .add(HC + "potatosoupItem", 1, 3, 1, S)
            .add(HC + "tomatosoupItem", 1, 3, 1, S)
            .add(HC + "friedriceItem", 1, 3, 1, S)
            .add(HC + "wafflesItem", 1, 3, 1, S)
            .add(HC + "grilledcheeseItem", 1, 3, 1, S)
            .add(HC + "hotdogItem", 1, 3, 1, S)
            .add(HC + "bakedbeansItem", 1, 3, 1, S)
            .add(HC + "mixedsaladItem", 1, 3, 1, S)
            .add(HC + "meatpieItem", 1, 2, 1, S)
            .add(HC + "shepardspieItem", 1, 2, 1, S)
            .build();
    }

    /** @return the infirmary's soups only (a sick bay serves broth) */
    public static LootPools.Pool soups() {
        return new B(MEALS).one(MC + "mushroom_stew", 2, S)
            .add(HC + "vegetablesoupItem", 1, 3, 2, S)
            .add(HC + "chickennoodlesoupItem", 1, 3, 3, S)
            .add(HC + "potatosoupItem", 1, 3, 1, S)
            .add(HC + "tomatosoupItem", 1, 3, 1, S)
            .build();
    }

    /** @return snacks */
    public static LootPools.Pool snacks() {
        return new B(SNACKS).add(MC + "cookie", 4, 12, 3, S)
            .add(MC + "apple", 2, 6, 3, S)
            .add(MC + "pumpkin_pie", 1, 3, 1, S)
            .one(MC + "cake", 1, S)
            .add(HC + "chocolatebarItem", 1, 4, 2, S)
            .add(HC + "donutItem", 1, 4, 2, S)
            .add(HC + "jellydonutItem", 1, 3, 1, S)
            .add(HC + "brownieItem", 1, 4, 1, S)
            .add(HC + "cinnamonrollItem", 1, 3, 1, S)
            .add(HC + "blueberrymuffinItem", 1, 3, 1, S)
            .add(HC + "friesItem", 1, 4, 2, S)
            .add(HC + "softpretzelItem", 1, 4, 1, S)
            .add(HC + "crackerItem", 2, 8, 2, S)
            .add(HC + "trailmixItem", 1, 4, 2, S)
            .add(HC + "beefjerkyItem", 2, 6, 3, S)
            .add(HC + "saltedsunflowerseedsItem", 2, 6, 1, S)
            .add(HC + "gummybearsItem", 1, 4, 1, S)
            .add(HC + "jellybeansItem", 1, 4, 1, S)
            .add(HC + "popcornItem", 2, 6, 2, S)
            .build();
    }

    /** @return drinks; they spoil to an empty glass bottle */
    public static LootPools.Pool drinks() {
        return new B(DRINKS).add(HC + "coffeeItem", 1, 4, 4, S)
            .add(HC + "teaItem", 1, 3, 2, S)
            .add(HC + "hotchocolateItem", 1, 3, 1, S)
            .add(HC + "colasodaItem", 1, 4, 3, S)
            .add(HC + "rootbeersodaItem", 1, 3, 2, S)
            .add(HC + "orangesodaItem", 1, 3, 1, S)
            .add(HC + "orangejuiceItem", 1, 3, 2, S)
            .add(HC + "applejuiceItem", 1, 3, 2, S)
            .add(HC + "chocolatemilkItem", 1, 3, 1, S)
            .add(HC + "energydrinkItem", 1, 3, 2, S)
            .one(MC + "milk_bucket", 1, S)
            .add(MC + "potion#0", 1, 4, 1, S)
            .build();
    }

    /** @return kitchen staples (the owner's "ingredients too") */
    public static LootPools.Pool ingredients() {
        return new B(INGREDIENTS).add(HC + "flourItem", 4, 16, 4, S)
            .add(HC + "doughItem", 2, 8, 2, S)
            .add(HC + "butterItem", 2, 6, 2, S)
            .add(HC + "cheeseItem", 2, 6, 2, S)
            .add(HC + "freshmilkItem", 2, 8, 2, S)
            .add(HC + "freshwaterItem", 2, 8, 2, S)
            .add(HC + "saltItem", 4, 16, 3, S)
            .add(HC + "blackpepperItem", 2, 8, 2, S)
            .add(HC + "oliveoilItem", 1, 4, 1, S)
            .add(HC + "ketchupItem", 1, 4, 1, S)
            .add(HC + "mayoItem", 1, 4, 1, S)
            .add(HC + "mustardItem", 1, 4, 1, S)
            .add(HC + "vinegarItem", 1, 4, 1, S)
            .add(HC + "stockItem", 1, 4, 1, S)
            .add(HC + "heavycreamItem", 1, 4, 1, S)
            .add(HC + "pastaItem", 2, 8, 2, S)
            .add(HC + "riceItem", 4, 12, 2, S)
            .add(HC + "honeyItem", 1, 4, 1, S)
            .add(HC + "spiceleafItem", 2, 8, 1, S)
            .add(MC + "sugar", 4, 16, 3, S)
            .add(MC + "egg", 2, 12, 3, S)
            .add(MC + "wheat", 4, 16, 2, S)
            .build();
    }

    /** @return fresh produce */
    public static LootPools.Pool produce() {
        return new B(PRODUCE).add(MC + "potato", 4, 16, 4, S)
            .add(MC + "carrot", 4, 12, 3, S)
            .add(MC + "apple", 2, 8, 2, S)
            .add(MC + "melon", 4, 12, 2, S)
            .add(MC + "reeds", 4, 12, 1, S)
            .add(HC + "tomatoItem", 2, 8, 3, S)
            .add(HC + "onionItem", 2, 8, 3, S)
            .add(HC + "garlicItem", 2, 6, 2, S)
            .add(HC + "lettuceItem", 2, 6, 2, S)
            .add(HC + "cornItem", 2, 8, 2, S)
            .add(HC + "cabbageItem", 1, 4, 2, S)
            .add(HC + "bellpepperItem", 2, 6, 1, S)
            .add(HC + "cucumberItem", 2, 6, 1, S)
            .add(HC + "beanItem", 2, 8, 2, S)
            .add(HC + "soybeanItem", 2, 8, 1, S)
            .add(HC + "sweetpotatoItem", 2, 6, 1, S)
            .add(HC + "eggplantItem", 1, 4, 1, S)
            .add(HC + "chiliItem", 1, 4, 1, S)
            .add(HC + "bananaItem", 2, 6, 1, S)
            .add(HC + "orangeItem", 2, 6, 1, S)
            .add(HC + "lemonItem", 2, 6, 1, S)
            .add(HC + "strawberryItem", 2, 8, 1, S)
            .add(HC + "grapeItem", 2, 8, 1, S)
            .add(HC + "cherryItem", 2, 6, 1, S)
            .add(HC + "peanutItem", 2, 8, 1, S)
            .build();
    }

    /** @return raw meat */
    public static LootPools.Pool rawmeat() {
        return new B(RAWMEAT).add(MC + "beef", 2, 8, 4, S)
            .add(MC + "porkchop", 2, 8, 3, S)
            .add(MC + "chicken", 2, 8, 3, S)
            .add(MC + "fish", 2, 8, 2, S)
            .add(MC + "fish#1", 1, 4, 1, S)
            .add(HC + "sausageItem", 2, 6, 1, S)
            .build();
    }

    /** @return food that never spoils, so a ruin's pantry still feeds you */
    public static LootPools.Pool preserved() {
        return new B(PRESERVED).add("IC2:itemTinCanFilled", 4, 16, 5, S)
            .add(HC + "beefjerkyItem", 2, 8, 3, S)
            .add(HC + "crackerItem", 2, 8, 2, S)
            .add(HC + "trailmixItem", 1, 4, 2, S)
            .add(HC + "strawberryjellyItem", 1, 4, 1, S)
            .add(HC + "grapejellyItem", 1, 4, 1, S)
            .add(MC + "snowball", 4, 16, 1, S)
            .add(MC + "packed_ice", 1, 4, 1, S)
            .build();
    }

    /** @return cookware */
    public static LootPools.Pool cookware() {
        B b = new B(COOKWARE);
        for (String tool : new String[] { "potItem", "skilletItem", "saucepanItem", "mixingbowlItem",
            "cuttingboardItem", "bakewareItem", "juicerItem", "mortarandpestleItem" }) {
            b.one(HC + tool, 2, S);
        }
        return b.add(MC + "bowl", 2, 8, 3, S)
            .add(MC + "glass_bottle", 2, 8, 2, S)
            .one(MC + "bucket", 2, S)
            .build();
    }

    /** @return seeds */
    public static LootPools.Pool seeds() {
        B b = new B(SEEDS).add(MC + "wheat_seeds", 4, 16, 4, S)
            .add(MC + "pumpkin_seeds", 2, 8, 2, S)
            .add(MC + "melon_seeds", 2, 8, 2, S)
            .add(HC + "tomatoseedItem", 2, 6, 2, S)
            .add(HC + "cornseedItem", 2, 6, 2, S);
        for (String seed : new String[] { "onion", "lettuce", "cabbage", "bean", "rice", "coffee", "peanut", "garlic",
            "soybean", "bellpepper", "cucumber", "strawberry", "sweetpotato", "tea" }) {
            b.add(HC + seed + "seedItem", 2, 6, 1, S);
        }
        return b.build();
    }

    /** @return the farm's supplies */
    public static LootPools.Pool farming() {
        return new B(FARMING).add(MC + "dye#15", 4, 16, 4, S)
            .add("Forestry:fertilizerCompound", 2, 8, 2, S)
            .add(MC + "hay_block", 1, 4, 2, S)
            .add(MC + "wheat", 4, 16, 3, S)
            .one(MC + "iron_hoe", 1, S)
            .one(MC + "stone_hoe", 1, S)
            .one(MC + "water_bucket", 1, S)
            .one(MC + "shears", 1, S)
            .build();
    }

    /** @return the mushroom cellar's crop */
    public static LootPools.Pool mushroom() {
        return new B(MUSHROOM).add(MC + "brown_mushroom", 4, 16, 5, S)
            .add(MC + "red_mushroom", 2, 12, 4, S)
            .add(HC + "whitemushroomItem", 2, 8, 2, S)
            .one(MC + "mushroom_stew", 2, S)
            .add(HC + "grilledmushroomItem", 1, 4, 1, S)
            .add(HC + "stuffedmushroomItem", 1, 3, 1, S)
            .add(HC + "mushroomrisottoItem", 1, 2, 1, S)
            .add(HC + "whitemushroomseedItem", 1, 4, 1, S)
            .add(MC + "mycelium", 2, 8, 1, S)
            .add(MC + "bowl", 2, 6, 1, S)
            .build();
    }

    private static final String[] PIECES = { "helmet", "chestplate", "leggings", "boots" };

    /** @return the bunks' armor-heavy pool (armor tiers from {@code ArmorCatalog}) */
    public static LootPools.Pool bunkarmor() {
        B b = new B(BUNKARMOR);
        set(b, G + "t1_combat_", 3, LV);
        set(b, G + "t2_combat_", 3, MV);
        set(b, G + "t2_commando_", 1, MV);
        set(b, G + "t2_riot_", 1, MV);
        b.one(G + "t2_beret", 2, MV);
        set(b, G + "t3_combat_", 1, EV);
        set(b, G + "t3_power_", 1, IV);
        set(b, G + "t3_exo_", 1, IV);
        set(b, MC + "leather_", 1, S);
        set(b, MC + "chainmail_", 1, S);
        set(b, MC + "iron_", 1, S);
        for (String p : new String[] { "helm", "chest", "leggings", "boots" }) {
            b.one(HC + "hardenedleather" + p + "Item", 1, S);
        }
        return b.one(G + "night_vision_goggles", 1, MV)
            .one(G + "gas_mask", 1, MV)
            .one(G + "ammo_pouch", 1, MV)
            .one(G + "tactical_mask", 1, HV)
            .build();
    }

    /** @return the armory's combat sets only (no vanilla, no leather) */
    public static LootPools.Pool combatArmor() {
        B b = new B(BUNKARMOR);
        set(b, G + "t1_combat_", 2, LV);
        set(b, G + "t2_combat_", 3, MV);
        set(b, G + "t2_riot_", 1, MV);
        set(b, G + "t3_combat_", 2, EV);
        set(b, G + "t3_power_", 1, IV);
        return b.one(G + "tactical_mask", 1, HV)
            .build();
    }

    private static void set(B b, String prefix, int weight, Tier tier) {
        for (String p : PIECES) {
            b.one(prefix + p, weight, tier);
        }
    }

    /** @return foot-locker effects */
    public static LootPools.Pool personal() {
        return new B(PERSONAL).add(MC + "book", 1, 2, 2, S)
            .one(MC + "writable_book", 1, S)
            .add(MC + "paper", 2, 8, 2, S)
            .one(MC + "compass", 1, S)
            .one(MC + "clock", 1, S)
            .add(MC + "wool", 1, 4, 2, S)
            .add(MC + "leather", 1, 4, 1, S)
            .one(MC + "name_tag", 1, S)
            .add(MC + "gold_nugget", 1, 6, 1, S)
            .add(G + "coin", 1, 8, 2, S)
            .add(MC + "torch", 2, 8, 1, S)
            .one("BiblioCraft:item.BiblioGlasses", 1, S)
            .add(HC + "chocolatebarItem", 1, 3, 1, S)
            .build();
    }

    /** @return laundry */
    public static LootPools.Pool linens() {
        return new B(LINENS).add(MC + "wool", 2, 8, 4, S)
            .add(MC + "wool#8", 2, 8, 2, S)
            .add(HC + "wovencottonItem", 2, 8, 2, S)
            .add(HC + "cottonItem", 2, 8, 1, S)
            .add(MC + "string", 2, 6, 2, S)
            .add(MC + "leather", 1, 4, 1, S)
            .one(MC + "sponge", 1, S)
            .build();
    }

    /** @return the infirmary's stock */
    public static LootPools.Pool medical() {
        return new B(MEDICAL).add(G + "bandages", 1, 6, 5, LV)
            .add(G + "medkit", 1, 3, 3, MV)
            .add(G + "antidote", 1, 2, 1, HV)
            .add(MC + "potion#8197", 1, 2, 3, S)
            .one(MC + "potion#8229", 1, S)
            .add(MC + "potion#8193", 1, 2, 2, S)
            .one(MC + "potion#8225", 1, S)
            .one(MC + "potion#8195", 1, S)
            .one(MC + "potion#8198", 1, S)
            .one(MC + "potion#16421", 1, S)
            .one(MC + "golden_apple", 1, S)
            .add(MC + "speckled_melon", 1, 4, 1, S)
            .add(MC + "glass_bottle", 2, 8, 2, S)
            .add(MC + "paper", 2, 6, 1, S)
            .add(MC + "wool", 1, 4, 1, S)
            .add("gt:plate.Rubber", 2, 6, 2, LV)
            .build();
    }

    /** @return hazmat gear */
    public static LootPools.Pool hazmat() {
        B b = new B(HAZMAT).one("IC2:itemArmorHazmatHelmet", 2, LV)
            .one("IC2:itemArmorHazmatChestplate", 2, LV)
            .one("IC2:itemArmorHazmatLeggings", 2, LV)
            .one("IC2:itemArmorRubBoots", 2, LV)
            .one(G + "gas_mask", 3, MV)
            .add(G + "part_gas_mask_filter", 1, 4, 4, MV);
        set(b, G + "t3_hev_", 1, EV);
        return b.one(G + "antidote", 1, HV)
            .one(G + "oxygen_mask", 1, HV)
            .one(G + "tank_air", 1, HV)
            .one(GCC + "oxygenMask", 1, HV)
            .one(GCC + "oxygenTankLightFull", 1, HV)
            .add("gt:plate.Rubber", 2, 6, 2, LV)
            .build();
    }

    /** @return circuits, boards, data storage, batteries and wire */
    public static LootPools.Pool electronics() {
        return new B(ELECTRONICS).add("IC2:itemPartCircuit", 1, 4, 4, LV)
            .add(GT3 + "32078", 1, 4, 2, LV)
            .add(GT1 + "32702", 1, 3, 3, MV)
            .add("IC2:itemPartCircuitAdv", 1, 2, 2, HV)
            .one(GT1 + "32704", 1, EV)
            .add(GT1 + "32710", 2, 6, 2, LV)
            .add(GT3 + "32007", 1, 4, 1, MV)
            .add(GT1 + "32708", 1, 2, 2, HV)
            .one(GT1 + "32707", 1, IV)
            .one(GT1 + "32518", 1, LV)
            .one(GT1 + "32528", 1, MV)
            .one("IC2:itemFreq", 1, LV)
            .add("gt:wireFine.Copper", 4, 16, 2, LV)
            .add("gt:wireFine.Gold", 4, 16, 2, HV)
            .add("gt:wireFine.Electrum", 4, 16, 1, HV)
            .add("gt:cableGt01.Tin", 4, 12, 2, LV)
            .add("gt:cableGt01.Copper", 4, 12, 1, LV)
            .add("gt:cableGt01.Gold", 4, 12, 1, HV)
            .add("gt:plate.Plastic", 2, 8, 2, MV)
            .build();
    }

    /** @return launch room clutter: papers, maps, guidance computers, sensors and emitters */
    public static LootPools.Pool control() {
        return new B(CONTROL).add(MC + "paper", 4, 16, 3, S)
            .add(MC + "map", 1, 2, 2, S)
            .one(MC + "writable_book", 1, S)
            .one("BiblioCraft:item.BiblioClipboard", 2, S)
            .one(MC + "compass", 1, S)
            .one(MC + "clock", 2, S)
            .one(G + "guidance_computer_mk1", 1, HV)
            .one(G + "guidance_computer_mk2", 1, EV)
            .one(G + "remote_detonator_hv", 1, HV)
            .add(GT1 + "32690", 1, 2, 2, LV)
            .one(GT1 + "32692", 1, HV)
            .add(GT1 + "32680", 1, 2, 1, LV)
            .one(GT1 + "32682", 1, HV)
            .build();
    }

    /** @return the commander's office */
    public static LootPools.Pool office() {
        return new B(OFFICE).add(MC + "book", 1, 3, 3, S)
            .one(MC + "writable_book", 2, S)
            .add(MC + "paper", 4, 16, 4, S)
            .one(MC + "map", 2, S)
            .one("BiblioCraft:item.BiblioClipboard", 2, S)
            .one("BiblioCraft:item.BiblioGlasses", 1, S)
            .one("BiblioCraft:item.BiblioGlasses#1", 1, S)
            .one("BiblioCraft:item.BiblioGlasses#2", 1, S)
            .add(MC + "feather", 1, 4, 1, S)
            .add(MC + "dye", 1, 4, 1, S)
            .one(MC + "enchanted_book", 1, S)
            .add(G + "coin", 4, 16, 2, S)
            .add("gt:ingot.Gold", 1, 4, 1, S)
            .add("gt:ingot.Silver", 1, 4, 1, S)
            .one(G + "t2_beret", 1, MV)
            .build();
    }

    /** @return GT components: motors, pumps, pistons, conveyors, robot arms, duct tape */
    public static LootPools.Pool components() {
        return new B(COMPONENTS).add(GT1 + "32600", 1, 2, 3, LV)
            .one(GT1 + "32601", 2, MV)
            .one(GT1 + "32602", 1, HV)
            .one(GT1 + "32603", 1, EV)
            .one(GT1 + "32610", 2, LV)
            .one(GT1 + "32611", 1, MV)
            .one(GT1 + "32612", 1, HV)
            .one(GT1 + "32640", 2, LV)
            .one(GT1 + "32641", 1, MV)
            .one(GT1 + "32630", 2, LV)
            .one(GT1 + "32631", 1, MV)
            .one(GT1 + "32650", 1, LV)
            .one(GT1 + "32651", 1, MV)
            .add(GT1 + "32764", 1, 4, 2, MV)
            .add(GT1 + "32500", 1, 2, 1, LV)
            .build();
    }

    /** @return GT hardware by the unificator (the ladder rungs are added by the table) */
    public static LootPools.Pool hardware() {
        return new B(HARDWARE).add("gt:screw.Steel", 4, 16, 3, LV)
            .add("gt:bolt.Steel", 4, 16, 3, LV)
            .add("gt:stick.Steel", 2, 8, 2, LV)
            .add("gt:ring.Rubber", 2, 8, 2, LV)
            .add("gt:spring.Steel", 2, 6, 1, LV)
            .add("gt:gearGtSmall.Steel", 1, 4, 2, LV)
            .add("gt:gearGt.Steel", 1, 2, 1, LV)
            .add("gt:rotor.Steel", 1, 2, 1, LV)
            .add("gt:plate.Iron", 4, 12, 2, S)
            .add("gt:plate.Steel", 2, 8, 3, LV)
            .add("gt:plate.Aluminium", 2, 8, 2, MV)
            .add("gt:plate.StainlessSteel", 1, 6, 1, HV)
            .add("gt:plate.Titanium", 1, 4, 1, EV)
            .add("gt:ingot.SolderingAlloy", 2, 8, 1, LV)
            .add("gt:ingot.TinAlloy", 2, 8, 1, LV)
            .build();
    }

    /** @return hand tools with no NBT */
    public static LootPools.Pool tools() {
        return new B(TOOLS).one(MC + "iron_pickaxe", 1, S)
            .one(MC + "iron_shovel", 1, S)
            .one(MC + "iron_axe", 1, S)
            .one(MC + "shears", 1, S)
            .one(MC + "flint_and_steel", 1, S)
            .one(MC + "bucket", 2, S)
            .add(MC + "lead", 1, 2, 1, S)
            .one(G + "crowbar", 2, LV)
            .one(G + "combat_knife", 1, LV)
            .one("IC2:itemTreetap", 1, S)
            .one("Railcraft:tool.crowbar", 1, S)
            .one(GCC + "standardWrench", 1, LV)
            .build();
    }

    /**
     * The missile body's own parts (Galacticraft, D69) and, rarely, a whole body. The body is the planet's rocket
     * tier; an abandoned complex's body is always a salvage body ({@link #spoilOf}).
     *
     * @param ceiling  the complex's ceiling
     * @param withBody whether the rare whole body is in (the assembly floor only)
     * @return the pool
     */
    public static LootPools.Pool missilebody(int ceiling, boolean withBody) {
        int tier = ComplexContents.bodyTier(ceiling);
        B b = new B(MISSILEBODY).add(GCC + "basicItem#9", 2, 8, 5, HV)
            .add(GCC + "heavyPlating", 1, 4, 3, HV)
            .add(GCC + "rocketFins", 1, 4, 4, HV)
            .one(GCC + "engine", 2, HV)
            .add(GCC + "noseCone", 1, 2, 2, HV)
            .add("GalacticraftMars:item.itemBasicAsteroids#6", 1, 4, 2, EV)
            .add("GalacticraftMars:item.itemBasicAsteroids#2", 1, 2, 1, IV)
            .one("GalacticraftMars:item.itemBasicAsteroids#1", 1, IV)
            .one(G + "guidance_computer_mk1", 2, HV)
            .one(G + "guidance_computer_mk2", 1, EV);
        if (withBody) {
            b.one(body(tier), 1, bodyTierOf(tier));
        }
        return b.add("gt:plate.StainlessSteel", 2, 8, 2, HV)
            .add("gt:frameGt.StainlessSteel", 1, 4, 1, HV)
            .add("gt:cell.Lubricant", 1, 4, 1, LV)
            .add("IC2:itemPartCircuitAdv", 1, 2, 1, HV)
            .build();
    }

    /** @return the reference of a whole missile body of a tier (1 to 3) */
    static String body(int tier) {
        return G + "missile_body_t" + tier;
    }

    /** @return the tier a whole body of a missile tier declares: T1 at EV, T2 at IV, T3 at LuV */
    private static Tier bodyTierOf(int tier) {
        return tier <= 1 ? EV : tier == 2 ? IV : LUV;
    }

    /**
     * @return inert warhead parts, fuzes, liners, sleeves and launcher warheads; never a live warhead (D17). An inert
     *         casing carries its rocket's nose cone and hull since D328, so the T1 casing weighs 1, as the others do
     *         (D330)
     */
    public static LootPools.Pool warheadparts() {
        return new B(WARHEADPARTS).one(G + "inert_warhead_t1", 1, HV)
            .one(G + "inert_warhead_t2", 1, EV)
            .add(G + "plastic_explosive", 2, 8, 4, HV)
            .add(G + "fuze_assembly_mk1", 1, 2, 3, HV)
            .one(G + "fuze_assembly_mk2", 1, EV)
            .add(G + "shaped_charge_liner_cone", 1, 4, 2, HV)
            .add(G + "fragmentation_sleeve_steel", 1, 2, 2, HV)
            .one(G + "fragmentation_sleeve_titanium", 1, EV)
            .add(G + "dispersal_canister", 1, 4, 2, HV)
            .add(G + "napalm_canister", 1, 2, 1, HV)
            .add(G + "launcher_warhead_he", 1, 3, 1, HV)
            .add(G + "launcher_warhead_grenade_he", 1, 3, 1, HV)
            .add(G + "launcher_warhead_incendiary", 1, 3, 1, HV)
            .add(G + "launcher_warhead_demolition", 1, 2, 1, EV)
            .one(G + "defuser", 1, HV)
            .one(G + "inert_warhead_t3", 1, ZPM)
            .one(G + "fuze_assembly_mk3", 1, LUV)
            .build();
    }

    /** @return bulk explosives and their stock */
    public static LootPools.Pool explosives() {
        return new B(EXPLOSIVES).add(MC + "gunpowder", 4, 16, 4, S)
            .add("IC2:blockITNT", 1, 4, 2, S)
            .add("IC2:itemDynamite", 2, 8, 1, S)
            .add("gt:dust.Saltpeter", 4, 12, 2, S)
            .add("gt:dust.Sulfur", 4, 12, 2, S)
            .add("gt:plate.Lead", 2, 8, 2, LV)
            .add("gt:plate.Polytetrafluoroethylene", 1, 4, 1, MV)
            .add(G + "stielgranate", 2, 6, 1, LV)
            .add(G + "frag_grenade", 1, 4, 1, MV)
            .build();
    }

    /** @return the fuel depot's stock (the fuel-cell bonus comes on top) */
    public static LootPools.Pool fuel() {
        return new B(FUEL).add(MC + "coal", 8, 24, 3, S)
            .add("Railcraft:fuel.coke", 4, 16, 2, S)
            .add("gt:cell.Lubricant", 1, 4, 2, LV)
            .add("gt:cell.Creosote", 1, 4, 1, LV)
            .add(GCC + "canister", 1, 4, 2, LV)
            .add(GCC + "canister#1", 1, 4, 1, LV)
            .add(GT1 + "32405", 1, 2, 1, MV)
            .add("IC2:itemCellEmpty", 2, 8, 2, LV)
            .add("gt:dust.Sulfur", 2, 8, 1, S)
            .one(G + "tank_fuel~full", 1, HV)
            .build();
    }

    /** @return generator hall spares */
    public static LootPools.Pool power() {
        return new B(POWER).one(GT1 + "32518", 2, LV)
            .one(GT1 + "32528", 1, MV)
            .one(GT1 + "32538", 1, HV)
            .add("gt:cableGt01.Tin", 4, 16, 3, LV)
            .add("gt:cableGt01.Copper", 4, 12, 2, LV)
            .add("gt:cableGt01.Gold", 2, 8, 1, HV)
            .add("gt:cableGt01.Aluminium", 2, 8, 1, EV)
            .add("gt:wireGt01.Copper", 4, 12, 1, LV)
            .add("gt:rotor.Steel", 1, 2, 1, LV)
            .add("gt:cell.Lubricant", 1, 4, 2, LV)
            .add(MC + "redstone", 4, 16, 2, S)
            .add(MC + "coal", 4, 16, 2, S)
            .add("Railcraft:fuel.coke", 4, 12, 2, S)
            .build();
    }

    /** @return air handling: fans, vents, filters, rotors */
    public static LootPools.Pool hvac() {
        return new B(HVAC).add(GCC + "airFan", 1, 2, 3, HV)
            .add(GCC + "airVent", 1, 4, 3, HV)
            .add(GT1 + "32729", 1, 2, 2, LV)
            .add(G + "part_gas_mask_filter", 1, 4, 2, MV)
            .one(GT1 + "32600", 2, LV)
            .add("gt:rotor.Steel", 1, 2, 2, LV)
            .add("gt:rotor.Tin", 1, 2, 1, LV)
            .add("gt:plate.Aluminium", 2, 6, 1, MV)
            .add("gt:pipeMedium.Steel", 1, 4, 1, LV)
            .add(MC + "wool", 2, 6, 1, S)
            .build();
    }

    /** @return pumps, pipes, seals and water treatment chemicals */
    public static LootPools.Pool pumps() {
        return new B(PUMPS).one(GT1 + "32610", 3, LV)
            .one(GT1 + "32611", 2, MV)
            .one(GT1 + "32612", 1, HV)
            .add("gt:pipeMedium.Bronze", 2, 8, 2, S)
            .add("gt:pipeMedium.Steel", 2, 8, 2, LV)
            .add("gt:ring.Rubber", 2, 8, 3, LV)
            .add("gt:plate.Rubber", 2, 6, 2, LV)
            .add("IC2:itemCellEmpty", 2, 8, 2, LV)
            .add("gt:cell.Water", 2, 8, 2, LV)
            .add("gt:cell.Chlorine", 1, 4, 1, MV)
            .add("gt:dust.Calcite", 2, 8, 1, S)
            .one(MC + "bucket", 2, S)
            .one(MC + "water_bucket", 1, S)
            .one(GT1 + "32729", 1, LV)
            .build();
    }

    /** @return the chem lab's cells, dusts and glassware */
    public static LootPools.Pool chem() {
        B b = new B(CHEM).add("gt:cell.SulfuricAcid", 1, 4, 2, LV)
            .add("gt:cell.NitricAcid", 1, 2, 1, MV)
            .add("gt:cell.Ethanol", 1, 4, 2, LV)
            .add("gt:cell.Acetone", 1, 2, 1, MV)
            .add("gt:cell.Hydrogen", 1, 4, 2, LV)
            .add("gt:cell.Oxygen", 1, 4, 2, LV);
        for (String m : new String[] { "Nitrogen", "Methane", "Mercury" }) {
            b.add("gt:cell." + m, 1, 4, 1, LV);
        }
        b.add("gt:cell.Chlorine", 1, 4, 1, MV)
            .add("gt:cell.Ammonia", 1, 4, 1, HV);
        b.add("gt:dust.Salt", 4, 16, 2, S)
            .add("gt:dust.Sulfur", 2, 12, 2, S);
        for (String m : new String[] { "Saltpeter", "Calcite", "Carbon", "Phosphorus" }) {
            b.add("gt:dust." + m, 2, 8, 1, LV);
        }
        return b.add("IC2:itemCellEmpty", 4, 16, 3, LV)
            .add(MC + "glass_bottle", 2, 8, 2, S)
            .add(MC + "fermented_spider_eye", 1, 4, 1, S)
            .build();
    }

    /** @return warehouse stock (the ingot ladder above EV is added by the table) */
    public static LootPools.Pool bulk() {
        return new B(BULK).add("gt:ingot.Iron", 8, 32, 4, S)
            .add("gt:ingot.Copper", 8, 24, 3, S)
            .add("gt:ingot.Tin", 8, 24, 3, S)
            .add("gt:ingot.Steel", 4, 16, 3, LV)
            .add("gt:ingot.Bronze", 4, 16, 2, S)
            .add("gt:ingot.Lead", 4, 16, 2, S)
            .add("gt:ingot.Aluminium", 4, 12, 2, MV)
            .add("gt:ingot.StainlessSteel", 2, 8, 1, HV)
            .add("gt:ingot.Titanium", 1, 4, 1, EV)
            .add("gt:plate.Iron", 4, 16, 2, S)
            .add("gt:plate.Steel", 4, 16, 2, LV)
            .add("gt:plate.Rubber", 2, 8, 2, LV)
            .add("gt:plate.Plastic", 2, 8, 1, MV)
            .add(MC + "wool", 8, 32, 2, S)
            .add(MC + "log", 8, 32, 1, S)
            .add(MC + "glass", 8, 32, 1, S)
            .add(MC + "torch", 8, 32, 2, S)
            .add(MC + "coal", 8, 32, 2, S)
            .build();
    }

    /** @return ash, scrap and slag */
    public static LootPools.Pool incinerator() {
        return new B(INCINERATOR).add("gt:dust.Ash", 4, 16, 4, S)
            .add("gt:dustSmall.Ash", 2, 8, 2, S)
            .add(SCRAP, 4, 16, 4, LV)
            .one("IC2:itemScrapbox", 1, LV)
            .add(MC + "coal", 4, 16, 3, S)
            .add(MC + "coal#1", 4, 16, 2, S)
            .add(MC + "bone", 2, 8, 2, S)
            .add("gt:nugget.Iron", 2, 8, 2, S)
            .add("gt:nugget.Copper", 2, 8, 1, S)
            .add("gt:nugget.Lead", 1, 6, 1, S)
            .add("gt:nugget.Gold", 1, 4, 1, S)
            .add("gt:plate.Iron", 1, 4, 1, S)
            .build();
    }

    /** @return the rec lounge's records, cards and poker pot */
    public static LootPools.Pool lounge() {
        B b = new B(LOUNGE);
        for (String r : new String[] { "13", "cat", "blocks", "chirp", "far", "mall", "mellohi", "stal", "strad",
            "ward", "11", "wait" }) {
            b.one(MC + "record_" + r, 1, S);
        }
        return b.add(MC + "book", 1, 2, 2, S)
            .add(MC + "paper", 2, 8, 2, S)
            .add(G + "coin", 4, 16, 3, S)
            .one(MC + "name_tag", 1, S)
            .one("BiblioCraft:item.BiblioGlasses#1", 1, S)
            .build();
    }

    /** @return the brig's evidence locker: confiscated knives, coins and contraband */
    public static LootPools.Pool evidence() {
        return new B(EVIDENCE).one(G + "combat_knife", 3, LV)
            .one(G + "crowbar", 2, LV)
            .one(MC + "iron_sword", 1, S)
            .one(MC + "stone_sword", 1, S)
            .one(MC + "shears", 1, S)
            .add(MC + "lead", 1, 3, 2, S)
            .add(G + "coin", 4, 16, 3, S)
            .add(MC + "gold_nugget", 2, 8, 2, S)
            .add("gt:ingot.Gold", 1, 2, 1, S)
            .one("gt:gem.Diamond", 1, LV)
            .one(MC + "name_tag", 1, S)
            .one(MC + "book", 1, S)
            .add(HC + "chocolatebarItem", 1, 4, 1, S)
            .build();
    }

    /** @return rover and cart parts */
    public static LootPools.Pool vehicle() {
        return new B(VEHICLE).one(GCC + "buggymat", 2, HV)
            .one(GCC + "buggymat#1", 1, HV)
            .one(GCC + "buggymat#2", 1, HV)
            .one(MC + "minecart", 1, S)
            .one(MC + "saddle", 1, S)
            .add(MC + "lead", 1, 2, 1, S)
            .add("gt:plate.Rubber", 2, 8, 2, LV)
            .build();
    }

    /** @return the range's target stock, bows and arrows */
    public static LootPools.Pool targets() {
        return new B(TARGETS).add(MC + "wool#14", 2, 8, 3, S)
            .add(MC + "hay_block", 1, 4, 2, S)
            .one(MC + "bow", 1, S)
            .add(MC + "arrow", 8, 24, 2, S)
            .build();
    }

    /** @return a ruin's junk (abandoned and mothballed rooms only); its own category so a pack can retune it */
    public static LootPools.Pool junk() {
        return new B(JUNK).add(MC + "rotten_flesh", 1, 6, 5, S)
            .add(SCRAP, 1, 6, 4, LV)
            .add(MC + "bone", 1, 4, 3, S)
            .add(MC + "gravel", 1, 8, 3, S)
            .add(MC + "cobblestone", 1, 8, 2, S)
            .add(MC + "string", 1, 4, 2, S)
            .add(MC + "web", 1, 2, 2, S)
            .add(MC + "stick", 1, 6, 2, S)
            .add(MC + "paper", 1, 4, 1, S)
            .add(MC + "glass_bottle", 1, 3, 2, S)
            .add(MC + "bowl", 1, 3, 1, S)
            .add("gt:dustSmall.Ash", 1, 6, 2, S)
            .add("gt:nugget.Iron", 1, 4, 1, S)
            .add("gt:bolt.Iron", 1, 6, 1, S)
            .add(MC + "feather", 1, 3, 1, S)
            .add(MC + "leather", 1, 2, 1, S)
            .add(MC + "spider_eye", 1, 2, 1, S)
            .build();
    }

    /**
     * What a line becomes when it spoils (D317), by the pool it came from: food to rotten flesh (stews and soups to a
     * bowl), drinks to an empty bottle, circuits and boards to scrap, batteries to a discharged one, a whole missile
     * body to a salvage body. Preserved food, mushrooms, metals, ammo, guns, parts and armor never spoil.
     *
     * @param pool the line's pool
     * @param ref  the line's reference
     * @return the spoil reference, or null when the line never spoils
     */
    public static @Nullable String spoilOf(String pool, String ref) {
        if (ref.startsWith(G + "missile_body_t")) {
            return ref + "#1"; // ItemBlockMissileBody.SALVAGE_DAMAGE
        }
        if (pool == null) {
            return null;
        }
        switch (pool) {
            case MEALS:
            case SNACKS:
            case PRODUCE:
            case INGREDIENTS:
            case RAWMEAT:
                return ref.contains("soup") || ref.contains("stew") ? BOWL : ROTTEN;
            case DRINKS:
                return ref.endsWith("milk_bucket") ? MC + "bucket" : BOTTLE;
            case ELECTRONICS:
            case CONTROL:
            case POWER:
                if (ref.equals(GT1 + "32518") || ref.equals(GT1 + "32528") || ref.equals(GT1 + "32538")) {
                    return DEAD_BATTERY;
                }
                if (ref.startsWith("IC2:itemPartCircuit") || ref.equals(GT3 + "32078")
                    || ref.equals(GT1 + "32702")
                    || ref.equals(GT1 + "32704")
                    || ref.equals(GT1 + "32710")
                    || ref.equals(GT3 + "32007")) {
                    return SCRAP;
                }
                return null;
            default:
                return null;
        }
    }

    /** @return whether a reference is a whole missile body (it always spoils to a salvage body in a gutted room) */
    public static boolean isBody(String ref) {
        return ref.startsWith(G + "missile_body_t") && ref.indexOf('#') < 0;
    }
}
