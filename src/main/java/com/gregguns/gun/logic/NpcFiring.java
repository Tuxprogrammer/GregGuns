package com.gregguns.gun.logic;

import java.util.Random;

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
