package com.gregguns.core.config;

import com.gtnewhorizon.gtnhlib.config.Config;

/**
 * Techguns' "Damage Factors" category, the {@code damagefactors} category of {@code config/gregguns.cfg}. Fields only;
 * no Minecraft types. All fields are synced: the server value wins on a multiplayer client (GGS-01-03), so a client
 * never works from a stale local value.
 */
// Spec: docs/gregguns-spec/01-core/config-gtnhlib.md ("Damage Factors ... Kept; @Config.Sync")
// Spec: docs/techguns-spec/01-core/config.md (Category Damage Factors)
@Config(modid = "gregguns", category = "damagefactors")
@Config.Sync
public class DamageFactorsConfig {

    @Config.Comment({
        "Damage multiplier when a player's shot hits another player. Has no effect while server PvP is off",
        "(PvP damage is then 0). Range 0 to 100. Default: 0.5." })
    @Config.RangeFloat(min = 0.0F, max = 100.0F)
    public static float damagePvP = 0.5F;

    @Config.Comment({ "Damage multiplier when a turret's shot hits a player. Used from the turret milestone on.",
        "Range 0 to 100. Default: 0.5." })
    @Config.RangeFloat(min = 0.0F, max = 100.0F)
    public static float damageTurretToPlayer = 0.5F;

    @Config.Comment({ "Damage multiplier for other NPCs hitting players, and for NPC against NPC. Used from the NPC",
        "milestone on. Range 0 to 100. Default: 1.0." })
    @Config.RangeFloat(min = 0.0F, max = 100.0F)
    public static float damageFactorNPC = 1.0F;

    @Config.Comment({ "Scales the armor penetration of non-player attackers against players. 0 makes armor",
        "fully effective against them. Range 0 to 1. Default: 1.0." })
    @Config.RangeFloat(min = 0.0F, max = 1.0F)
    public static float armorPenetrationMultiplierNPCtoPlayer = 1.0F;

    @Config.Comment({ "Scales the armor penetration of a player's shots against other players. Range 0 to 1.",
        "Default: 1.0." })
    @Config.RangeFloat(min = 0.0F, max = 1.0F)
    public static float armorPenetrationMultiplierPvP = 1.0F;
}
