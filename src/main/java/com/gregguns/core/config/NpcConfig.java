package com.gregguns.core.config;

import com.gtnewhorizon.gtnhlib.config.Config;

/**
 * NPC, mob and spawning tuning (M7), the {@code npcs} category of {@code config/gregguns.cfg}. Every field the spawn
 * gate, the caps and the NPC gun code read is declared here once, so the parallel M7 pieces never edit this file.
 * Fields only; no Minecraft types. Read on the server, none is synced.
 */
// Spec: docs/gregguns-spec/10-entities-npcs/spawning.md (GGS-10-23, GGS-10-24), docs/design/0005-world-presence.md
@Config(modid = "gregguns", category = "npcs")
public class NpcConfig {

    @Config.Comment({
        "Dimension ids that use the Overworld spawn table (zombies, Skeleton Soldiers, bandits, Psycho Steve).",
        "A dimension in no spawn list has no natural NPC spawns, silently.", "Default: 0 (the Overworld)." })
    public static int[] spawnDimensions = { 0 };

    @Config.Comment({
        "Dimension ids that use the Nether spawn table (Zombie Pigman Soldiers, Super Mutants, the Cyberdemon).",
        "Only biomes of the NETHER type hold these spawns.", "Default: -1 (the Nether)." })
    public static int[] netherSpawnDimensions = { -1 };

    @Config.Comment({ "Dimension ids that use the End spawn table (Commando squads).",
        "Only biomes of the END type hold these spawns.", "Default: 1 (the End)." })
    public static int[] endSpawnDimensions = { 1 };

    @Config.Comment({ "Zombie Soldier, Zombie Farmer and Zombie Miner spawn naturally in the Overworld,",
        "and the Zombie Pigman Soldier in the Nether.", "Default: true." })
    public static boolean zombies = true;

    @Config.Comment({ "Skeleton Soldier spawns naturally in the Overworld and Commando squads in the End.",
        "Army Soldiers come from camps, carriers and silo complexes.", "Default: true." })
    public static boolean military = true;

    @Config.Comment({ "Bandits spawn naturally.", "Default: true." })
    public static boolean bandits = true;

    @Config.Comment({
        "Mini-bosses may spawn: Psycho Steve naturally, Dictator Dave at the post of a red Military Base,",
        "and both from eggs. Off, none does.", "Default: true." })
    public static boolean miniBosses = true;

    @Config.Comment({ "Super Mutants may spawn (naturally in the Nether, and from eggs). Off, none does.",
        "Default: true." })
    public static boolean mutants = true;

    @Config.Comment({
        "Alien Bugs may spawn (spawner block and egg). Off, the spawner stays idle and the egg does nothing.",
        "Default: true." })
    public static boolean alienBug = true;

    @Config.Comment({ "The Cyberdemon may spawn (a rare boss deep in the Nether, and from eggs). Off, none does.",
        "Default: true." })
    public static boolean cyberdemon = true;

    @Config.Comment({
        "The Outcast may spawn (the elite posts of silo complexes with a LuV or higher ceiling, and eggs).",
        "Off, none does.", "Default: true." })
    public static boolean outcast = true;

    @Config.Comment({ "The Attack Helicopter may spawn (camps, and when a Camp Flag breaks).", "Default: true." })
    public static boolean helicopter = true;

    @Config.Comment({ "Spawn weight of the Zombie Farmer. 0 means it is never registered or selected.",
        "Default: 100 (half of the Techguns value)." })
    @Config.RangeInt(min = 0, max = 10000)
    public static int weightZombieFarmer = 100;

    @Config.Comment({ "Spawn weight of the Zombie Miner. 0 disables it.", "Default: 100." })
    @Config.RangeInt(min = 0, max = 10000)
    public static int weightZombieMiner = 100;

    @Config.Comment({ "Spawn weight of the Zombie Soldier. 0 disables it.", "Default: 50." })
    @Config.RangeInt(min = 0, max = 10000)
    public static int weightZombieSoldier = 50;

    @Config.Comment({ "Spawn weight of the Skeleton Soldier. 0 disables it.", "Default: 50." })
    @Config.RangeInt(min = 0, max = 10000)
    public static int weightSkeletonSoldier = 50;

