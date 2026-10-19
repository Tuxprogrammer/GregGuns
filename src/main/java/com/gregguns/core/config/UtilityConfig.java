package com.gregguns.core.config;

import com.gtnewhorizon.gtnhlib.config.Config;

/**
 * Utility and defense tuning (M6), the {@code utility} category of {@code config/gregguns.cfg}. Every field the Turret,
 * Rampart, explosive charges, Bio Blob and Camp Flag read is declared here once, so the parallel M6 chunks never edit
 * this file. Fields only; no Minecraft types. Read on the server, none is synced.
 */
// Spec: docs/gregguns-spec/09-machines-utility/turret.md (GGS-09-07), explosive-charges.md (GGS-09-14),
// docs/techguns-spec/09-machines-utility/misc-tile-entities.md (Bio Blob, Camp Flag)
@Config(modid = "gregguns", category = "utility")
public class UtilityConfig {

    @Config.Comment({ "EU per tick a Turret draws while it stands guard.", "Default: 4." })
    @Config.RangeInt(min = 0, max = 4096)
    public static int turretUpkeepEuPerTick = 4;

    @Config.Comment({ "EU the Turret spends per hit point it heals.", "Default: 120." })
    @Config.RangeInt(min = 0, max = 100000)
    public static int turretEuPerHealedHp = 120;

    @Config.Comment({ "EU per tick the Turret draws during the repair after its entity died.", "Default: 10." })
    @Config.RangeInt(min = 0, max = 4096)
    public static int turretRepairEuPerTick = 10;

    @Config.Comment({ "Length in ticks of the repair before a dead Turret entity respawns.", "Default: 200." })
    @Config.RangeInt(min = 20, max = 24000)
    public static int turretRepairTicks = 200;

    @Config.Comment({ "Blocks within which a Turret looks for targets.", "Default: 24." })
    @Config.RangeInt(min = 4, max = 64)
    public static int turretRange = 24;

    @Config.Comment({ "Highest block hardness the Explosive Charge (TNT) bores through. Obsidian is 50.",
        "Default: 30.0." })
    @Config.RangeFloat(min = 0.0F, max = 100000.0F)
    public static float chargeMaxHardness = 30.0F;

    @Config.Comment({ "Highest block hardness the Advanced Explosive Charge bores through.", "Default: 100.0." })
    @Config.RangeFloat(min = 0.0F, max = 100000.0F)
    public static float chargeAdvancedMaxHardness = 100.0F;

    @Config.Comment({ "Bio Blob poison does not hurt players when true. Other living entities are always hurt.",
        "Default: false." })
    public static boolean bioBlobSparesPlayers = false;

    @Config.Comment({ "Ticks between one Bio Blob decay step and the next.", "Default: 60." })
    @Config.RangeInt(min = 1, max = 24000)
    public static int bioBlobDecayTicks = 60;

    @Config.Comment({ "Ticks between Camp Flag spawn attempts.", "Default: 200." })
    @Config.RangeInt(min = 20, max = 24000)
    public static int campFlagSpawnIntervalTicks = 200;

    @Config.Comment({ "Camp Flag NPC cap is int(((sizeX + sizeZ) / 2) * this factor).", "Default: 0.75." })
    @Config.RangeFloat(min = 0.0F, max = 10.0F)
    public static float campFlagCapFactor = 0.75F;

    @Config.Comment({ "Breaking a Camp Flag sends a helicopter against the player (does nothing until NPCs exist).",
        "Default: true." })
    public static boolean campFlagBreakHelicopter = true;
}
