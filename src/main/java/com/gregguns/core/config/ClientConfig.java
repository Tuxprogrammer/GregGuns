package com.gregguns.core.config;

import com.gtnewhorizon.gtnhlib.config.Config;

/**
 * Client-side display options, the {@code client} category of {@code config/gregguns.cfg}. Fields only; no Minecraft
 * types. Not synced: only the local HUD reads them.
 */
// Spec: docs/gregguns-spec/01-core/hud-overlays.md (GGS-01-32: bottom-right plus hudOffsetX / hudOffsetY)
@Config(modid = "gregguns", category = "client")
public class ClientConfig {

    @Config.Comment({ "Draw the ammo HUD (rounds in the gun, magazines carried, reload progress) while a gun is held.",
        "Default: true." })
    public static boolean showAmmoHud = true;

    @Config.Comment({ "Distance in pixels from the right screen edge to the right edge of the ammo HUD text.",
        "Default: 6." })
    @Config.RangeInt(min = 0, max = 2000)
    public static int hudOffsetX = 6;

    @Config.Comment({ "Distance in pixels from the bottom screen edge to the bottom of the ammo HUD.",
        "The default sits above the InGame-Info-XML potion lines that the pack draws in the same corner.",
        "Default: 40." })
    @Config.RangeInt(min = 0, max = 2000)
    public static int hudOffsetY = 40;

    @Config.Comment({
        "Draw the Ammo Press's progress-synced front animation. When off, the active press shows its still",
        "front overlay. Purely cosmetic; never changes a recipe. Default: true." })
    public static boolean ammoPressAnimation = true;

    @Config.Comment({ "Most GregGuns effect particles alive at once. Bursts shrink as the count nears this ceiling and",
        "never pass it, so other mods keep room under the pack's particle cap (8000 in GTNH).", "Default: 2000." })
    @Config.RangeInt(min = 100, max = 8000)
    public static int particleCeiling = 2000;

    @Config.Comment({
        "Aim down sights: right-click on an iron-sight or red-dot gun moves the gun to the screen center.",
        "False turns the client half off: the gun is not moved and the right-click is not sent for those guns.",
        "Default: true." })
    public static boolean adsEnabled = true;

    @Config.Comment({ "Field of view multiplier while aiming down the sights of a gun with no scope zoom of its own",
        "(0.9 narrows the view by 10 percent, 1.0 leaves it alone). Scoped guns keep their own zoom. Default: 0.9." })
    @Config.RangeFloat(min = 0.3F, max = 1.0F)
    public static float adsFov = 0.9F;

    @Config.Comment({ "Client ticks the gun takes to move to the center (and back). 1 is nearly instant. Default: 5." })
    @Config.RangeInt(min = 1, max = 20)
    public static int adsTransitionTicks = 5;

    @Config.Comment({ "Hide the crosshair while the sights are lined up. Default: true." })
    public static boolean adsHideCrosshair = true;

    @Config.Comment({ "Stop sprinting while aiming down the sights (the sprint key is ignored until you stop aiming).",
        "Default: true." })
    public static boolean adsStopsSprint = true;
}
