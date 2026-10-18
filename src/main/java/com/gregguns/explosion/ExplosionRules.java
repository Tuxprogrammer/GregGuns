package com.gregguns.explosion;

import java.util.Locale;

/**
 * The decisions around an explosion that need no world: whether it may break blocks, and the audit log line. No
 * Minecraft types, both sides.
 */
// Spec: docs/gregguns-spec/04-projectiles-fx/explosions-pack.md (GGS-04-14, GGS-04-15)
public final class ExplosionRules {

    /** Name of the logger that receives one line per explosion that broke blocks. */
    public static final String LOGGER_NAME = "gregguns.explosions";

    private ExplosionRules() {}

    /**
     * Whether an explosion may break blocks at all.
     *
     * @param configBlockDamage the {@code explosions.blockDamage} setting
     * @param causedByPlayer    true when a player (or fake player) caused it
     * @param mobGriefing       the world's {@code mobGriefing} gamerule
     * @return true when blocks may be removed; an NPC-caused blast also needs {@code mobGriefing} (GGS-04-14)
     */
    public static boolean mayBreakBlocks(boolean configBlockDamage, boolean causedByPlayer, boolean mobGriefing) {
        return configBlockDamage && (causedByPlayer || mobGriefing);
    }

    /**
     * @param dimension the dimension id
     * @param x         centre X
     * @param y         centre Y
     * @param z         centre Z
     * @param radius    the blast radius, blocks
     * @param shooter   the shooter's name, or {@code null}
     * @param weapon    the weapon's name
     * @param blocks    how many blocks were removed
     * @return the audit line: dimension, position, radius, shooter, weapon and block count (GGS-04-15)
     */
    public static String logLine(int dimension, double x, double y, double z, double radius, String shooter,
        String weapon, int blocks) {
        return String.format(
            Locale.ROOT,
            "dim=%d pos=(%.1f, %.1f, %.1f) radius=%.1f shooter=%s weapon=%s blocks=%d",
            dimension,
            x,
            y,
            z,
            radius,
            shooter == null ? "none" : shooter,
            weapon,
            blocks);
    }
}
