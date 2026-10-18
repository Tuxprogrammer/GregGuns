package com.gregguns.projectile;

/**
 * The yes/no rules of bullet collision, kept free of Minecraft types so they can be tested. Plain Java.
 */
// Spec: docs/techguns-spec/04-projectiles-fx/projectiles.md (2.3 steps 3 and 4, 2.5 Block hit)
public final class HitRules {

    /** Ticks after launch during which a bullet cannot hit the entity that fired it. */
    public static final int SHOOTER_GRACE_TICKS = 5;

    /** The fraction used when no block was hit: beyond the end of any segment. */
    public static final double NO_BLOCK = Double.POSITIVE_INFINITY;

    private HitRules() {}

    /**
     * Whether the bullet may hit its own shooter yet.
     *
     * @param ticksExisted the bullet's age in ticks
     * @return {@code true} once the grace period is over
     */
    // Deviation: Techguns never lets a bullet hit its shooter. A lobbed bullet can fall back onto its shooter, so
    // GregGuns only protects the first ticks, where the muzzle is inside the shooter's own box.
    public static boolean mayHitShooter(int ticksExisted) {
        return ticksExisted > SHOOTER_GRACE_TICKS;
    }

    /**
     * Whether an entity on the segment is hit rather than hidden behind a block.
     *
     * @param entityFraction where the segment enters the entity's box, 0 to 1
     * @param blockFraction  where the segment meets a solid block, or {@link #NO_BLOCK}
     * @return {@code true} if the entity is strictly nearer than the block
     */
    // Bug PFX-23: Techguns' hitscan helper preferred an entity over a nearer block. Here the nearer one wins.
    public static boolean entityBeforeBlock(double entityFraction, double blockFraction) {
        return entityFraction < blockFraction;
    }

    /**
     * Whether a block the segment meets stops the bullet.
     *
     * @param blockIsPassable the block's {@code getBlocksMovement}: true for tall grass, flowers and open doors
     * @return {@code true} for a solid block
     */
    // Bug PFX-27: some Techguns projectiles stopped on grass and flowers. A bullet flies through passable blocks.
    public static boolean blockStopsBullet(boolean blockIsPassable) {
        return !blockIsPassable;
    }
}
