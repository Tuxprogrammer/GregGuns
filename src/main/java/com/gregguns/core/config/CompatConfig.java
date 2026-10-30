package com.gregguns.core.config;

import com.gtnewhorizon.gtnhlib.config.Config;

/**
 * Switches for the in-memory pack-fit hooks, the {@code compat} category of {@code config/gregguns.cfg}. Each hook
 * adjusts another mod's settings in memory at startup; none writes another mod's config file. Fields only; no
 * Minecraft types. Not synced: each side applies its own copy.
 */
// Spec: docs/gregguns-spec/PACK-CHANGES.md (status column), docs/design/0015-m9-self-contained-polish.md (decision 3)
@Config(modid = "gregguns", category = "compat")
public class CompatConfig {

    @Config.Comment({
        "Adds every GregGuns gun to Backhand's offhandBlacklist and mainhandUseStopsOffhandFallback lists",
        "in memory at startup (a gun cannot fire from the offhand, and a zoom right-click must not also use the",
        "offhand item). Needs Backhand; ignored without it. backhand.cfg is never written. Default: true." })
    public static boolean backhandGuns = true;

    @Config.Comment({ "Registers Thaumcraft scan aspects for the GregGuns NPCs (gregguns.<Name>) at startup. Needs",
        "Thaumcraft; ignored without it. A ThaumcraftMobAspects gregguns.cfg that a pack ships still registers its",
        "own values; turn this off if the pack wants GregGuns mobs left unscannable. Default: true." })
    public static boolean thaumcraftMobAspects = true;

    @Config.Comment({ "Adds 'gregguns.' to AngerMod's kamikaze.butcherItems (so melee kills with GregGuns tools do not",
        "trigger its 10% explosion) and 'gregguns.SimpleTurret' to Mobs Info's MobBlacklist, in memory at startup.",
        "Needs the respective mod; ignored without it. Their config files are never written. Default: true." })
    public static boolean listHooks = true;

    @Config.Comment({ "Puts every GregGuns NPC on EnderIO's Powered Spawner blacklist by IMC at startup, so breaking a",
        "GregGuns structure spawner drops no Broken Spawner and releases no burst of extra mobs, and GregGuns NPCs",
        "cannot be farmed in a Powered Spawner. Needs EnderIO; ignored without it. Default: true." })
    public static boolean enderioSpawnerBlacklist = true;

    @Config.Comment({ "Server only. Adds the GregGuns trades (guns, ammo, magazines, parts, armor, gear and NPC spawn",
        "eggs, priced in GregGuns Coins) to the pack's Vending Machine when the server starts, in memory; its",
        "tradeDatabase.json is never written. With the GregGuns quest line installed, each tier's trades open with",
        "the tier's intro quest. Needs the Vending Machine mod; ignored without it. Off removes them at the next",
        "start. Default: true." })
    public static boolean vendingTrades = true;

    @Config.Comment({ "Adds the GregGuns loot bag groups (the quest rewards, EnhancedLootBags ids 1701-1712) to",
        "EnhancedLootBags' group list at startup, from the jar or from config/gregguns-lootbags.xml when present.",
        "LootBags.xml is never written. Turn off only if the pack defines these groups itself. Needs",
        "EnhancedLootBags; ignored without it. Default: true." })
    public static boolean lootBags = true;
}
