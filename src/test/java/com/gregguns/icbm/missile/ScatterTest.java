package com.gregguns.icbm.missile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

class ScatterTest {

    @Test
    void theBaseHalfWidthAtZeroDistance() {
        assertEquals(12.0, Scatter.halfWidth(12, 0, 0, 1000), 1e-9);
    }

    @Test
    void eachGuidanceTierRemovesAQuarterOfTheBase() {
        assertEquals(9.0, Scatter.halfWidth(12, 1, 0, 1000), 1e-9);
        assertEquals(6.0, Scatter.halfWidth(12, 2, 0, 1000), 1e-9);
        assertEquals(3.0, Scatter.halfWidth(12, 3, 0, 1000), 1e-9);
    }

    @Test
    void theBaseNeverShrinksBelowItsFloor() {
        assertEquals(1.2, Scatter.halfWidth(12, 10, 0, 1000), 1e-9);
    }

    @Test
    void theDistanceTermIsTenTimesTheSquaredRatio() {
        // ICBM-Classic's term: 10 * (d / range)^2.
        assertEquals(12.0 + 2.5, Scatter.halfWidth(12, 0, 500, 1000), 1e-9);
        assertEquals(12.0 + 10.0, Scatter.halfWidth(12, 0, 1000, 1000), 1e-9);
        assertEquals(12.0 + 10.0, Scatter.halfWidth(12, 0, 9999, 1000), 1e-9, "the ratio is capped at 1");
    }

    @Test
    void theOffsetIsTriangularAndBounded() {
        Random rng = new Random(7);
        double half = 12;
        double sum = 0;
        int center = 0;
        int n = 20000;
        for (int i = 0; i < n; i++) {
            double o = Scatter.offset(rng, half);
            assertTrue(Math.abs(o) <= half);
            sum += o;
            if (Math.abs(o) < half / 4) {
                center++;
            }
        }
        assertEquals(0.0, sum / n, 0.2, "centered on the target");
        // Triangular: a quarter of the width around the middle holds about 44% of the mass (a uniform one: 25%).
        assertTrue(center > 0.40 * n && center < 0.48 * n, "triangular shape, not flat: " + center);
    }

    @Test
    void thereIsNoFailureRoll() {
        // D24: no duds. Whatever the dice say, the offset is finite.
        for (double r : new double[] { 0.0, 1.0 - 1e-12 }) {
            Random fixed = new Random() {

                @Override
                public double nextDouble() {
                    return r;
                }
            };
            assertTrue(Math.abs(Scatter.offset(fixed, 5)) < 1e-9);
        }
    }
}
