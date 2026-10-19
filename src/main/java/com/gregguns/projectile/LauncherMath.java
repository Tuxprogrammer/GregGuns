package com.gregguns.projectile;

import com.gregguns.explosion.BlastFalloff;
import com.gregguns.explosion.ExplosionRules;

/**
 * The pure rules of the two launcher projectiles, the rocket and the 40 mm grenade: blast radii, the direct-hit and
 * splash damage split, how far a bounce shortens the grenade's fuse, and when a blast may remove blocks. No Minecraft
 * types, so unit tests cover them. Both sides.
 *
 * <pre>
 * {@code
 * LauncherMath.splashDamage(50.0, 1.75, 2.0);   // 50.0, inside the half radius
 * LauncherMath.bounceFuse(100);                 // 80
 * }
 * </pre>
 */
// Spec: docs/techguns-spec/04-projectiles-fx/projectiles.md (3.4, 3.7); docs/gregguns-spec/02-guns/index.md (launchers,
// batch B5)
public final class LauncherMath {

    /** Ticks a launcher grenade's fuse loses with every bounce. */
    public static final int BOUNCE_FUSE_PENALTY = 20;
    /** Bounces a 40 mm grenade survives. */
    public static final int GRENADE_BOUNCES = 2;
    /** Full-damage share of the blast radius. */
    public static final double INNER_SHARE = 0.5;
    /** Zero-damage multiple of the blast radius. */
    public static final double OUTER_SHARE = 1.5;
    /** How far back from a block surface a rocket explodes, blocks, so its blast is not centred inside the wall. */
    public static final double STANDOFF = 0.15;

    private LauncherMath() {}

    /**
     * @param radius the gun's blast radius, blocks
     * @return the radius of full damage
     */
    public static double innerRadius(double radius) {
        return radius * INNER_SHARE;
    }

    /**
     * @param radius the gun's blast radius, blocks
     * @return the radius where damage reaches zero
     */
    public static double outerRadius(double radius) {
        return radius * OUTER_SHARE;
    }

    /**
     * @param damage   the gun's damage, half hearts
     * @param radius   the gun's blast radius, blocks
     * @param distance the target's distance from the blast centre, blocks
     * @return what the blast deals at that distance: full inside half the radius, none beyond one and a half
     */
    public static double splashDamage(double damage, double radius, double distance) {
        return BlastFalloff.damage(damage, innerRadius(radius), outerRadius(radius), distance);
    }

    /**
     * The rocket's direct hit: the gun's damage in full, with no drop-off and no distance falloff, because the rocket
     * is on the target. The target is then left out of the splash, so it takes exactly this once.
     *
     * @param damage the gun's damage, scaled by the shooter's bonus
     * @return the direct-hit damage
     */
    public static double directDamage(double damage) {
        return damage;
    }

    /**
     * @param fuse the grenade's fuse before a bounce, ticks
     * @return the fuse after it, never below 1 (the grenade then explodes on its next update)
     */
    public static int bounceFuse(int fuse) {
        return Math.max(1, fuse - BOUNCE_FUSE_PENALTY);
    }

    /**
     * @param fuse    a grenade's fuse, ticks
     * @param bounces how many times it bounces on the way
     * @return the fuse left after that many bounces
     */
    public static int fuseAfterBounces(int fuse, int bounces) {
        int left = fuse;
        for (int i = 0; i < bounces; i++) {
            left = bounceFuse(left);
        }
        return left;
    }

    /**
     * How far a projectile that starts level has fallen under gravity and drag, the way {@link Ballistics} steps it
     * (position moves by the current motion, then drag and gravity change the motion).
     *
     * @param gravity blocks per tick squared
     * @param ticks   ticks of flight
     * @return how far it has fallen below its start, blocks (positive is down)
     */
    public static double fallAfter(double gravity, int ticks) {
        double drag = Ballistics.AIR_DRAG;
        double motionY = 0.0;
        double fallen = 0.0;
        for (int i = 0; i < ticks; i++) {
            fallen -= motionY;
            motionY = Ballistics.nextMotionY(motionY, drag, gravity);
        }
        return fallen;
    }

    /**
     * @param configBlockDamage the {@code explosions.blockDamage} setting
     * @param firedByPlayer     true when a player (or fake player) fired the launcher
     * @param mobGriefing       the world's {@code mobGriefing} rule
     * @return true when the blast may remove blocks: only with the config on, and an NPC also needs mobGriefing
     */
    public static boolean blastMayBreakBlocks(boolean configBlockDamage, boolean firedByPlayer, boolean mobGriefing) {
        return ExplosionRules.mayBreakBlocks(configBlockDamage, firedByPlayer, mobGriefing);
    }
}
