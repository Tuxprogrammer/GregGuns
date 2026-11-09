package com.gregguns.gun.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

import com.gregguns.api.Tier;
import com.gregguns.gun.logic.NpcFiring.Difficulty;
import com.gregguns.gun.logic.NpcFiring.Scaling;

class NpcFiringTest {

    private static final GunStats PISTOL = GunCatalog.PISTOL;

    @Test
    void gunDamageFlagZeroesTheLaunchScaleOnly() {
        Scaling hard = NpcFiring.scaling(Difficulty.HARD, false);
        assertEquals(0.375F, NpcFiring.damageScale(hard, true), 1e-6F);
        assertEquals(0.0F, NpcFiring.damageScale(hard, false));
    }

    @Test
    void difficultyTableMatchesTheSpec() {
        assertEquals(
            0.0,
            NpcFiring.scaling(Difficulty.PEACEFUL, false)
                .damageFactor());
        assertEquals(
            0.25,
            NpcFiring.scaling(Difficulty.EASY, false)
                .damageFactor(),
            1e-12);
        assertEquals(
            0.30,
            NpcFiring.scaling(Difficulty.NORMAL, false)
                .damageFactor(),
            1e-12);
        assertEquals(
            0.375,
            NpcFiring.scaling(Difficulty.HARD, false)
                .damageFactor(),
            1e-12);
        assertEquals(
            1.3,
            NpcFiring.scaling(Difficulty.EASY, false)
                .accuracyFactor());
        assertEquals(
            1.15,
            NpcFiring.scaling(Difficulty.NORMAL, false)
                .accuracyFactor());
        assertEquals(
            1.0,
            NpcFiring.scaling(Difficulty.HARD, false)
                .accuracyFactor());
    }

    @Test
    void turretsUseUnitFactors() {
        Scaling s = NpcFiring.scaling(Difficulty.HARD, true);
        assertEquals(1.0, s.damageFactor());
        assertEquals(1.0, s.accuracyFactor());
    }

    @Test
    void doesNotReproduceGUN06_accuracyFactorIsAppliedOnce() {
        Scaling s = NpcFiring.scaling(Difficulty.EASY, false);
        assertEquals(PISTOL.spread() * 1.3, SpreadModel.mainSpread(PISTOL, s.context()), 1e-12);
    }

    @Test
    void doesNotReproduceGUN07_npcsGetNoZoomBonus() {
        GunStats scoped = GunStats.builder("s")
            .ammoType("a")
            .magazine(5)
            .spread(0.1)
            .zoom(0.5, 0.5)
            .build();
        Scaling s = NpcFiring.scaling(Difficulty.HARD, false);
        assertEquals(0.1, SpreadModel.mainSpread(scoped, s.context()), 1e-12);
    }

    @Test
    void npcRollsAreSeededAndBounded() {
        Scaling s = NpcFiring.scaling(Difficulty.EASY, false);
        ShotOffset[] a = NpcFiring.roll(new Random(5), PISTOL, s);
        ShotOffset[] b = NpcFiring.roll(new Random(5), PISTOL, s);
        assertEquals(a[0].yawDegrees(), b[0].yawDegrees());
        assertTrue(Math.abs(a[0].yawDegrees()) <= 1.3);
    }

    @Test
    void attackDelayScalesWithDistanceAndClamps() {
        // attackTime 30, variance 10, range 24
        assertEquals(12, NpcFiring.nextAttackDelay(PISTOL, 0)); // f clamps to 0.1: floor(0.1*20+10)
        assertEquals(20, NpcFiring.nextAttackDelay(PISTOL, 12)); // f 0.5
        assertEquals(30, NpcFiring.nextAttackDelay(PISTOL, 24));
        assertEquals(30, NpcFiring.nextAttackDelay(PISTOL, 100));
    }

    @Test
    void rangeIsComparedSquared() {
        assertTrue(NpcFiring.inRange(PISTOL, 24 * 24));
        assertFalse(NpcFiring.inRange(PISTOL, 24 * 24 + 1));
    }

    @Test
    void difficultyIdsClamp() {
        assertEquals(Difficulty.PEACEFUL, Difficulty.fromVanillaId(-1));
        assertEquals(Difficulty.NORMAL, Difficulty.fromVanillaId(2));
        assertEquals(Difficulty.HARD, Difficulty.fromVanillaId(9));
    }

