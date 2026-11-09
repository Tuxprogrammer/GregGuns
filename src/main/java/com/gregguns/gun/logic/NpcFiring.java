package com.gregguns.gun.logic;

import java.util.Random;

import com.gregguns.api.Tier;

/**
 * The NPC and turret hooks: difficulty scaling and the ranged-AI timing. NPCs never use ammo, never reload and
 * never zoom, so they bypass {@link GunController}: the glue asks {@link #scaling} for the factors, rolls the
 * offsets with {@link #roll} and spawns the projectiles. Server only.
 *
 * <p>
 * Deviations: the difficulty accuracy factor is applied once (Techguns applies it twice, GUN-06) and NPCs do not get
 * the zoomed-spread bonus (GUN-07). The damage factor is exposed for the combat module to apply; this class does no
 * damage maths.
 *
 * <p>
 * GregGuns adds two difficulty knobs on top (design 0027): a per-tier damage multiplier with an extra one for elite
 * shooters ({@link #shotDamageScale}, D358), and an aim error that grows with distance past a free range
 * ({@link #distanceAimError}, D359). Both apply to hostile NPCs and turrets only; a player's own spread is untouched.
 *
 * <p>
 * Spec: gun-lifecycle.md section 6.
 */
public final class NpcFiring {

    /** The scale every non-turret NPC passes before the difficulty factor ("dmgScale"). */
    public static final double NPC_DAMAGE_SCALE = 0.5;

    /** World difficulty, matching vanilla's ids 0 to 3. */
    public enum Difficulty {

        PEACEFUL(0.0, 1.3),
        EASY(0.5, 1.3),
        NORMAL(0.6, 1.15),
        HARD(0.75, 1.0);

        private final double damageFactor;
        private final double accuracyFactor;

        Difficulty(double damageFactor, double accuracyFactor) {
            this.damageFactor = damageFactor;
            this.accuracyFactor = accuracyFactor;
        }

        /** Maps vanilla's difficulty id (0 to 3); out-of-range ids clamp. */
        public static Difficulty fromVanillaId(int id) {
            Difficulty[] all = values();
            return all[Math.max(0, Math.min(all.length - 1, id))];
        }
    }

    /** Factors for one NPC shot. Immutable. */
    public static final class Scaling {

        private final double damageFactor;
        private final double accuracyFactor;

        Scaling(double damageFactor, double accuracyFactor) {
            this.damageFactor = damageFactor;
            this.accuracyFactor = accuracyFactor;
        }

        /** Multiplier on the gun's damage, for the combat module. */
        public double damageFactor() {
            return damageFactor;
        }

        /** Multiplier on the gun's spread (above 1 is less accurate). */
        public double accuracyFactor() {
            return accuracyFactor;
        }

        /** The shot context for {@link SpreadModel}. */
        public ShotContext context() {
            return ShotContext.npc(accuracyFactor);
        }
    }

    private NpcFiring() {}

    /**
     * The damage multiplier an NPC passes to the launcher: the scaling's factor, or 0 when
     * {@code npcs.gunDamageEnabled}
     * is off (the bullets still fly but hurt nothing).
     */
    public static float damageScale(Scaling scaling, boolean gunDamageEnabled) {
        return gunDamageEnabled ? (float) scaling.damageFactor() : 0.0F;
    }

    /**
     * Difficulty factors. Turrets use 1.0 for both; other NPCs use {@link #NPC_DAMAGE_SCALE} times the difficulty
     * damage factor, and the difficulty accuracy factor once.
     */
    public static Scaling scaling(Difficulty difficulty, boolean turret) {
        if (turret) {
            return new Scaling(1.0, 1.0);
        }
        return new Scaling(NPC_DAMAGE_SCALE * difficulty.damageFactor, difficulty.accuracyFactor);
    }

    /** Rolls the projectile offsets for one NPC shot (hand 0). */
    public static ShotOffset[] roll(Random random, GunStats stats, Scaling scaling) {
        return SpreadModel.roll(random, stats, scaling.context(), 0);
    }

