package com.gregguns.core.config;

import com.gtnewhorizon.gtnhlib.config.Config;

/**
 * Aim down sights (ADS), the server half: the {@code aim} category of {@code config/gregguns.cfg}. The server owns the
 * aiming state and rolls the spread, so these two keys decide whether a right-click on an iron-sight or red-dot gun
 * starts an aim and how much it tightens the spread. The client's own options (field of view, speed, crosshair) are in
 * {@link ClientConfig}. Fields only; no Minecraft types. Read on the server, none is synced.
 */
// Spec: docs/gregguns-spec/03-gun-visuals-audio/ads.md; docs/gregguns-spec/02-guns/index.md (aim down sights)
@Config(modid = "gregguns", category = "aim")
public class AimConfig {

    @Config.Comment({ "Let the sights-only guns (iron sights or a red dot, no scope) aim down their sights on a",
        "right-click. When false the server ignores the aim request and a gun's own scope zoom is unaffected.",
        "Default: true." })
    public static boolean adsEnabled = true;

    @Config.Comment({ "Multiplier on a shot's spread while aiming down the sights of a gun that has no scope zoom of",
        "its own (0.75 is 25 percent tighter, 1.0 is no bonus). A gun with a scope keeps its own zoom spread",
        "multiplier and this never stacks on it. Default: 0.75." })
    @Config.RangeFloat(min = 0.0F, max = 1.0F)
    public static float adsSpreadMultiplier = 0.75F;
}
