package com.gregguns.projectile;

/**
 * The Bio Gun projectile's rules as pure functions: the poison it applies, how a charge level is clamped, and when it
 * may touch blocks. No Minecraft types, both sides.
 *
 * <p>
 * Spec: docs/techguns-spec/04-projectiles-fx/projectiles.md (3.5 BiogunProjectile); fixes PFX-28.
 */
// Deviation (PFX-28): Techguns placed and grew blobs whatever the safe-mode flag said, so only a protection mod's place
// event could stop it. Here blob placement and growth need the same switch every GregGuns world-altering projectile
// uses (explosions.blockDamage, "safe mode"), and a player's shot also asks the protection mods through
// BlockEvent.PlaceEvent.
public final class BioRules {

    /** Poison IV is amplifier 3. */
    public static final int POISON_AMPLIFIER = 3;
    /** Ticks the poison lasts (5 s). */
    public static final int POISON_TICKS = 100;
    /** The highest charge level. */
    public static final int MAX_LEVEL = 3;

    private BioRules() {}

    /**
     * @param level a requested level
     * @return the level clamped to 1..3
     */
    public static int clampLevel(int level) {
        return Math.max(1, Math.min(MAX_LEVEL, level));
    }

    /**
     * @param blockDamageEnabled the {@code explosions.blockDamage} switch, GregGuns' safe mode (true = off)
     * @return true if a blob may be placed or grown by this shot
     */
    public static boolean mayTouchBlocks(boolean blockDamageEnabled) {
        return blockDamageEnabled;
    }
}
