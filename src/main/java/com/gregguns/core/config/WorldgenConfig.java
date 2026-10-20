package com.gregguns.core.config;

import com.gtnewhorizon.gtnhlib.config.Config;

/**
 * Structure placement, gating and loot tuning (M8), the {@code worldgen} category of {@code config/gregguns.cfg}.
 * Fields only; no Minecraft types. Read on the server, none is synced. Nothing here is read unless
 * {@code modules.worldgen} is on. The gates that use these fields are pure functions in
 * {@code com.gregguns.worldgen.WorldgenGate}; {@code WorldgenSettings} reads the per-structure fields.
 */
// Spec: docs/gregguns-spec/11-worldgen/structure-placement.md (GGS-11-07, GGS-11-12, GGS-11-15),
// docs/gregguns-spec/11-worldgen/structure-loot.md (GGS-11-23)
@Config(modid = "gregguns", category = "worldgen")
public class WorldgenConfig {

    @Config.Comment({ "Place natural structures at all. When false the structure generator is not registered.",
        "Only applies while modules.worldgen is on. Default: true." })
    public static boolean spawnStructures = true;

    @Config.Comment({ "Grid cell size in chunks for SMALL structures (one seeded attempt per cell). Default: 16." })
    @Config.RangeInt(min = 2, max = 1024)
    public static int spacingSmall = 16;

    @Config.Comment({ "Grid cell size in chunks for MEDIUM structures. Default: 32." })
    @Config.RangeInt(min = 2, max = 1024)
    public static int spacingMedium = 32;

    @Config.Comment({ "Grid cell size in chunks for BIG structures (Military Base, ships). Default: 64." })
    @Config.RangeInt(min = 2, max = 1024)
    public static int spacingBig = 64;

    @Config.Comment({ "Dimension ids in which structures are placed. Other dimensions are skipped silently.",
        "Default: 0 (the Overworld)." })
    public static int[] structureDimensions = { 0 };

    @Config.Comment({ "World type names that allow structures (WorldType.getWorldTypeName, compared ignoring case).",
        "Flat and unknown world types are out unless listed. Default: default, RWG, largeBiomes, amplified." })
    public static String[] structureWorldTypes = { "default", "RWG", "largeBiomes", "amplified" };

    @Config.Comment({ "Place the Small Factory Building. Default: true." })
    public static boolean enabledFactorySmall = true;

    @Config.Comment({
        "BiomeDictionary type names (for example SNOWY) in which the Small Factory Building is never placed.",
        "Default: none." })
    public static String[] biomeBlacklistFactorySmall = {};

    @Config.Comment({
        "Loot tier ceiling of the Small Factory Building: 1 is LV, 2 is MV, 3 is HV. Picks the loot category.",
        "Default: 1." })
    @Config.RangeInt(min = 1, max = 3)
    public static int lootTierCeilingFactorySmall = 1;

    @Config.Comment({
        "Loot richness of the Small Factory Building, 0 poor to 2 rich. Scales counts and weights only, never the tier.",
        "Default: 1." })
    @Config.RangeInt(min = 0, max = 2)
    public static int lootTierFactorySmall = 1;

    @Config.Comment({ "Place the Small Train Station. Default: true." })
    public static boolean enabledTrainStation = true;

    @Config.Comment({
        "BiomeDictionary type names (for example SNOWY) in which the Small Train Station is never placed.",
        "Default: none." })
    public static String[] biomeBlacklistTrainStation = {};

    @Config.Comment({
        "Loot tier ceiling of the Small Train Station: 1 is LV, 2 is MV, 3 is HV. Picks the loot category.",
        "Default: 1." })
    @Config.RangeInt(min = 1, max = 3)
    public static int lootTierCeilingTrainStation = 1;

    @Config.Comment({
        "Loot richness of the Small Train Station, 0 poor to 2 rich. Scales counts and weights only, never the tier.",
        "Default: 1." })
    @Config.RangeInt(min = 0, max = 2)
    public static int lootTierTrainStation = 1;

    @Config.Comment({ "Place the Gas Station. Default: true." })
    public static boolean enabledGasStation = true;

    @Config.Comment({ "BiomeDictionary type names (for example SNOWY) in which the Gas Station is never placed.",
        "Default: none." })
    public static String[] biomeBlacklistGasStation = {};

    @Config.Comment({ "Loot tier ceiling of the Gas Station: 1 is LV, 2 is MV, 3 is HV. Picks the loot category.",
        "Default: 1." })
    @Config.RangeInt(min = 1, max = 3)
    public static int lootTierCeilingGasStation = 1;

    @Config.Comment({
        "Loot richness of the Gas Station, 0 poor to 2 rich. Scales counts and weights only, never the tier.",
        "Default: 1." })
    @Config.RangeInt(min = 0, max = 2)
    public static int lootTierGasStation = 1;

    @Config.Comment({ "Place the Medium House. Default: true." })
    public static boolean enabledHouseMedium = true;

    @Config.Comment({ "BiomeDictionary type names (for example SNOWY) in which the Medium House is never placed.",
        "Default: none." })
    public static String[] biomeBlacklistHouseMedium = {};

