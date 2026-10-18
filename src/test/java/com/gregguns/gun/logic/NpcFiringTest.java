package com.gregguns.gun.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

import com.gregguns.gun.logic.NpcFiring.Difficulty;
import com.gregguns.gun.logic.NpcFiring.Scaling;

class NpcFiringTest {

    private static final GunStats PISTOL = GunCatalog.PISTOL;

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
}
