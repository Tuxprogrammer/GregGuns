package com.gregguns.core.config;

import com.gtnewhorizon.gtnhlib.config.Config;

/**
 * Machine tuning, the {@code machines} category of {@code config/gregguns.cfg}. Fields only; no Minecraft types. Read
 * on the server.
 */
// Spec: docs/gregguns-spec/08-machines-production/reaction-chamber-multiblock.md (GGS-08-25)
@Config(modid = "gregguns", category = "machines")
public class MachinesConfig {

    @Config.Comment({ "Pollution the Retort adds per second while it runs. The single block pollutes nothing.",
        "Default: 100." })
    @Config.RangeInt(min = 0, max = 100000)
    public static int retortPollutionPerSecond = 100;

    @Config.Comment({ "Pollution added once to the chunk when a reaction fails.", "Default: 500." })
    @Config.RangeInt(min = 0, max = 100000)
    public static int reactionFailurePollution = 500;

    @Config.Comment({ "Radius in blocks around a failed reaction in which players are irradiated.", "Default: 8." })
    @Config.RangeInt(min = 1, max = 64)
    public static int reactionRadiationRadius = 8;

    @Config.Comment({ "Radiation level (GT applyRadioactivity) of a RAD_LOW failure.", "Default: 1." })
    @Config.RangeInt(min = 0, max = 10)
    public static int radiationLevelLow = 1;

    @Config.Comment({ "Radiation level of a RAD_MEDIUM failure.", "Default: 3." })
    @Config.RangeInt(min = 0, max = 10)
    public static int radiationLevelMedium = 3;

    @Config.Comment({ "Radiation level of a RAD_HIGH failure.", "Default: 5." })
    @Config.RangeInt(min = 0, max = 10)
    public static int radiationLevelHigh = 5;

    @Config.Comment({ "GT voltage index whose explosion power a small (EXPLOSION_LOW) failure uses. 1 is LV.",
        "Default: 1." })
    @Config.RangeInt(min = 0, max = 7)
    public static int explosionLowVoltageIndex = 1;

    @Config.Comment({ "GT voltage index whose explosion power a medium (EXPLOSION_MEDIUM) failure uses. 2 is MV.",
        "Default: 2." })
    @Config.RangeInt(min = 0, max = 7)
    public static int explosionMediumVoltageIndex = 2;
}
