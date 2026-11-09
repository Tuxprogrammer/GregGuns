package com.gregguns.core.config;

import com.gtnewhorizon.gtnhlib.config.Config;

/**
 * The {@code combat} category of {@code config/gregguns.cfg}: hit knockback, and how hard and how accurately hostile
 * NPCs and turrets shoot at players. Fields only; no Minecraft types. All fields are synced: the server's values decide
 * every hit (GGS-01-03).
 *
 * <p>
 * The defaults make a gentler game than the plain gun stats: no knockback, high-tier NPC guns hit for less, and NPCs
 * lose accuracy past 5 blocks. docs/config.md ("Make combat harder") lists the values for a harder game.
 */
// Design: docs/design/0027-combat-difficulty-knobs.md; decisions D357 to D359
@Config(modid = "gregguns", category = "combat")
@Config.Sync
public class CombatConfig {

    @Config.Comment({ "Whether GregGuns gun and projectile hits push their target, for players and NPCs alike.",
        "Explosion blasts push either way. Default: false." })
    public static boolean knockback = false;

    @Config.Comment({
        "How hard a hit pushes while knockback is on, times the gun's own knockback: 1 is vanilla's push.",
        "Range 0 to 10. Default: 1.0." })
    @Config.RangeFloat(min = 0.0F, max = 10.0F)
    public static float knockbackMultiplier = 1.0F;

    @Config.Comment({ "Damage multiplier for NPC and turret shots from Steam-tier guns. Range 0 to 10. Default: 1.0." })
    @Config.RangeFloat(min = 0.0F, max = 10.0F)
    public static float npcDamageSteam = 1.0F;

    @Config.Comment({ "Damage multiplier for NPC and turret shots from LV guns. Range 0 to 10. Default: 1.0." })
    @Config.RangeFloat(min = 0.0F, max = 10.0F)
    public static float npcDamageLV = 1.0F;

    @Config.Comment({ "Damage multiplier for NPC and turret shots from MV guns. Range 0 to 10. Default: 1.0." })
    @Config.RangeFloat(min = 0.0F, max = 10.0F)
    public static float npcDamageMV = 1.0F;

    @Config.Comment({ "Damage multiplier for NPC and turret shots from HV guns, and for the Attack Helicopter.",
        "Range 0 to 10. Default: 0.85." })
    @Config.RangeFloat(min = 0.0F, max = 10.0F)
    public static float npcDamageHV = 0.85F;

    @Config.Comment({ "Damage multiplier for NPC and turret shots from EV guns. Range 0 to 10. Default: 0.7." })
    @Config.RangeFloat(min = 0.0F, max = 10.0F)
    public static float npcDamageEV = 0.7F;

    @Config.Comment({ "Damage multiplier for NPC and turret shots from IV guns. Range 0 to 10. Default: 0.55." })
    @Config.RangeFloat(min = 0.0F, max = 10.0F)
    public static float npcDamageIV = 0.55F;

    @Config.Comment({ "Damage multiplier for NPC and turret shots from LuV guns. Range 0 to 10. Default: 0.45." })
    @Config.RangeFloat(min = 0.0F, max = 10.0F)
    public static float npcDamageLuV = 0.45F;

    @Config.Comment({ "Damage multiplier for NPC and turret shots from ZPM guns. Range 0 to 10. Default: 0.4." })
    @Config.RangeFloat(min = 0.0F, max = 10.0F)
    public static float npcDamageZPM = 0.4F;

    @Config.Comment({
        "Extra damage multiplier for shots from an elite, ultra or infernal NPC (Infernal Mobs) or a boss,",
        "on top of the tier multiplier. Range 0 to 10. Default: 0.75." })
    @Config.RangeFloat(min = 0.0F, max = 10.0F)
    public static float npcEliteDamage = 0.75F;

    @Config.Comment({ "Distance in blocks within which NPC and turret shots keep their gun's full accuracy.",
        "Range 0 to 256. Default: 5.0." })
    @Config.RangeFloat(min = 0.0F, max = 256.0F)
    public static float npcAccuracyRange = 5.0F;

    @Config.Comment({
        "Degrees of aim error an NPC or turret gains per block beyond npcAccuracyRange, up to npcMaxSpread.",
        "0 turns the distance penalty off. Range 0 to 10. Default: 0.12." })
    @Config.RangeFloat(min = 0.0F, max = 10.0F)
    public static float npcSpreadPerBlock = 0.12F;

    @Config.Comment({ "Largest aim error in degrees that distance adds to an NPC or turret shot. Range 0 to 45.",
        "Default: 4.0." })
    @Config.RangeFloat(min = 0.0F, max = 45.0F)
    public static float npcMaxSpread = 4.0F;
}