    @Test
    void infiniteAmmoServesNpcs() {
        assertEquals(Integer.MAX_VALUE, AmmoInventory.INFINITE.count("anything"));
        assertEquals(5, AmmoInventory.INFINITE.withdraw("anything", 5));
    }

    // --- D358 and D359: the combat config's NPC knobs (design 0027) ------------------------------------------------

    /** The shipped defaults of combat.npcDamage*, in Tier order (CombatConfigTest pins them against the config). */
    static final double[] DEFAULT_TIER_DAMAGE = { 1.0, 1.0, 1.0, 0.85, 0.7, 0.55, 0.45, 0.4 };

    @Test
    void aimErrorIsZeroWithinTheFreeRange() {
        assertEquals(0.0, NpcFiring.distanceAimError(0.0, 5.0, 0.12, 4.0));
        assertEquals(0.0, NpcFiring.distanceAimError(5.0, 5.0, 0.12, 4.0));
    }

    @Test
    void aimErrorGrowsLinearlyPastTheFreeRangeUpToTheCap() {
        assertEquals(0.6, NpcFiring.distanceAimError(10.0, 5.0, 0.12, 4.0), 1e-9);
        assertEquals(1.8, NpcFiring.distanceAimError(20.0, 5.0, 0.12, 4.0), 1e-9);
        assertEquals(4.0, NpcFiring.distanceAimError(60.0, 5.0, 0.12, 4.0), 1e-9);
        assertEquals(4.0, NpcFiring.distanceAimError(1000.0, 5.0, 0.12, 4.0), 1e-9);
        double last = -1.0;
        for (int d = 0; d <= 80; d++) {
            double e = NpcFiring.distanceAimError(d, 5.0, 0.12, 4.0);
            assertTrue(e >= last, "never shrinks with distance, at " + d);
            last = e;
        }
    }

    @Test
    void aimErrorOffOrBrokenInputsAddNothing() {
        assertEquals(0.0, NpcFiring.distanceAimError(50.0, 5.0, 0.0, 4.0), "0 per block turns it off");
        assertEquals(0.0, NpcFiring.distanceAimError(50.0, 5.0, 0.12, 0.0), "a 0 cap turns it off");
        assertEquals(0.0, NpcFiring.distanceAimError(Double.NaN, 5.0, 0.12, 4.0));
        assertEquals(0.0, NpcFiring.distanceAimError(Double.POSITIVE_INFINITY, 5.0, 0.12, Double.POSITIVE_INFINITY));
    }

    @Test
    void aimErrorMovesEveryProjectileOfAShotTogetherAndStaysInBounds() {
        Scaling hard = NpcFiring.scaling(Difficulty.HARD, false);
        GunStats shotgun = GunCatalog.SAWED_OFF;
        for (long seed = 0; seed < 200; seed++) {
            ShotOffset[] plain = NpcFiring.roll(new Random(seed), shotgun, hard);
            ShotOffset[] aimed = NpcFiring.roll(new Random(seed), shotgun, hard, 3.0);
            assertEquals(plain.length, aimed.length);
            double dYaw = aimed[0].yawDegrees() - plain[0].yawDegrees();
            double dPitch = aimed[0].pitchDegrees() - plain[0].pitchDegrees();
            assertTrue(Math.abs(dYaw) <= 3.0 && Math.abs(dPitch) <= 3.0, "within +-3 degrees, seed " + seed);
            for (int i = 0; i < plain.length; i++) {
                assertEquals(dYaw, aimed[i].yawDegrees() - plain[i].yawDegrees(), 1e-9, "same yaw shift");
                assertEquals(dPitch, aimed[i].pitchDegrees() - plain[i].pitchDegrees(), 1e-9, "same pitch shift");
                assertEquals(plain[i].forwardTickFraction(), aimed[i].forwardTickFraction());
            }
        }
    }

