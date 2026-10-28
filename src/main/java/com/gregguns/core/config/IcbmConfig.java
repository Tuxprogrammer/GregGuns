package com.gregguns.core.config;

import com.gtnewhorizon.gtnhlib.config.Config;

/**
 * ICBM blast switches, the {@code icbm} category of {@code config/gregguns.cfg}. Fields only; no Minecraft types.
 * Synced, except the silo complex keys (server-only, each marked): the server's values decide what a blast does.
 * Terrain breaks by the per-tier key here only;
 * {@code explosions.blockDamage}
 * governs hand-held weapons, not ICBM blasts (D11, D52). Claims and protection always apply.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/explosive-registry.md (config); DECISIONS.md D11
@Config(modid = "gregguns", category = "icbm")
@Config.Sync
public class IcbmConfig {

    @Config.Comment({ "The highest explosive tier that may detonate (1 to 5); a warhead above it does nothing.",
        "Default: 5." })
    @Config.RangeInt(min = 1, max = 5)
    public static int maxBlastTier = 5;

    @Config.Comment({ "Whether tier 1 (HV) blasts may remove terrain.", "Default: true." })
    public static boolean terrainDamageT1 = true;

    @Config.Comment({ "Whether tier 2 (EV to IV) blasts may remove terrain.", "Default: true." })
    public static boolean terrainDamageT2 = true;

    @Config.Comment({ "Whether tier 3 (LuV to ZPM) blasts may remove terrain.", "Default: true." })
    public static boolean terrainDamageT3 = true;

    @Config.Comment({ "Whether tier 4 and 5 (UV antimatter, UHV red matter) blasts may remove terrain. Off: red",
        "matter still pulls and annihilates but has nothing to eat, so it idles and ends.", "Default: true." })
    public static boolean terrainDamageT4 = true;

    @Config.Comment({ "How long a red matter body lives, in seconds, at most (D10: a bounded lifetime).",
        "Range 5 to 600. Default: 60." })
    @Config.RangeInt(min = 5, max = 600)
    public static int redMatterLifetimeSeconds = 60;

    @Config.Comment({ "Milliseconds per server tick that all running blasts share for breaking blocks.",
        "Range 1 to 40. Default: 12." })
    @Config.RangeInt(min = 1, max = 40)
    public static int blastBudgetMs = 12;

    @Config.Comment({ "Most blasts that may run at once; a detonation past this is refused. Range 1 to 256.",
        "Default: 16." })
    @Config.RangeInt(min = 1, max = 256)
    public static int maxActiveBlasts = 16;

    @Config.Comment({ "Largest blast radius in blocks; bigger ones are clamped. The ray cast copies a cube of",
        "blocks this wide, so memory grows with the cube (96 is about 21 MB; see maxGridMemoryMb). Range 8 to 200. Default: 96." })
    @Config.RangeInt(min = 8, max = 200)
    public static int maxBlastRadius = 96;

    @Config.Comment({ "Total megabytes the ray-cast cubes of all running blasts may hold. A blast that would go past",
        "this waits in the queue until others finish casting (a lone blast always runs). Range 16 to 4096.",
        "Default: 256." })
    @Config.RangeInt(min = 16, max = 4096)
    public static int maxGridMemoryMb = 256;

    @Config.Comment({ "Whether blocks destroyed by a blast drop items. Off avoids thousands of item entities.",
        "Default: false." })
    public static boolean blastDrops = false;

    @Config.Comment({
        "Multiplier on a hand-lit or redstone-lit warhead's fuse. The base fuse is 3 seconds plus 1 second",
        "per 3 blocks of the warhead's blast radius, rounded up (a radius 9 warhead: 6 s). A warhead set off by",
        "another explosion keeps a fixed 10 tick fuse. Range 0.1 to 10. Default: 1.0." })
    @Config.RangeDouble(min = 0.1, max = 10.0)
    public static double fuseMultiplier = 1.0;

    @Config.Comment({ "Most missiles that may be in flight at once, server wide (D11). Each holds at most two chunks,",
        "so the default of 20 stays far inside Forge's per-mod ticket budget. Range 1 to 100. Default: 20." })
    @Config.RangeInt(min = 1, max = 100)
    public static int maxMissilesInFlight = 20;

    @Config.Comment({ "Multiplier on every missile tier's fuel tank (D20). Range 0.1 to 10. Default: 1.0." })
    @Config.RangeDouble(min = 0.1, max = 10.0)
    public static double missileFuelFactor = 1.0;

    @Config.Comment({ "Multiplier on every missile tier's range. Range 0.1 to 10. Default: 1.0." })
    @Config.RangeDouble(min = 0.1, max = 10.0)
    public static double missileRangeFactor = 1.0;

    @Config.Comment({
        "Multiplier on the scatter of a missile launched from a Missile Silo (D70), on top of the guidance",
        "rule; the 10% floor of the base scatter still applies. Below 1 is more accurate. Range 0.1 to 2. Default: 0.5." })
    @Config.RangeDouble(min = 0.1, max = 2.0)
    public static double siloScatterFactor = 0.5;

    @Config.Comment({ "Multiplier on the range of a missile launched from a Missile Silo (D70), on top of",
        "icbm.missileRangeFactor. The open Galacticraft pad keeps the plain range. Range 0.5 to 5. Default: 1.25." })
    @Config.RangeDouble(min = 0.5, max = 5.0)
    public static double siloRangeFactor = 1.25;

    @Config.Comment({
        "The most the unburned fuel adds to a blast radius, as a fraction (D20): 0.25 is up to 25% bigger",
        "at point-blank range, and nothing at full range. 0 turns the bonus off. Range 0 to 1. Default: 0.25." })
    @Config.RangeDouble(min = 0.0, max = 1.0)
    public static double fuelBonusMax = 0.25;

    @Config.Comment({ "Ticks a missile may fly past its planned arrival before it detonates where it is (a safety net",
        "for a target that was never reached). Range 20 to 6000. Default: 600." })
    @Config.RangeInt(min = 20, max = 6000)
    public static int missileTimeoutTicks = 600;

    @Config.Comment({ "Ticks the Missile Silo doors take to swing open or shut (D89). Range 10 to 200. Default: 40." })
    @Config.RangeInt(min = 10, max = 200)
    public static int doorOpenTicks = 40;

    @Config.Comment({ "Ticks of countdown between the doors being open (or the launch on a pad) and ignition (D88).",
        "The launch can be aborted until it ends. Range 20 to 1200. Default: 200." })
    @Config.RangeInt(min = 20, max = 1200)
    public static int countdownTicks = 200;

    @Config.Comment({ "Blocks the missile's bottom must climb above the silo rim before the doors swing shut (D89).",
        "Range 1 to 32. Default: 4." })
    @Config.RangeInt(min = 1, max = 32)
    public static int doorCloseHeight = 4;

    @Config.Comment({
        "Most devices (GT machines, battery buffers, hatches, IC2 storage) one EMP pulse may drain, nearest",
        "first (D16); a dense base cannot stall the server. Missiles are not counted. Range 1 to 1024. Default: 64." })
    @Config.RangeInt(min = 1, max = 1024)
    public static int empMaxDevices = 64;

    @Config.Comment({ "Largest radius an EMP Tower may be set to, blocks; the pulse costs 25 EU per square block of",
        "radius (D16). Range 8 to 256. Default: 100." })
    @Config.RangeInt(min = 8, max = 256)
    public static int empMaxRadius = 100;

    @Config.Comment({ "Chance an EMP pulse destroys a missile in flight outright (ICBM's 0.5). Range 0 to 1.",
        "Default: 0.5." })
    @Config.RangeDouble(min = 0.0, max = 1.0)
    public static double empMissileKillChance = 0.5;

    @Config.Comment({ "Chance, among the survivors, that an EMP pulse fries a missile's guidance so it falls",
        "(ICBM's 0.8). Range 0 to 1. Default: 0.8." })
    @Config.RangeDouble(min = 0.0, max = 1.0)
    public static double empMissileGuidanceChance = 0.8;

    @Config.Comment({ "Chance, among the rest, that an EMP pulse sets a missile's warhead off where it is",
        "(ICBM's 0.1). Range 0 to 1. Default: 0.1." })
    @Config.RangeDouble(min = 0.0, max = 1.0)
    public static double empMissileTriggerChance = 0.1;

    @Config.Comment({ "Most entities one ender warhead teleports (D80); the rest are only pulled.",
        "Range 1 to 1024. Default: 64." })
    @Config.RangeInt(min = 1, max = 1024)
    public static int enderMaxTeleports = 64;

    @Config.Comment({ "Most blocks one anti-gravitational warhead lifts into flying blocks (D80), top layer first.",
        "Range 0 to 4096. Default: 512." })
    @Config.RangeInt(min = 0, max = 4096)
    public static int antigravMaxBlocks = 512;

    /**
     * @param tier the explosive tier number, 1 to 5
     * @return the terrain switch for that tier
     */
    public static boolean terrainDamage(int tier) {
        switch (tier) {
            case 1:
                return terrainDamageT1;
            case 2:
                return terrainDamageT2;
            case 3:
                return terrainDamageT3;
            default:
                return terrainDamageT4;
        }
    }

    @Config.Comment({ "Whether silo complexes (D128) generate at all. They also need modules.worldgen on and",
        "worldgen.spawnStructures. Default: true." })
    @Config.Sync(false)
    public static boolean siloComplexEnabled = true;

    @Config.Comment({ "Dimensions that get silo complexes (D38), by key: \"Overworld\", or a Galacticraft planet's own",
        "unlocalized name such as moon, iojupiter, ganymed or barnarda2. Remove a name to keep new complexes out of",
        "that planet. A name outside the D38 table gets no complexes (the log says so once). Default: the Overworld",
        "and every planet of the D38 table except T Ceti E, an ocean world (D239)." })
    @Config.Sync(false)
    public static String[] siloComplexDimensions = { "Overworld", "moon", "mars", "phobos", "deimos", "ceres",
        "callisto", "europa", "ganymed", "iojupiter", "mercury", "venus", "enceladus", "titan", "miranda", "oberon",
        "proteus", "triton", "pluto", "haumea", "makemake", "barnarda2", "barnarda4", "barnarda5", "centauribb",
        "vega1", "anubis", "horus", "maahes", "neper", "seth", "ross128b", "ross128ba" };

    @Config.Comment({ "Nine weights, EV first to UMV last, for the loot ceiling a complex on Ross128b or Ross128ba",
        "rolls (D38). A complex rolls once from its own seed; negative weights count as 0. Default: 30, 22, 16, 11,",
        "8, 6, 4, 2, 1, so about half roll EV to IV." })
    @Config.Sync(false)
    public static int[] siloComplexRoss128Weights = { 30, 22, 16, 11, 8, 6, 4, 2, 1 };

    @Config.Comment({ "Share of the sparse grid's cells (one per 64 x 64 chunks) that attempt a complex, 0 to 100.",
        "Default: 25." })
    @Config.RangeInt(min = 0, max = 100)
    @Config.Sync(false)
    public static int siloComplexChancePercent = 25;

    @Config.Comment({ "Overworld biome types (BiomeDictionary names) that may host a complex; blank allows all.",
        "Default: dry and temperate land types." })
    @Config.Sync(false)
    public static String[] siloComplexOverworldBiomes = { "PLAINS", "FOREST", "SANDY", "SAVANNA", "CONIFEROUS", "SNOWY",
        "MESA", "DRY" };

    @Config.Comment({ "Relative weights of the activity levels a complex rolls from its seed (D36), in the order",
        "abandoned, partly abandoned, operational. Three whole numbers, 0 or more; other lists keep the default.",
        "Default: 50, 35, 15." })
    @Config.Sync(false)
    public static int[] siloComplexActivityWeights = { 50, 35, 15 };
}
