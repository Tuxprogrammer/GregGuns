package com.gregguns.core.config;

import com.gtnewhorizon.gtnhlib.config.Config;

/**
 * Gun handling on the server: the {@code guns} category of {@code config/gregguns.cfg}. The server owns every reload,
 * so these keys are read there only and none is synced; the client animates what the server's reload messages say.
 * Fields only; no Minecraft types.
 */
// Spec: docs/design/0026-single-round-and-chamber-reloads.md; docs/gregguns-spec/02-guns/index.md (reloading)
@Config(modid = "gregguns", category = "guns")
public class GunsConfig {

    @Config.Comment({ "Ticks a held revolver or shotgun (the guns that load one chamber at a time) must go without a",
        "shot, a trigger pull or a reload before it starts loading its empty chambers by itself, when the player",
        "carries its ammo. Firing stops it, as it stops any chamber reload. 0 turns the idle reload off.",
        "Default: 40." })
    @Config.RangeInt(min = 0, max = 1200)
    public static int idleReloadDelayTicks = 40;
}
