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

    @Config.Comment({ "Whether a GregTech machine caught in a GregGuns blast reacts as it does to any explosion",
        "(charged machines can blow up and chain). When false the machine is left standing. Default: true." })
    public static boolean machineExplosions = true;

    @Config.Comment({
        "Whether a nuclear blast (the Nuke Launcher, the ICBM nuclear warhead) leaves fallout: irradiated",
        "surface blocks. Only blasts that broke terrain leave any. Default: true." })
    public static boolean fallout = true;

    @Config.Comment({ "The most blocks one nuclear blast may turn into irradiated earth. The conversion chance is",
        "scaled so the expected total stays under this, and the count is a hard stop. Default: 6000." })
    @Config.RangeInt(min = 0, max = 100000)
    public static int falloutMaxBlocks = 6000;

    @Config.Comment({ "The largest fallout radius, blocks, whatever the blast radius. Default: 96." })
    @Config.RangeInt(min = 8, max = 256)
    public static int falloutMaxRadius = 96;

    @Config.Comment({ "The most columns fallout scans in one server tick, across all blasts. Default: 400." })
    @Config.RangeInt(min = 1, max = 5000)
    public static int falloutColumnsPerTick = 400;

    @Config.Comment({ "The most milliseconds fallout may spend in one server tick. Default: 3." })
    @Config.RangeInt(min = 1, max = 20)
    public static int falloutBudgetMs = 3;

    @Config.Comment({ "Minutes of game time before irradiated earth reverts to the block it was. Default: 60." })
    @Config.RangeInt(min = 1, max = 10080)
    public static int falloutDecayMinutes = 60;

    @Config.Comment({ "The most irradiated blocks that revert per tick in one world. Default: 32." })
    @Config.RangeInt(min = 1, max = 1000)
    public static int falloutRevertPerTick = 32;

    @Config.Comment({ "GregTech radioactivity level applied about once a second to an unprotected player standing on",
        "or beside irradiated earth; 0 turns exposure off. Default: 2." })
    @Config.RangeInt(min = 0, max = 10)
    public static int falloutRadiationLevel = 2;

    @Config.Comment({
        "The most fallout goo blocks one blast makes (D21); goo is rare and near ground zero, and while it",
        "stands in a chunk, that chunk's irradiated earth does not decay. Default: 24." })
    @Config.RangeInt(min = 0, max = 500)
    public static int falloutMaxGoo = 24;
}
