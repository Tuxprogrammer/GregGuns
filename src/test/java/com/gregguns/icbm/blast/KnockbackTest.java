package com.gregguns.icbm.blast;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Blast knockback: away from the center and scaled with distance like the damage. */
// Spec: docs/gregguns-icbm-spec/02-explosives/blast-engine.md (knockback)
class KnockbackTest {

    @Test
    void fullSpeedInTheCoreAndZeroAtTheEdge() {
        assertEquals(Knockback.MAX_SPEED, Knockback.speed(20.0, 0.0), 1e-9);
        assertEquals(Knockback.MAX_SPEED, Knockback.speed(20.0, 2.0), 1e-9);
        assertEquals(0.0, Knockback.speed(20.0, 20.0), 1e-9);
        assertEquals(0.0, Knockback.speed(20.0, 50.0), 1e-9);
    }

    @Test
    void fallsWithDistance() {
        assertTrue(Knockback.speed(20.0, 8.0) > Knockback.speed(20.0, 14.0));
        assertTrue(Knockback.speed(20.0, 14.0) > 0.0);
    }

    @Test
    void pushesAwayFromTheCenter() {
        double[] v = Knockback.velocity(20.0, 5.0, 0.0, 0.0);
        assertTrue(v[0] > 0.0);
        assertEquals(0.0, v[1], 1e-12);
        double[] w = Knockback.velocity(20.0, 0.0, -3.0, 4.0);
        assertTrue(w[1] < 0.0 && w[2] > 0.0);
        assertEquals(Knockback.speed(20.0, 5.0), Math.sqrt(w[0] * w[0] + w[1] * w[1] + w[2] * w[2]), 1e-9);
    }

    @Test
    void outsideTheRadiusIsZeroAndTheExactCenterPushesUp() {
        assertArrayEquals(new double[] { 0, 0, 0 }, Knockback.velocity(10.0, 30.0, 0.0, 0.0));
        double[] up = Knockback.velocity(10.0, 0.0, 0.0, 0.0);
        assertEquals(0.0, up[0]);
        assertEquals(Knockback.MAX_SPEED, up[1], 1e-9);
    }

    @Test
    void nanOffsetPushesNothing() {
        assertArrayEquals(new double[] { 0, 0, 0 }, Knockback.velocity(10.0, Double.NaN, 0.0, 0.0));
    }
}
