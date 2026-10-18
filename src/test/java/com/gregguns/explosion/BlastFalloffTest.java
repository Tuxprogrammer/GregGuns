package com.gregguns.explosion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BlastFalloffTest {

    private static final double EPS = 1.0E-9;

    @Test
    void stielgranateRampRunsFromTwoAndAHalfToSevenAndAHalfBlocks() {
        assertEquals(20.0, BlastFalloff.damage(20.0, 2.5, 7.5, 0.0), EPS);
        assertEquals(20.0, BlastFalloff.damage(20.0, 2.5, 7.5, 2.5), EPS);
        assertEquals(10.0, BlastFalloff.damage(20.0, 2.5, 7.5, 5.0), EPS);
        assertEquals(0.0, BlastFalloff.damage(20.0, 2.5, 7.5, 7.5), EPS);
        assertEquals(0.0, BlastFalloff.damage(20.0, 2.5, 7.5, 7.6), EPS);
    }

    @Test
    void neverNegativeAndDegenerateRampsStayFinite() {
        assertEquals(0.0, BlastFalloff.damage(-5.0, 1.0, 2.0, 0.5), EPS);
        assertEquals(0.0, BlastFalloff.damage(20.0, 2.5, 7.5, Double.NaN), EPS);
        assertEquals(7.0, BlastFalloff.damage(7.0, 3.0, 3.0, 3.0), EPS);
    }

    @Test
    void mobGriefingOnlyGatesBlastsNotCausedByPlayers() {
        assertTrue(ExplosionRules.mayBreakBlocks(true, true, false));
        assertFalse(ExplosionRules.mayBreakBlocks(true, false, false));
        assertTrue(ExplosionRules.mayBreakBlocks(true, false, true));
        assertFalse(ExplosionRules.mayBreakBlocks(false, true, true));
    }

    @Test
    void auditLineNamesDimensionPositionRadiusShooterAndWeapon() {
        assertEquals(
            "dim=0 pos=(1.5, 64.0, -3.5) radius=5.0 shooter=Alex weapon=stielgranate blocks=12",
            ExplosionRules.logLine(0, 1.5, 64.0, -3.5, 5.0, "Alex", "stielgranate", 12));
        assertTrue(
            ExplosionRules.logLine(-1, 0, 0, 0, 2, null, "x", 1)
                .contains("shooter=none"));
    }
}
