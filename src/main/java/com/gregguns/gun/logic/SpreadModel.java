package com.gregguns.gun.logic;

import java.util.Random;

/**
 * Turns a gun's spread stats and a {@link ShotContext} into per-projectile direction offsets. Pure maths over a
 * caller-supplied {@link Random}, so tests seed it.
 *
 * <p>
 * Server only: the server rolls the offsets when it spawns the projectiles. The client may roll its own for the
 * muzzle effect but never decides hits.
 *
 * <p>
 * Spec: gun-lifecycle.md section 5, "How the projectile applies spread": yaw and pitch each get an independent
 * uniform offset in +-{@code spread * 40} degrees, where spread is {@code accuracy * accuracyBonus} and the accuracy
 * bonus is {@code (1 - armor bonus) * (zoomed ? zoombonus : 1)}.
 */
public final class SpreadModel {

    /** Degrees of maximum offset per unit of spread. */
    public static final double DEGREES_PER_SPREAD = 40.0;

    private SpreadModel() {}

    /** The multiplier a context applies to the gun's base spread. */
    public static double multiplier(GunStats stats, ShotContext ctx) {
        double m = (1.0 - ctx.armorAccuracyBonus()) * ctx.accuracyScale();
        if (ctx.zoomed() && stats.hasScope()) {
            m *= stats.zoomSpreadMultiplier();
        }
        return m;
    }

    /** Effective spread of the main projectile. */
    public static double mainSpread(GunStats stats, ShotContext ctx) {
        return stats.spread() * multiplier(stats, ctx);
    }

    /** Effective spread of the extra pellets (uses the pellet spread, not the main spread). */
    public static double pelletSpread(GunStats stats, ShotContext ctx) {
        return stats.pelletSpread() * multiplier(stats, ctx);
    }

    /** Largest possible offset in degrees, per axis, for a spread value. */
    public static double maxDegrees(double spread) {
        return spread * DEGREES_PER_SPREAD;
    }

    /**
     * Rolls the offsets for one trigger pull. The list holds the main projectile (from the hand named by
     * {@code attackType} for alternating akimbo), the second main projectile for double-shot akimbo (left hand), and
     * then the extra pellets, in that order. Pellets of an in-line gun are shifted forward by {@code i / count} of a
     * tick's travel.
     *
     * @param random     the random source
     * @param stats      the gun
     * @param ctx        the shooter's modifiers
     * @param attackType the hand from {@link ShotResult#attackType()} (1 means left)
     * @return {@code bulletsPerShot} offsets, plus one for double-shot akimbo
     */
    public static ShotOffset[] roll(Random random, GunStats stats, ShotContext ctx, int attackType) {
        boolean dual = stats.akimbo() && stats.doubleShot();
        int pellets = stats.bulletsPerShot() - 1;
        ShotOffset[] out = new ShotOffset[1 + (dual ? 1 : 0) + pellets];
        int n = 0;
        double main = mainSpread(stats, ctx);
        out[n++] = one(random, main, 0.0, stats.akimbo() && !dual && attackType == 1);
        if (dual) {
            out[n++] = one(random, main, 0.0, true);
        }
        double pelletSpread = pelletSpread(stats, ctx);
        for (int i = 1; i <= pellets; i++) {
            double forward = stats.pelletsInLine() ? (double) i / pellets : 0.0;
            out[n++] = one(random, pelletSpread, forward, false);
        }
        return out;
    }

    private static ShotOffset one(Random random, double spread, double forward, boolean left) {
        double max = maxDegrees(spread);
        double yaw = (random.nextDouble() * 2.0 - 1.0) * max;
        double pitch = (random.nextDouble() * 2.0 - 1.0) * max;
        return new ShotOffset(yaw, pitch, forward, left);
    }
}
