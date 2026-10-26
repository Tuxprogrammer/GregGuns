package com.gregguns.core.config;

import com.gtnewhorizon.gtnhlib.config.Config;

/**
 * ICBM blast switches, the {@code icbm} category of {@code config/gregguns.cfg}. Fields only; no Minecraft types.
 * Synced: the server's values decide what a blast does. Terrain breaks by the per-tier key here only;
 * {@code explosions.blockDamage}
 * governs hand-held weapons, not ICBM blasts (D11, D52). Claims and protection always apply.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/explosive-registry.md (config); DECISIONS.md D11
@Config(modid = "gregguns", category = "icbm")
@Config.Sync
public class IcbmConfig {

    @Config.Comment({ "The highest explosive tier that may detonate (1 to 4); a warhead above it does nothing.",
        "Default: 4." })
    @Config.RangeInt(min = 1, max = 4)
    public static int maxBlastTier = 4;

    @Config.Comment({ "Whether tier 1 (HV) blasts may remove terrain.", "Default: true." })
    public static boolean terrainDamageT1 = true;

    @Config.Comment({ "Whether tier 2 (EV to IV) blasts may remove terrain.", "Default: true." })
    public static boolean terrainDamageT2 = true;

    @Config.Comment({ "Whether tier 3 (LuV to ZPM) blasts may remove terrain.", "Default: true." })
    public static boolean terrainDamageT3 = true;

    @Config.Comment({ "Whether tier 4 (UV and UHV) blasts may remove terrain.", "Default: true." })
    public static boolean terrainDamageT4 = true;

    @Config.Comment({ "Milliseconds per server tick that all running blasts share for breaking blocks.",
        "Range 1 to 40. Default: 12." })
    @Config.RangeInt(min = 1, max = 40)
    public static int blastBudgetMs = 12;

    @Config.Comment({ "Most blasts that may run at once; a detonation past this is refused. Range 1 to 256.",
        "Default: 16." })
    @Config.RangeInt(min = 1, max = 256)
    public static int maxActiveBlasts = 16;

    @Config.Comment({ "Largest blast radius in blocks; bigger ones are clamped. The ray cast copies a cube of",
        "blocks this wide, so memory grows with the cube (96 is about 14 MB). Range 8 to 200. Default: 96." })
    @Config.RangeInt(min = 8, max = 200)
    public static int maxBlastRadius = 96;

    @Config.Comment({ "Whether blocks destroyed by a blast drop items. Off avoids thousands of item entities.",
        "Default: false." })
    public static boolean blastDrops = false;

    @Config.Comment({
        "Multiplier on a hand-lit or redstone-lit warhead's fuse. The base fuse is 3 seconds plus 1 second",
        "per 3 blocks of the warhead's blast radius, rounded up (a radius 9 warhead: 6 s). A warhead set off by",
        "another explosion keeps a fixed 10 tick fuse. Range 0.1 to 10. Default: 1.0." })
    @Config.RangeDouble(min = 0.1, max = 10.0)
    public static double fuseMultiplier = 1.0;

    /**
     * @param tier the explosive tier number, 1 to 4
     * @return the terrain switch for that tier
     */
    public static boolean terrainDamage(int tier) {
        switch (tier) {
            case 1:
                return terrainDamageT1;
            case 2:
                return terrainDamageT2;
            case 3:
                return terrainDamageT3;
            default:
                return terrainDamageT4;
        }
    }
}
