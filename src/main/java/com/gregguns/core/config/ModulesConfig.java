package com.gregguns.core.config;

import com.gtnewhorizon.gtnhlib.config.Config;

/**
 * Module switches, the {@code modules} category of {@code config/gregguns.cfg}. One flag per roadmap content area.
 *
 * <p>
 * Every flag defaults to {@code false} for now: the roadmap says a flag defaults to on once its milestone's exit gate
 * is met, and no content milestone is met yet. The pull request that completes a milestone flips that milestone's
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
        "Default: false until the M2 exit gate is met." })
    @Config.RequiresMcRestart
    public static boolean guns = false;

    @Config.Comment({ "Armor, set bonuses and special slots (M3). Needs 'guns'.",
        "Default: false until the M3 exit gate is met." })
    @Config.RequiresMcRestart
    public static boolean armor = false;

    @Config.Comment({ "Production chain and GregTech machines (M4). Needs 'guns'.",
        "Default: false until the M4 exit gate is met." })
    @Config.RequiresMcRestart
    public static boolean machines = false;

    @Config.Comment({ "Energy and exotic weapons (M5). Needs 'guns' and 'machines'.",
        "Default: false until the M5 exit gate is met." })
    @Config.RequiresMcRestart
    public static boolean energyWeapons = false;

    @Config.Comment({ "Utility and defense machines (M6). Needs 'machines'.",
        "Default: false until the M6 exit gate is met." })
    @Config.RequiresMcRestart
    public static boolean utility = false;

    @Config.Comment({ "NPCs and spawning (M7). Needs 'guns'.", "Default: false until the M7 exit gate is met." })
    @Config.RequiresMcRestart
    public static boolean npcs = false;

    @Config.Comment({ "Worldgen and structures (M8). Needs 'npcs'.", "Default: false until the M8 exit gate is met." })
    @Config.RequiresMcRestart
    public static boolean worldgen = false;
}
