package com.gregguns.core.config;

import com.gtnewhorizon.gtnhlib.config.Config;

/**
 * Explosion switches, the {@code explosions} category of {@code config/gregguns.cfg}. Fields only; no Minecraft
 * types. Synced: the server's values decide what a blast does.
 */
// Spec: docs/gregguns-spec/04-projectiles-fx/explosions-pack.md; docs/techguns-spec/04-projectiles-fx/explosions.md
@Config(modid = "gregguns", category = "explosions")
@Config.Sync
public class ExplosionsConfig {

    @Config.Comment({ "Whether GregGuns explosions (grenades, later rockets) remove blocks. When false they still",
        "hurt entities. Techguns' hand grenades never broke blocks, so the default keeps that. Protection mods",
        "that filter explosions still apply when this is true. Default: false." })
    public static boolean blockDamage = false;

    @Config.Comment({ "Whether the player who set off an explosion can be hurt by it (Techguns made the shooter",
        "immune, PFX-25). Default: true." })
    public static boolean selfDamage = true;
}