    @Config.Comment({ "Spawn weight of the Bandit. 0 disables it.", "Default: 25." })
    @Config.RangeInt(min = 0, max = 10000)
    public static int weightBandit = 25;

    @Config.Comment({ "Spawn weight of Psycho Steve. 0 disables it.", "Default: 1." })
    @Config.RangeInt(min = 0, max = 10000)
    public static int weightPsychoSteve = 1;

    @Config.Comment({ "Nether spawn weight of the Zombie Pigman Soldier. 0 disables it.", "Default: 15." })
    @Config.RangeInt(min = 0, max = 10000)
    public static int weightNetherPigmanSoldier = 15;

    @Config.Comment({ "Nether spawn weight of the Super Mutant (Basic), from Nether danger 2. 0 disables it.",
        "Default: 6." })
    @Config.RangeInt(min = 0, max = 10000)
    public static int weightNetherMutantBasic = 6;

    @Config.Comment({ "Nether spawn weight of the Heavy Super Mutant, from Nether danger 3. 0 disables it.",
        "Default: 3." })
    @Config.RangeInt(min = 0, max = 10000)
    public static int weightNetherMutantHeavy = 3;

    @Config.Comment({ "Nether spawn weight of the Elite Super Mutant, from Nether danger 4. 0 disables it.",
        "Default: 2." })
    @Config.RangeInt(min = 0, max = 10000)
    public static int weightNetherMutantElite = 2;

    @Config.Comment({ "Nether spawn weight of the Cyberdemon, from Nether danger 4, one at a time. 0 disables it.",
        "Default: 1." })
    @Config.RangeInt(min = 0, max = 10000)
    public static int weightNetherCyberdemon = 1;

    @Config.Comment({ "End spawn weight of a Commando squad (2 to 3). 0 disables it.", "Default: 1." })
    @Config.RangeInt(min = 0, max = 10000)
    public static int weightEndCommando = 1;

    @Config.Comment({ "No NPC spawns naturally in the End closer than this to its center (0, 0), in blocks,",
        "so the dragon's island stays clear.", "Default: 256." })
    @Config.RangeInt(min = 0, max = 100000)
    public static int endMinDistance = 256;

    @Config.Comment({ "A natural boss (the Cyberdemon) does not spawn while another of its kind is within this many",
        "blocks (a square, full height, loaded chunks only).", "Default: 128." })
    @Config.RangeInt(min = 0, max = 1024)
    public static int bossSpacing = 128;

    @Config.Comment({ "Distance from the world spawn (blocks) at which spawn danger level 0 ends and level 1 begins.",
        "Default: 500." })
    @Config.RangeInt(min = 0, max = 100000)
    public static int distanceLevel0 = 500;

    @Config.Comment({ "Distance from the world spawn (blocks) at which danger level 2 begins.", "Default: 1000." })
    @Config.RangeInt(min = 0, max = 100000)
    public static int distanceLevel1 = 1000;

    @Config.Comment({ "Distance from the world spawn (blocks) at which the highest distance danger begins.",
        "Default: 2500." })
    @Config.RangeInt(min = 0, max = 100000)
    public static int distanceLevel2 = 2500;

    @Config.Comment({ "Most GregGuns NPCs that may stand in one chunk before natural spawning there stops.",
        "Default: 4." })
    @Config.RangeInt(min = 0, max = 256)
    public static int perChunkCap = 4;

    @Config.Comment({ "Most GregGuns NPCs counted around one player before natural spawning near them stops.",
        "Default: 8." })
    @Config.RangeInt(min = 0, max = 1024)
    public static int perPlayerCap = 8;

    @Config.Comment({ "NPC gunfire hurts its targets when true. When false NPC bullets deal no damage.",
        "Default: true." })
    public static boolean gunDamageEnabled = true;

    @Config.Comment({ "Adds tiered GregGuns drops to elite, ultra and infernal GregGuns NPCs (needs Infernal Mobs).",
        "Leave Infernal Mobs' own drop lists unchanged when this is on, or an NPC may drop twice.", "Default: true." })
    public static boolean infernalTieredLoot = true;

    @Config.Comment({ "NPCs placed by spawner blocks never roll as infernal mobs when true (farm guard).",
        "Off, they roll like any other NPC at Infernal Mobs' own rates.", "Default: false." })
    public static boolean infernalSpawnerOptOut = false;
}
