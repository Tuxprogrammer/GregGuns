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
}
