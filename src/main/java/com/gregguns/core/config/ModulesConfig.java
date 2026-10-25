package com.gregguns.core.config;

import com.gtnewhorizon.gtnhlib.config.Config;

/**
 * Module switches, the {@code modules} category of {@code config/gregguns.cfg}. One flag per roadmap content area.
 *
 * <p>
 * A flag defaults to {@code true} once its milestone's exit gate is met, as the roadmap says. The gates for M2 to M8
 * are met, so every module flag defaults to on, except {@code icbm}, which stays off until the ICBM beta gate. The pull
 * request that completes a milestone flips that milestone's
 * default (here and in {@code docs/config.md}).
 *
 * <p>
 * Fields only, no logic, and no Minecraft types, so the class loads in a plain unit test. Names must match
 * {@link com.gregguns.core.module.ModuleId#configKey()}. The flags never change what is registered: content always
 * exists, so a client and server with different flags can join each other. A flag only switches behavior (recipes,
 * spawning, worldgen, machine operation, NEI and creative visibility, use of existing items), and the server's values
 * reach clients at login (see {@code ModuleState}).
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

    @Config.Comment({ "Armor, set bonuses and special slots (M3). Needs 'guns' (behavior only).",
        "Default: true (M3 exit gate met)." })
    @Config.RequiresMcRestart
    public static boolean armor = true;

    @Config.Comment({ "Production chain and GregTech machines (M4). Needs 'guns' (behavior only).",
        "Default: true (M4 exit gate met)." })
    @Config.RequiresMcRestart
    public static boolean machines = true;

    @Config.Comment({ "Energy and exotic weapons (M5). Needs 'guns' and 'machines' (behavior only).",
        "Default: true (M5 exit gate met)." })
    @Config.RequiresMcRestart
    public static boolean energyWeapons = true;

    @Config.Comment({ "Utility and defense machines (M6). Needs 'machines' (behavior only).",
        "Default: true (M6 exit gate met)." })
    @Config.RequiresMcRestart
    public static boolean utility = true;

    @Config.Comment({ "NPCs and spawning (M7). Needs 'guns' (behavior only).", "Default: true (M7 exit gate met)." })
    @Config.RequiresMcRestart
    public static boolean npcs = true;

    @Config.Comment({ "Worldgen and structures (M8). Needs 'npcs' (behavior only).",
        "Default: true (M8 exit gate met)." })
    @Config.RequiresMcRestart
    public static boolean worldgen = true;

    @Config.Comment({
        "ICBM missiles and warheads (ICBM roadmap I0 to I7). Needs 'guns' and 'machines' (behavior only),",
        "and the Galacticraft mod: without Galacticraft this module turns itself off with a log line (D9).",
        "Default: false until the ICBM beta gate." })
    @Config.RequiresMcRestart
    public static boolean icbm = false;
}
