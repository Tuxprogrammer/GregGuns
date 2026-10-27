package com.gregguns.explosion.nuclear;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FalloutMathTest {

    @Test
    void expectedConversionsStayUnderTheBudget() {
        // The peak chance is chosen so the expected total is 80 percent of the cap, for any radius.
        for (int radius : new int[] { 15, 48, 96 }) {
            double peak = FalloutMath.peakChance(radius, 6000);
            double expected = 0.0;
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    expected += FalloutMath.chanceAt(peak, Math.sqrt(x * x + z * z), radius)
                        * FalloutMath.AVERAGE_DEPTH;
                }
            }
            assertTrue(expected <= 6000 * 0.9, "radius " + radius + " expects " + expected);
        }
    }

    @Test
    void chanceFallsWithDistanceAndIsZeroOutsideTheDisc() {
        double peak = FalloutMath.peakChance(48, 6000);
        assertTrue(peak <= FalloutMath.MAX_CHANCE);
        assertTrue(FalloutMath.chanceAt(peak, 0, 48) > FalloutMath.chanceAt(peak, 30, 48));
        assertEquals(0.0, FalloutMath.chanceAt(peak, 49, 48));
        assertEquals(0.0, FalloutMath.chanceAt(peak, Double.NaN, 48));
        assertEquals(0.0, FalloutMath.peakChance(48, 0));
    }

    @Test
    void rollsAreDeterministicAndInRange() {
        for (int i = 0; i < 1000; i++) {
            double a = FalloutMath.roll(42L, i, -i, 0);
            assertTrue(a >= 0.0 && a < 1.0);
            assertEquals(a, FalloutMath.roll(42L, i, -i, 0));
        }
        assertTrue(FalloutMath.roll(1L, 5, 5, 0) != FalloutMath.roll(2L, 5, 5, 0));
    }

    @Test
    void falloutRadiusFollowsTheBlastAndTheCap() {
        assertEquals(15, NuclearScale.falloutRadius(10.0, 96)); // the Nuke Launcher
        assertEquals(96, NuclearScale.falloutRadius(64.0, 96)); // a full nuclear warhead
        assertEquals(40, NuclearScale.falloutRadius(64.0, 40));
        assertEquals(1, NuclearScale.falloutRadius(0.0, 96));
        assertEquals(38.4, NuclearScale.killRadius(64.0), 1e-9);
    }
}