    /**
     * Rolls one NPC shot with a distance aim error on top of the gun's spread. The error is one draw, uniform within
     * {@code +-aimErrorDegrees} on each axis, shared by every projectile of the shot: the NPC misjudges where the
     * target is, so a shotgun's pellets or a ring volley move together and keep their own pattern.
     *
     * @param aimErrorDegrees the largest aim error, from {@link #distanceAimError}; 0 or less adds none and draws
     *                        nothing extra from {@code random}
     */
    public static ShotOffset[] roll(Random random, GunStats stats, Scaling scaling, double aimErrorDegrees) {
        ShotOffset[] offsets = roll(random, stats, scaling);
        if (!(aimErrorDegrees > 0.0)) {
            return offsets;
        }
        double yaw = (random.nextDouble() * 2.0 - 1.0) * aimErrorDegrees;
        double pitch = (random.nextDouble() * 2.0 - 1.0) * aimErrorDegrees;
        for (int i = 0; i < offsets.length; i++) {
            ShotOffset o = offsets[i];
            offsets[i] = new ShotOffset(
                o.yawDegrees() + yaw,
                o.pitchDegrees() + pitch,
                o.forwardTickFraction(),
                o.leftHand(),
                o.volley());
        }
        return offsets;
    }

    /**
     * The largest aim error distance adds to an NPC shot (D359): none within {@code freeRange}, then
     * {@code degreesPerBlock} more for each block beyond it, capped at {@code maxDegrees}.
     *
     * @param distance        distance to the target in blocks
     * @param freeRange       {@code combat.npcAccuracyRange}
     * @param degreesPerBlock {@code combat.npcSpreadPerBlock}
     * @param maxDegrees      {@code combat.npcMaxSpread}
     * @return degrees, at least 0; 0 for any non-finite input
     */
    public static double distanceAimError(double distance, double freeRange, double degreesPerBlock,
        double maxDegrees) {
        double error = Math.min(Math.max(0.0, distance - freeRange) * degreesPerBlock, maxDegrees);
        return Double.isNaN(error) || Double.isInfinite(error) || error < 0.0 ? 0.0 : error;
    }

    /**
     * The tier multiplier for a shot from a gun of this tier (D358).
     *
     * @param tier    the gun's tier; null counts as the lowest
     * @param perTier one multiplier per {@link Tier}, in enum order; a missing entry is 1
     * @return the multiplier, at least 0
     */
    public static double tierDamage(Tier tier, double[] perTier) {
        int i = tier == null ? 0 : tier.ordinal();
        if (perTier == null || i >= perTier.length) {
            return 1.0;
        }
        double m = perTier[i];
        return Double.isNaN(m) || m < 0.0 ? 0.0 : m;
    }

    /**
     * The damage scale an NPC or turret passes to the launcher (D358): the difficulty scale times the tier multiplier,
     * times the elite multiplier for an infernal NPC or a boss.
     *
     * @param base            the difficulty scale, {@link #damageScale} (0 when NPC gun damage is off)
     * @param tierMultiplier  {@link #tierDamage}
     * @param elite           true for an Infernal Mobs elite, ultra or infernal, or a boss
     * @param eliteMultiplier {@code combat.npcEliteDamage}
     * @return the scale, at least 0
     */
    public static float shotDamageScale(double base, double tierMultiplier, boolean elite, double eliteMultiplier) {
        double scale = base * tierMultiplier * (elite ? eliteMultiplier : 1.0);
        return Double.isNaN(scale) || scale < 0.0 ? 0.0F : (float) scale;
    }

    /**
     * Ticks until the NPC's next attack once its burst is spent: {@code floor(f * (attackTime - attackTime/3) +
     * attackTime/3)} with {@code f = distance / range} clamped to {@code [0.1, 1]} and integer division for the
     * variance. Within a burst use {@link GunStats#aiBurstDelay()} instead.
     *
     * @param distance distance to the target in blocks
     */
    public static int nextAttackDelay(GunStats stats, double distance) {
        double f = Math.max(0.1, Math.min(1.0, distance / stats.aiRange()));
        int variance = stats.aiAttackTime() / 3;
        return (int) Math.floor(f * (stats.aiAttackTime() - variance) + variance);
    }

    /** True if the target is in range: the comparison is on squared distances, as the AI does. */
    public static boolean inRange(GunStats stats, double distanceSquared) {
        return distanceSquared <= stats.aiRange() * stats.aiRange();
    }
}
