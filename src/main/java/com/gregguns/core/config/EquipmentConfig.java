package com.gregguns.core.config;

import com.gtnewhorizon.gtnhlib.config.Config;

/**
 * Special-slot equipment numbers, the {@code equipment} category of {@code config/gregguns.cfg}: the Night Vision
 * Goggles, the Tactical Mask, the Gas Mask and the Oxygen Tanks. Fields only; no Minecraft types. Synced, because the
 * client shows the capacities in tooltips and the server decides the drain.
 */
// Spec: docs/gregguns-spec/05-armor/baubles-slots.md; docs/gregguns-spec/05-armor/oxygen-gear.md (GGS-05-24);
// docs/design/0003-energy-model.md (every capacity is a config option)
@Config(modid = "gregguns", category = "equipment")
@Config.Sync
public class EquipmentConfig {

    @Config.Comment({ "Energy the Night Vision Goggles hold, in EU (MV electric item). Range 1000 to 100000000.",
        "Default: 100000." })
    @Config.RangeInt(min = 1000, max = 100000000)
    public static int nightVisionCapacityEu = 100000;

    @Config.Comment({ "EU per tick the Night Vision Goggles and the Tactical Mask draw while night vision is on.",
        "GT's own Mech Armor night vision draws 2. Range 0 to 1000. Default: 2." })
    @Config.RangeInt(min = 0, max = 1000)
    public static int nightVisionDrainEuPerTick = 2;

    @Config.Comment({ "Energy the Tactical Mask holds, in EU (HV electric item). Range 1000 to 100000000.",
        "Default: 200000." })
    @Config.RangeInt(min = 1000, max = 100000000)
    public static int tacticalMaskCapacityEu = 200000;

    @Config.Comment({ "Ticks of poison and wither cure one Gas Mask filter holds; each cured effect costs one tick.",
        "Applies to the Gas Mask only. Range 2 to 100000. Default: 300 (Techguns' value)." })
    @Config.RangeInt(min = 2, max = 100000)
    public static int gasMaskFilterTicks = 300;

    @Config.Comment({ "Air units one Oxygen Tanks item holds: the item's durability, which Galacticraft drains one",
        "point per 9 ticks outside a breathable block. Default 2000 is Galacticraft's tier 2 tank.",
        "Range 100 to 100000. Default: 2000." })
    @Config.RangeInt(min = 100, max = 100000)
    public static int oxygenTankCapacity = 2000;
}