    @Test
    void aimErrorSpreadsShotsAcrossTheWholeBand() {
        // Uniform within +-e: over many shots both ends of the band are reached, so far shots really do miss.
        Scaling hard = NpcFiring.scaling(Difficulty.HARD, false);
        double min = 0.0;
        double max = 0.0;
        for (int i = 0; i < 2000; i++) {
            double base = NpcFiring.roll(new Random(i), PISTOL, hard)[0].yawDegrees();
            double yaw = NpcFiring.roll(new Random(i), PISTOL, hard, 4.0)[0].yawDegrees() - base;
            min = Math.min(min, yaw);
            max = Math.max(max, yaw);
        }
        assertTrue(min < -3.8 && max > 3.8, "band reached: " + min + " to " + max);
    }

    @Test
    void noAimErrorLeavesTheRollAndTheRandomStreamAlone() {
        Scaling normal = NpcFiring.scaling(Difficulty.NORMAL, false);
        Random a = new Random(7);
        Random b = new Random(7);
        ShotOffset plain = NpcFiring.roll(a, PISTOL, normal)[0];
        ShotOffset none = NpcFiring.roll(b, PISTOL, normal, 0.0)[0];
        assertEquals(plain.yawDegrees(), none.yawDegrees());
        assertEquals(plain.pitchDegrees(), none.pitchDegrees());
        assertEquals(a.nextLong(), b.nextLong(), "no extra draw");
    }

    @Test
    void tierDamageFallsWithTierAndNeverRises() {
        double last = Double.MAX_VALUE;
        for (Tier t : Tier.values()) {
            double m = NpcFiring.tierDamage(t, DEFAULT_TIER_DAMAGE);
            assertTrue(m <= last, t + " should not hit harder than the tier below");
            last = m;
        }
        assertEquals(1.0, NpcFiring.tierDamage(Tier.MV, DEFAULT_TIER_DAMAGE));
        assertEquals(0.55, NpcFiring.tierDamage(Tier.IV, DEFAULT_TIER_DAMAGE));
    }

    @Test
    void tierDamageToleratesShortTablesAndBadValues() {
        assertEquals(1.0, NpcFiring.tierDamage(Tier.ZPM, new double[] { 0.5 }));
        assertEquals(1.0, NpcFiring.tierDamage(Tier.LV, null));
        assertEquals(0.5, NpcFiring.tierDamage(null, new double[] { 0.5 }), "null counts as the lowest tier");
        assertEquals(0.0, NpcFiring.tierDamage(Tier.STEAM, new double[] { -2.0 }));
        assertEquals(0.0, NpcFiring.tierDamage(Tier.STEAM, new double[] { Double.NaN }));
    }

    @Test
    void shotDamageScaleMultipliesDifficultyTierAndElite() {
        double normal = NpcFiring.damageScale(NpcFiring.scaling(Difficulty.NORMAL, false), true); // 0.3
        assertEquals(0.3F, NpcFiring.shotDamageScale(normal, 1.0, false, 0.75), 1e-6F);
        assertEquals(0.165F, NpcFiring.shotDamageScale(normal, 0.55, false, 0.75), 1e-6F);
        assertEquals(0.12375F, NpcFiring.shotDamageScale(normal, 0.55, true, 0.75), 1e-6F);
        assertEquals(0.0F, NpcFiring.shotDamageScale(0.0, 0.55, true, 0.75), "gun damage off stays off");
        assertEquals(0.0F, NpcFiring.shotDamageScale(Double.NaN, 1.0, false, 1.0));
    }

    @Test
    void highTierNpcHitsTakeMoreHitsToKillThanIn040() {
        // Design 0027's headline: a Normal-difficulty IV Blaster Rifle (npcDamage 10, penetration 0.2) against an
        // HV NanoSuit (90%) took 24 hits in 0.4.0; with the new tier multiplier (0.55) and the NPC penetration
        // multiplier at 0.5 it takes at least 60.
        double old = hitsToKill(10 * 0.3, 0.2, 0.90);
        double now = hitsToKill(10 * 0.3 * NpcFiring.tierDamage(Tier.IV, DEFAULT_TIER_DAMAGE), 0.2 * 0.5, 0.90);
        assertEquals(24.0, old);
        assertTrue(now >= 60.0, "now " + now);
    }

    private static double hitsToKill(double raw, double penetration, double absorb) {
        double taken = raw * (penetration + (1.0 - penetration) * (1.0 - absorb));
        return Math.ceil(20.0 / taken);
    }
}
