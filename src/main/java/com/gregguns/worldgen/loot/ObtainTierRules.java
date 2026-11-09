package com.gregguns.worldgen.loot;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * The pure half of {@link ObtainTier}: what GT tier a player needs to make an item at all, from string facts only
 * (a circuit's ore name, a GT prefix and material, a GT item list name, a registry name). Every answer is a GT voltage
 * index (0 ULV/steam, 1 LV, ... 12 UMV) or {@link #UNKNOWN}, which no ceiling admits, so an item nobody vouched for
 * never enters world loot (D363).
 *
 * <p>
 * The tier is the whole crafting chain: the tier at which a player can first make the item, never the voltage of the
 * recipe that happens to output it (a Fluid Solidifier casts a White Dwarf Matter nugget at a low voltage, but the
 * matter itself is endgame). Materials therefore come from a curated table, not from a blast furnace temperature:
 * many late materials (White Dwarf Matter, the exotic alloys) need no blast furnace at all. A material not in the
 * table is unknown. When the table is unsure it rounds up.
 */
// Spec: docs/gregguns-spec/11-worldgen/structure-loot.md (Loot tier cap); D363
public final class ObtainTierRules {

    /** No known tier: above every ceiling. */
    public static final int UNKNOWN = Integer.MAX_VALUE;

    /** GT's voltage names by index ({@code GTValues.VN}), upper-cased for lookup. */
    private static final String[] VOLTAGES = { "ULV", "LV", "MV", "HV", "EV", "IV", "LUV", "ZPM", "UV", "UHV", "UEV",
        "UIV", "UMV", "UXV", "MAX" };

    /**
     * GT's older circuit material names by tier (Primitive is ULV ... Transcendent is MAX); the ore names
     * {@code circuitBasic} and so on use them alongside {@code circuitLV}.
     */
    // Source: the circuit materials ULV to MAX are named Primitive, Basic, Good, Advanced, Data, Elite, Master,
    // Ultimate, Superconductor, Infinite, Bio, Optical, Exotic, Cosmic, Transcendent:
    // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/materials/MaterialsInit.java#L4494-L4640
    private static final String[] CIRCUIT_NAMES = { "PRIMITIVE", "BASIC", "GOOD", "ADVANCED", "DATA", "ELITE", "MASTER",
        "ULTIMATE", "SUPERCONDUCTOR", "INFINITE", "BIO", "OPTICAL", "EXOTIC", "COSMIC", "TRANSCENDENT" };

    private static final Map<String, Integer> MATERIALS = new HashMap<>();
    private static final Map<String, Integer> PREFIXES = new HashMap<>();
    private static final Map<String, Integer> ITEM_LIST = new HashMap<>();
    private static final Map<String, Integer> VANILLA_EXCEPTIONS = new HashMap<>();
    private static final Map<String, Integer> ALLOWED = new HashMap<>();

    static {
        // Steam age (ULV): ores, vanilla stuff and the alloys a steam player makes (Steel in the bricked blast
        // furnace).
        materials(
            0,
            "Iron",
            "WroughtIron",
            "PigIron",
            "Copper",
            "Tin",
            "Gold",
            "Silver",
            "Lead",
            "Zinc",
            "Nickel",
            "Bronze",
            "Brass",
            "Steel",
            "Invar",
            "Cupronickel",
            "Electrum",
            "Potin",
            "RedAlloy",
            "Coal",
            "Charcoal",
            "Lignite",
            "Diamond",
            "Emerald",
            "Ruby",
            "Sapphire",
            "GreenSapphire",
            "Lapis",
            "Lazurite",
            "Sodalite",
            "Redstone",
            "Glowstone",
            "NetherQuartz",
            "CertusQuartz",
            "Quartzite",
            "Obsidian",
            "Stone",
            "Clay",
            "Flint",
            "Wood",
            "Paper",
            "Glass",
            "Sulfur",
            "Saltpeter",
            "Salt",
            "RockSalt",
            "Ash",
            "DarkAsh",
            "Gunpowder",
            "Bone",
            "Sugar",
            "Wheat",
            "Apatite",
            "Rubber",
            "RawRubber",
            "Empty",
            "Water",
            "Lava",
            "Creosote",
            "Leather",
            "Meat",
            "Cocoa",
            "Blaze",
            "EnderPearl",
            "Netherrack",
            "Endstone",
            "Brick",
            "Fireclay",
            "Antimony",
            "Bismuth",
            "Cobalt",
            "Magnetite",
            "BandedIron",
            "BrownLimonite",
            "YellowLimonite",
            "Pyrite",
            "Chalcopyrite",
            "Cassiterite",
            "Galena",
            "Sphalerite",
            "Malachite",
            "Tetrahedrite",
            "Stibnite",
            "Garnierite",
            "Pentlandite",
            "Calcite",
            "CastIron",
            "Coke");
        // LV: what a basic chemical reactor, distillery, wiremill and bender make.
        materials(
            1,
            "AnnealedCopper",
            "BatteryAlloy",
            "SolderingAlloy",
            "Lubricant",
            "Plastic",
            "Polyethylene",
            "Glue",
            "SulfuricAcid",
            "Ethanol",
            "Oil",
            "Biomass",
            "Fuel",
            "Hydrogen",
            "Oxygen",
            "Nitrogen",
            "Methane",
            "Carbon",
            "Phosphorus",
            "Sodium",
            "Potassium",
            "Calcium",
            "Magnesium",
            "Manganese",
            "SteelMagnetic",
            "IronMagnetic",
            "Seedoil",
            "FishOil",
            "Glycerol",
            "Gallium",
            "Arsenic",
            "RedSteel",
            "BlackBronze",
            "BismuthBronze",
            "TinAlloy",
            "Mercury");
        // MV: the electric blast furnace's first metals, PTFE, silicon.
        materials(
            2,
            "Aluminium",
            "Kanthal",
            "Magnalium",
            "Silicon",
            "Polytetrafluoroethylene",
            "GalliumArsenide",
            "BlackSteel",
            "Graphite",
            "Chlorine",
            "HydrochloricAcid",
            "NitricAcid",
            "Thorium",
            "Beryllium",
            "Lithium",
            "Polycaprolactam",
            "Electrotine",
            "DamascusSteel",
            "Acetone");
        // HV: stainless steel and titanium (the HV and EV hull metals), chrome, epoxy.
        materials(
            3,
            "StainlessSteel",
            "Titanium",
            "Chrome",
            "Nichrome",
            "VanadiumSteel",
            "BlueSteel",
            "Epoxid",
            "EpoxidFiberReinforced",
            "Uranium",
            "Uranium238",
            "Vanadium",
            "Neodymium",
            "NeodymiumMagnetic",
            "SiliconSolarGrade",
            "StyreneButadieneRubber",
            "Silicone",
            "Polystyrene",
            "Polydimethylsiloxane",
            // Source: the Haber step runs at 384 EU/t, past MV's 128:
            // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/postload/recipes/ChemicalRecipes.java#L4356
            "Ammonia");
        // EV: tungsten steel and the platinum group's first metals.
        materials(
            4,
            "TungstenSteel",
            "Tungsten",
            "TungstenCarbide",
            "Platinum",
            "Palladium",
            "Plutonium244",
            "Ultimet",
            "Graphene",
            "PolyphenyleneSulfide");
        // IV and up: never in default loot; listed so the check names them instead of calling them unknown.
        materials(5, "Iridium", "Osmium", "Osmiridium", "HSSG", "Naquadah", "Polybenzimidazole");
        materials(6, "HSSE", "HSSS", "NaquadahEnriched", "Europium", "Rhodium", "Ruthenium");
        // Bartworks' Rhodium-Plated Palladium, the LuV casing metal, by its ore dictionary spelling.
        materials(6, "Rhodium-PlatedPalladium", "RhodiumPlatedPalladium");
        // Fissile metals and naquadah alloy follow the questbook's reward tiers (tools/questbook/item_tiers.json).
        materials(7, "Naquadria", "Trinium", "Americium", "Duranium", "NaquadahAlloy", "Uranium235", "Plutonium");
        materials(8, "Neutronium", "Tritanium", "Plutonium241");
        materials(9, "Infinity", "CosmicNeutronium");
        // Transcendent metal is the UIV hull metal, as the silo tables and their spec sheet have it.
        materials(11, "TranscendentMetal");
        materials(
            12,
            "WhiteDwarfMatter",
            "BlackDwarfMatter",
            "Universium",
            "MagnetohydrodynamicallyConstrainedStarMatter",
            "SpaceTime");

        // GT prefixes a material item may have, with the tier the form itself needs (a fine wire needs a wiremill).
        // A prefix not listed (hot ingots, plasma cells, nanites, ...) is unknown.
        prefixes(
            0,
            "ingot",
            "nugget",
            "dust",
            "dustSmall",
            "dustTiny",
            "dustImpure",
            "dustPure",
            "crushed",
            "crushedPurified",
            "gem",
            "gemChipped",
            "gemFlawed",
            "block",
            "plate",
            "plateDouble",
            "stick",
            "bolt",
            "screw",
            "ring",
            "round",
            "gearGt",
            "gearGtSmall",
            "rotor",
            "spring",
            "springSmall",
            "frameGt",
            "pipeTiny",
            "pipeSmall",
            "pipeMedium",
            "pipeLarge",
            "pipeHuge",
            "wireGt01",
            "wireGt02",
            "wireGt04",
            "wireGt08",
            "wireGt12",
            "wireGt16",
            "cableGt01",
            "cableGt02",
            "cableGt04",
            "cableGt08",
            "cableGt12",
            "cableGt16",
            "cell",
            "toolHeadSword",
            "toolHeadPickaxe",
            "toolHeadShovel",
            "toolHeadAxe",
            "toolHeadHoe",
            "toolHeadHammer",
            "toolHeadFile",
            "toolHeadSaw",
            "ore");
        prefixes(
            1,
            "wireFine",
            "foil",
            "stickLong",
            "plateTriple",
            "plateQuadruple",
            "plateQuintuple",
            "plateDense",
            "crushedCentrifuged",
            "gemFlawless",
            "gemExquisite",
            "lens");

        // GT item list names with no tier in the name. A name with a voltage token (Electric_Motor_EV,
        // Battery_RE_HV_Lithium) takes that token's tier.
        ITEM_LIST.put("Tool_DataStick", 3);
        ITEM_LIST.put("Tool_DataOrb", 5);
        ITEM_LIST.put("Circuit_Board_Basic", 0);
        ITEM_LIST.put("Circuit_Board_Coated", 0);
        ITEM_LIST.put("Circuit_Board_Phenolic", 1);
        ITEM_LIST.put("Circuit_Board_Plastic", 2);
        ITEM_LIST.put("Circuit_Board_Epoxy", 3);
        ITEM_LIST.put("Circuit_Parts_Resistor", 1);
        ITEM_LIST.put("Circuit_Parts_Diode", 1);
        ITEM_LIST.put("Circuit_Parts_Transistor", 2);
        ITEM_LIST.put("Circuit_Parts_Capacitor", 2);
        ITEM_LIST.put("Circuit_Silicon_Ingot", 2);
        ITEM_LIST.put("Circuit_Silicon_Wafer", 2);
        ITEM_LIST.put("Cell_Empty", 0);
        ITEM_LIST.put("Large_Fluid_Cell_Steel", 1);
        ITEM_LIST.put("Large_Fluid_Cell_Aluminium", 2);
        ITEM_LIST.put("Large_Fluid_Cell_StainlessSteel", 3);
        ITEM_LIST.put("Large_Fluid_Cell_Titanium", 4);
        ITEM_LIST.put("Spray_Empty", 1);
        ITEM_LIST.put("Shape_Empty", 1);
        ITEM_LIST.put("Duct_Tape", 1);
        ITEM_LIST.put("Component_Filter", 1);
        ITEM_LIST.put("Ingot_IridiumAlloy", 5);
        ITEM_LIST.put("Energy_LapotronicOrb", 5);
        ITEM_LIST.put("QuantumEye", 6);
        ITEM_LIST.put("QuantumStar", 7);

        // Vanilla items a player cannot make at all.
        VANILLA_EXCEPTIONS.put("minecraft:command_block", UNKNOWN);
        VANILLA_EXCEPTIONS.put("minecraft:mob_spawner", UNKNOWN);
        VANILLA_EXCEPTIONS.put("minecraft:bedrock", UNKNOWN);
        VANILLA_EXCEPTIONS.put("minecraft:end_portal_frame", UNKNOWN);
        VANILLA_EXCEPTIONS.put("minecraft:dragon_egg", UNKNOWN);
        VANILLA_EXCEPTIONS.put("minecraft:spawn_egg", UNKNOWN);
        VANILLA_EXCEPTIONS.put("minecraft:nether_star", 3);

        // Other mods' items our pools name, by registry name (and @meta where it matters), at their own tier.
        // Everything else from another mod is unknown.
        allowed(
            0,
            "IC2:itemCellEmpty",
            "IC2:itemTreetap",
            "IC2:itemTinCanFilled",
            "IC2:itemTinCan",
            "IC2:itemDynamite",
            "IC2:blockITNT",
            "IC2:itemRubber",
            "Railcraft:fuel.coke",
            "Railcraft:tool.crowbar",
            "BiblioCraft:item.BiblioGlasses",
            "BiblioCraft:item.BiblioClipboard",
            "TwilightForest:item.charmOfKeeping1",
            "Thaumcraft:ItemLootBag",
            "Thaumcraft:ItemBowBone",
            "Thaumcraft:ItemBaubleBlanks",
            "TConstruct:fletching",
            "SpiceOfLife:lunchbag",
            "Natura:natura.bow.darkwood",
            "Backpack:tannedLeather",
            "Backpack:backpack",
            "harvestcraft:*");
        // Tinkers' iron tool parts (meta 2) come off a Part Builder or a smeltery cast, with no voltage.
        for (String part : new String[] { "toolRod", "binding", "hatchetHead", "shovelHead", "swordBlade", "knifeBlade",
            "arrowhead" }) {
            allowed(0, "TConstruct:" + part + "@2");
        }
        allowed(
            1,
            "IC2:itemScrap",
            "IC2:itemScrapbox",
            "IC2:itemFreq",
            "IC2:itemBatREDischarged",
            "IC2:itemBatRE",
            "IC2:itemArmorRubBoots",
            "IC2:itemArmorHazmatHelmet",
            "IC2:itemArmorHazmatChestplate",
            "IC2:itemArmorHazmatLeggings",
            "GalacticraftCore:item.standardWrench",
            "GalacticraftCore:item.canister@0",
            "GalacticraftCore:item.canister@1");
        // Galacticraft in the pack: the Tier 1 rocket's parts and the oxygen gear are HV, the Tier 2 rocket's EV and
        // the heavy engine and fins of the Tier 3 and 4 rockets IV (see GalacticraftParts).
        allowed(
            3,
            "GalacticraftCore:item.basicItem@9",
            "GalacticraftCore:item.heavyPlating",
            "GalacticraftCore:item.rocketFins",
            "GalacticraftCore:item.engine",
            "GalacticraftCore:item.noseCone",
            "GalacticraftCore:item.airFan",
            "GalacticraftCore:item.airVent",
            "GalacticraftCore:item.oxygenMask",
            "GalacticraftCore:item.oxygenTankLightFull",
            "GalacticraftCore:item.buggymat");
        allowed(4, "GalacticraftMars:item.itemBasicAsteroids@6");
        allowed(5, "GalacticraftMars:item.itemBasicAsteroids@1", "GalacticraftMars:item.itemBasicAsteroids@2");
    }

    private ObtainTierRules() {}

    private static void materials(int tier, String... names) {
        for (String n : names) MATERIALS.put(n, tier);
    }

    private static void prefixes(int tier, String... names) {
        for (String n : names) PREFIXES.put(n, tier);
    }

    private static void allowed(int tier, String... names) {
        for (String n : names) ALLOWED.put(n, tier);
    }

    /**
     * @param name a GT voltage name ("LV", "LuV", "UMV"; any case)
     * @return its index, or {@link #UNKNOWN}
     */
    public static int voltage(String name) {
        String n = name.toUpperCase(Locale.ROOT);
        for (int i = 0; i < VOLTAGES.length; i++) {
            if (VOLTAGES[i].equals(n)) return i;
        }
        return UNKNOWN;
    }

    /**
     * @param oreName an ore dictionary name
     * @return the circuit tier for {@code circuit<Voltage>} or {@code circuit<OldName>} ({@code circuitLV},
     *         {@code circuitBasic}), or {@link #UNKNOWN} when the name is no circuit name
     */
    public static int circuit(String oreName) {
        if (!oreName.startsWith("circuit") || oreName.length() == "circuit".length()) return UNKNOWN;
        String suffix = oreName.substring("circuit".length());
        int v = voltage(suffix);
        if (v != UNKNOWN) return v;
        String n = suffix.toUpperCase(Locale.ROOT);
        for (int i = 0; i < CIRCUIT_NAMES.length; i++) {
            if (CIRCUIT_NAMES[i].equals(n)) return i;
        }
        return UNKNOWN;
    }

    /**
     * @param material a GT material name ({@code Materials.mName})
     * @return the material's tier, or {@link #UNKNOWN} when the table does not list it
     */
    public static int material(String material) {
        Integer t = MATERIALS.get(material);
        return t == null ? UNKNOWN : t;
    }

    /**
     * @param prefix   a GT ore prefix name ({@code OrePrefixes.name()})
     * @param material a GT material name
     * @return the higher of the form's and the material's tier, {@link #UNKNOWN} when either is unknown; a circuit
     *         prefix takes the circuit tier of the material name
     */
    public static int materialItem(String prefix, String material) {
        if ("circuit".equals(prefix)) return circuit("circuit" + material);
        Integer form = PREFIXES.get(prefix);
        if (form == null) return UNKNOWN;
        return Math.max(form, material(material));
    }

    /**
     * @param oreName an ore dictionary name ({@code ingotSteel}, {@code plateRhodium-PlatedPalladium})
     * @return the tier of the longest known prefix with a known material after it, or {@link #UNKNOWN}; for another
     *         mod's copy of a GT material item, which GT did not unify
     */
    public static int oreName(String oreName) {
        String best = null;
        for (String prefix : PREFIXES.keySet()) {
            if (oreName.length() > prefix.length() && oreName.startsWith(prefix)
                && Character.isUpperCase(oreName.charAt(prefix.length()))
                && (best == null || prefix.length() > best.length())) {
                best = prefix;
            }
        }
        return best == null ? UNKNOWN : materialItem(best, oreName.substring(best.length()));
    }

    /**
     * @param name a GT {@code ItemList} constant name
     * @return its tier: the curated table first, else the highest voltage token in the name ({@code Electric_Motor_EV}
     *         is EV), else {@link #UNKNOWN}
     */
    public static int itemList(String name) {
        if (neverLoot(name)) return UNKNOWN;
        Integer t = ITEM_LIST.get(name);
        if (t != null) return t;
        int best = UNKNOWN;
        for (String token : name.split("_")) {
            int v = voltage(token);
            if (v != UNKNOWN && (best == UNKNOWN || v > best)) best = v;
        }
        return best;
    }

    /**
     * @param name a GT {@code ItemList} constant name
     * @return true for an entry no player can make even though it carries a circuit ore name: GT's ghost circuits
     *         ({@code Circuit_BasicElectronic} under {@code circuitBasic}, {@code Circuit_Integrated_Advanced} under
     *         {@code circuitAdvanced}), which are never loot whatever their ore name says
     */
    public static boolean neverLoot(String name) {
        return "Circuit_BasicElectronic".equals(name) || "Circuit_Integrated_Advanced".equals(name);
    }

    /**
     * @param registryName a vanilla item's registry name ({@code minecraft:...})
     * @return 0, or the exception's tier for an item a player cannot make
     */
    public static int vanilla(String registryName) {
        Integer t = VANILLA_EXCEPTIONS.get(registryName);
        return t == null ? 0 : t;
    }

    /**
     * @param registryName another mod's item registry name
     * @param meta         its damage
     * @return its allow-listed tier ({@code name@meta} first, then {@code name}, then {@code modid:*}), or
     *         {@link #UNKNOWN}
     */
    public static int allowed(String registryName, int meta) {
        Integer t = ALLOWED.get(registryName + "@" + meta);
        if (t == null) t = ALLOWED.get(registryName);
        if (t == null) {
            int colon = registryName.indexOf(':');
            if (colon > 0) t = ALLOWED.get(registryName.substring(0, colon) + ":*");
        }
        return t == null ? UNKNOWN : t;
    }

    /**
     * @param tier    an obtain tier
     * @param ceiling a source's ceiling (voltage index)
     * @param maxTier the {@code loot.maxTier} key
     * @return whether the item may appear: its tier is known and at most the lower of the two
     */
    public static boolean allowed(int tier, int ceiling, int maxTier) {
        return tier != UNKNOWN && tier <= Math.min(ceiling, maxTier);
    }

    /** @return the curated material table, for the docs check */
    public static Map<String, Integer> materials() {
        return Collections.unmodifiableMap(MATERIALS);
    }

    /**
     * @param tier a voltage index or {@link #UNKNOWN}
     * @return its GT name, or "unknown"
     */
    public static String name(int tier) {
        if (tier == UNKNOWN || tier < 0 || tier >= VOLTAGES.length) return "unknown";
        return tier == 6 ? "LuV" : VOLTAGES[tier];
    }
}
