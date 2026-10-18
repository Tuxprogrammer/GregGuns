package com.gregguns.core.config;

import com.gtnewhorizon.gtnhlib.config.Config;

/**
 * Developer aids, the {@code debug} category of {@code config/gregguns.cfg}. Fields only; no Minecraft types.
 */
// Spec: docs/gregguns-spec/01-core/config-gtnhlib.md ("Debug: enableDebugKeybinds: Kept")
// Spec: docs/techguns-spec/01-core/config.md (Category Debug)
@Config(modid = "gregguns", category = "debug")
public class DebugConfig {

    @Config.Comment({ "Registers the numpad debug keybinds. Nothing reads this until the M1 keybinds exist.",
        "Default: false." })
    public static boolean enableDebugKeybinds = false;
}