    @Config.Comment({ "Loot tier ceiling of the Medium House: 1 is LV, 2 is MV, 3 is HV. Picks the loot category.",
        "Default: 1." })
    @Config.RangeInt(min = 1, max = 3)
    public static int lootTierCeilingHouseMedium = 1;

    @Config.Comment({
        "Loot richness of the Medium House, 0 poor to 2 rich. Scales counts and weights only, never the tier.",
        "Default: 1." })
    @Config.RangeInt(min = 0, max = 2)
    public static int lootTierHouseMedium = 1;

    @Config.Comment({ "Place the Big Bunker. Default: true." })
    public static boolean enabledBigBunker = true;

    @Config.Comment({ "BiomeDictionary type names (for example SNOWY) in which the Big Bunker is never placed.",
        "Default: none." })
    public static String[] biomeBlacklistBigBunker = {};

    @Config.Comment({ "Loot tier ceiling of the Big Bunker: 1 is LV, 2 is MV, 3 is HV. Picks the loot category.",
        "Default: 2." })
    @Config.RangeInt(min = 1, max = 3)
    public static int lootTierCeilingBigBunker = 2;

    @Config.Comment({
        "Loot richness of the Big Bunker, 0 poor to 2 rich. Scales counts and weights only, never the tier.",
        "Default: 1." })
    @Config.RangeInt(min = 0, max = 2)
    public static int lootTierBigBunker = 1;

    @Config.Comment({ "Place the Alien Bug Nest. Default: false." })
    public static boolean enabledAlienBugNest = false;

    @Config.Comment({ "BiomeDictionary type names (for example SNOWY) in which the Alien Bug Nest is never placed.",
        "Default: none." })
    public static String[] biomeBlacklistAlienBugNest = {};

    @Config.Comment({ "Loot tier ceiling of the Alien Bug Nest: 1 is LV, 2 is MV, 3 is HV. Picks the loot category.",
        "Default: 1." })
    @Config.RangeInt(min = 1, max = 3)
    public static int lootTierCeilingAlienBugNest = 1;

    @Config.Comment({
        "Loot richness of the Alien Bug Nest, 0 poor to 2 rich. Scales counts and weights only, never the tier.",
        "Default: 1." })
    @Config.RangeInt(min = 0, max = 2)
    public static int lootTierAlienBugNest = 1;

    @Config.Comment({ "Place the Military Base. Default: true." })
    public static boolean enabledMilitaryBase = true;

    @Config.Comment({ "BiomeDictionary type names (for example SNOWY) in which the Military Base is never placed.",
        "Default: none." })
    public static String[] biomeBlacklistMilitaryBase = {};

    @Config.Comment({ "Loot tier ceiling of the Military Base: 1 is LV, 2 is MV, 3 is HV. Picks the loot category.",
        "Default: 2." })
    @Config.RangeInt(min = 1, max = 3)
    public static int lootTierCeilingMilitaryBase = 2;

    @Config.Comment({
        "Loot richness of the Military Base, 0 poor to 2 rich. Scales counts and weights only, never the tier.",
        "Default: 1." })
    @Config.RangeInt(min = 0, max = 2)
    public static int lootTierMilitaryBase = 1;

    @Config.Comment({ "Place the Aircraft Carrier. Default: true." })
    public static boolean enabledAircraftCarrier = true;

    @Config.Comment({ "BiomeDictionary type names (for example SNOWY) in which the Aircraft Carrier is never placed.",
        "Default: none." })
    public static String[] biomeBlacklistAircraftCarrier = {};

    @Config.Comment({ "Loot tier ceiling of the Aircraft Carrier: 1 is LV, 2 is MV, 3 is HV. Picks the loot category.",
        "Default: 2." })
    @Config.RangeInt(min = 1, max = 3)
    public static int lootTierCeilingAircraftCarrier = 2;

    @Config.Comment({
        "Loot richness of the Aircraft Carrier, 0 poor to 2 rich. Scales counts and weights only, never the tier.",
        "Default: 1." })
    @Config.RangeInt(min = 0, max = 2)
    public static int lootTierAircraftCarrier = 1;

    @Config.Comment({ "Place the Submarine. Default: true." })
    public static boolean enabledSubmarine = true;

    @Config.Comment({ "BiomeDictionary type names (for example SNOWY) in which the Submarine is never placed.",
        "Default: none." })
    public static String[] biomeBlacklistSubmarine = {};

    @Config.Comment({ "Loot tier ceiling of the Submarine: 1 is LV, 2 is MV, 3 is HV. Picks the loot category.",
        "Default: 2." })
    @Config.RangeInt(min = 1, max = 3)
    public static int lootTierCeilingSubmarine = 2;

    @Config.Comment({
        "Loot richness of the Submarine, 0 poor to 2 rich. Scales counts and weights only, never the tier.",
        "Default: 1." })
    @Config.RangeInt(min = 0, max = 2)
    public static int lootTierSubmarine = 1;
}
