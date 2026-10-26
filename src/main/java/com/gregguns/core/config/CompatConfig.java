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
}
