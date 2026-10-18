package com.gregguns.core.config;

import com.gtnewhorizon.gtnhlib.config.Config;

/**
 * Back-device numbers, the {@code flight} category of {@code config/gregguns.cfg}: the Jump Pack and the Anti Gravity
 * Device (the Glider uses no energy). Fields only; no Minecraft types. Synced, because the client shows capacities in
 * tooltips and the server decides every drain.
 */
// Spec: docs/gregguns-spec/05-armor/powered-flight.md (GGS-05-15); docs/design/0003-energy-model.md (every capacity
// is a config option)
@Config(modid = "gregguns", category = "flight")
@Config.Sync
public class FlightConfig {

    @Config.Comment({ "Energy the Jump Pack holds, in EU (HV electric item). Range 1000 to 100000000.",
        "Default: 1000000 (a Mech Armor HV core holds the same)." })
    @Config.RangeInt(min = 1000, max = 100000000)
    public static int jumpPackCapacityEu = 1000000;

    @Config.Comment({ "EU one Jump Pack boost jump costs. Range 0 to 1000000. Default: 2000." })
    @Config.RangeInt(min = 0, max = 1000000)
    public static int jumpPackJumpCostEu = 2000;

    @Config.Comment({ "EU the Jump Pack spends to absorb one fall longer than 3 blocks. Range 0 to 1000000.",
        "Default: 1000." })
    @Config.RangeInt(min = 0, max = 1000000)
    public static int jumpPackFallCostEu = 1000;

    @Config.Comment({ "Energy the Anti Gravity Device holds, in EU (IV electric item). Range 1000 to 1000000000.",
        "Default: 2000000." })
    @Config.RangeInt(min = 1000, max = 1000000000)
    public static int antiGravityCapacityEu = 2000000;

    @Config.Comment({ "EU per tick the Anti Gravity Device draws while it is flying (airborne and switched on).",
        "Default 100 lasts about 17 minutes of flight on a full charge. Range 0 to 100000. Default: 100." })
    @Config.RangeInt(min = 0, max = 100000)
    public static int antiGravityDriveEuPerTick = 100;
}
