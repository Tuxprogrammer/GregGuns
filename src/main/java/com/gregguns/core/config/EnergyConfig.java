package com.gregguns.core.config;

import com.gtnewhorizon.gtnhlib.config.Config;

/**
 * Energy gun capacity switches, the {@code energy} category of {@code config/gregguns.cfg}. Fields only; no
 * Minecraft types. The scale is synced because clients read the capacity for tooltips and the charge bar.
 */
// Spec: docs/gregguns-spec/02-guns/energy-electric-items.md (GGS-02-14); docs/design/0003-energy-model.md (every
// capacity is a config option)
@Config(modid = "gregguns", category = "energy")
public class EnergyConfig {

    @Config.Comment({
        "Scale of every energy gun's default capacity, in percent (100 = the GT battery of the gun's tier).",
        "Range 1 to 10000. Default: 100." })
    @Config.RangeInt(min = 1, max = 10000)
    @Config.Sync
    public static int capacityScalePercent = 100;

    @Config.Comment({ "Per-gun capacity overrides in EU, one 'gunId=eu' per line, for example laser_rifle=8000000.",
        "An override wins over the scale. Lines that do not parse or hold less than one shot are ignored.",
        "Not synced: set the same lines on the server and the clients so tooltips match. Default: none." })
    public static String[] capacityOverrides = {};
}
