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

    @Config.Comment({ "Enables the op-only /gregguns dumprecipes <file> command, which writes every crafting and GT",
        "recipe to JSON for the recipe-clash review. Leave off in play. Default: false." })
    public static boolean recipeDump = false;
}
