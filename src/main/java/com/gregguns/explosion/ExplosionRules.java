package com.gregguns.explosion;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The decisions around an explosion that need no world: whether it may break blocks, and the audit log line. No
 * Minecraft types, both sides.
 */
// Spec: docs/gregguns-spec/04-projectiles-fx/explosions-pack.md (GGS-04-14, GGS-04-15)
public final class ExplosionRules {

    /** Name of the logger that receives one line per explosion that broke blocks. */
    public static final String LOGGER_NAME = "gregguns.explosions";

    /** Most blocks one blast breaks in a single tick; the rest wait for later ticks (nuke scale). */
    public static final int BLOCKS_PER_TICK = 1500;

    /** A blast with a vanilla power at or above this is a nuke: it sends the long-range FX packet. */
    public static final float NUKE_FX_MIN_SIZE = 8.0F;

    /** Players within this many blocks of a nuke receive its FX packet (vanilla's 64 would miss the flash). */
    public static final double NUKE_FX_RANGE = 150.0;

    private ExplosionRules() {}

    /**
     * The entities a blast may hurt: everything in range, minus its owner when {@code explosions.selfDamage} is off.
     * With it on the owner stays in the list (fixes PFX-25: Techguns made the shooter immune).
     *
     * @param candidates entities in range
     * @param owner      who caused the blast, may be {@code null}
     * @param selfDamage the {@code explosions.selfDamage} setting
     * @param <T>        entity type
     * @return a new list
     */
    public static <T> List<T> victims(List<? extends T> candidates, T owner, boolean selfDamage) {
        List<T> out = new ArrayList<>(candidates);
        if (!selfDamage) {
            out.remove(owner);
        }
        return out;
    }

    /**
     * @param distanceSquared squared distance from a nuke to a player, blocks squared
     * @return true when the player is close enough to get the FX packet
     */
    public static boolean inNukeFxRange(double distanceSquared) {
        return distanceSquared <= NUKE_FX_RANGE * NUKE_FX_RANGE;
    }

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
