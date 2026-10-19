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

    @Config.Comment({ "Dimension ids in which NPCs may spawn naturally. Other dimensions are skipped silently.",
        "Default: 0 (the Overworld)." })
    public static int[] spawnDimensions = { 0 };

    @Config.Comment({ "Zombie Soldier, Zombie Farmer and Zombie Miner spawn naturally.", "Default: true." })
    public static boolean zombies = true;

    @Config.Comment({ "Skeleton Soldier spawns naturally. Army Soldier and Commando come from camps and eggs only.",
        "Default: true." })
    public static boolean military = true;

    @Config.Comment({ "Bandits spawn naturally.", "Default: true." })
    public static boolean bandits = true;

    @Config.Comment({ "Mini-bosses (Psycho Steve, Dictator Dave) may spawn.", "Default: true." })
    public static boolean miniBosses = true;

    @Config.Comment({ "Super Mutants may spawn (camps and eggs).", "Default: true." })
    public static boolean mutants = true;

    @Config.Comment({ "Alien Bugs may spawn (spawner block and egg).", "Default: false." })
    public static boolean alienBug = false;

    @Config.Comment({ "The Cyberdemon may spawn (egg and command only).", "Default: false." })
    public static boolean cyberdemon = false;

    @Config.Comment({ "The Attack Helicopter may spawn (camps, and when a Camp Flag breaks).", "Default: false." })
    public static boolean helicopter = false;

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
}
