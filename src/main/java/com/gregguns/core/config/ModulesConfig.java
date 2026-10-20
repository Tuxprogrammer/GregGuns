package com.gregguns.core.config;

import com.gtnewhorizon.gtnhlib.config.Config;

/**
 * Module switches, the {@code modules} category of {@code config/gregguns.cfg}. One flag per roadmap content area.
 *
 * <p>
 * A flag defaults to {@code true} once its milestone's exit gate is met, as the roadmap says. The gates for M2 to M8
 * are met, so every module flag defaults to on. The pull request that completes a milestone flips that milestone's
 * default (here and in {@code docs/config.md}).
 *
 * <p>
 * Fields only, no logic, and no Minecraft types, so the class loads in a plain unit test. Names must match
 * {@link com.gregguns.core.module.ModuleId#configKey()}. The flags are not {@code @Config.Sync}: content is registered
 * once at startup, so a server value arriving later could not add or remove it. A client and server must simply be
 * started with the same flags.
 */
// Spec: docs/gregguns-spec/01-core/config-gtnhlib.md (GGS-01-01, GGS-01-04, GGS-01-06)
// Source:
// https://github.com/GTNewHorizons/GTNHLib/blob/0.11.52/src/main/java/com/gtnewhorizon/gtnhlib/config/Config.java#L14
@Config(modid = "gregguns", category = "modules")
public class ModulesConfig {

    @Config.Comment({ "Combat core and ballistic arsenal: damage system, guns, projectiles and ammo (M1, M2).",
        "Default: true (M2 exit gate met)." })
    @Config.RequiresMcRestart
    public static boolean guns = true;

    @Config.Comment({ "Armor, set bonuses and special slots (M3). Needs 'guns'.", "Default: true (M3 exit gate met)." })
    @Config.RequiresMcRestart
    public static boolean armor = true;

    @Config.Comment({ "Production chain and GregTech machines (M4). Needs 'guns'.",
        "Default: true (M4 exit gate met)." })
    @Config.RequiresMcRestart
    public static boolean machines = true;

    @Config.Comment({ "Energy and exotic weapons (M5). Needs 'guns' and 'machines'.",
        "Default: true (M5 exit gate met)." })
    @Config.RequiresMcRestart
    public static boolean energyWeapons = true;

    @Config.Comment({ "Utility and defense machines (M6). Needs 'machines'.", "Default: true (M6 exit gate met)." })
    @Config.RequiresMcRestart
    public static boolean utility = true;

    @Config.Comment({ "NPCs and spawning (M7). Needs 'guns'.", "Default: true (M7 exit gate met)." })
    @Config.RequiresMcRestart
    public static boolean npcs = true;

    @Config.Comment({ "Worldgen and structures (M8). Needs 'npcs'.", "Default: true (M8 exit gate met)." })
    @Config.RequiresMcRestart
    public static boolean worldgen = true;
}
