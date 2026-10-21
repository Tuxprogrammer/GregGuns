package com.gregguns.armor.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SwimBoostTest {

    private static final double EPS = 1.0E-9;

    @Test
    void hevGivesDoubleSpeed() {
        assertEquals(2.0, SwimBoost.multiplier(5.0), EPS);
        assertEquals(1.0, SwimBoost.multiplier(0.0), EPS);
    }

    @Test
    void noInputAddsNothing() {
        double[] m = SwimBoost.extraMotion(0, 0, 33, 2.0);
        assertEquals(0.0, m[0], EPS);
        assertEquals(0.0, m[1], EPS);
    }

    @Test
    void noBoostAddsNothing() {
        double[] m = SwimBoost.extraMotion(0, 1, 0, 1.0);
        assertEquals(0.0, m[0] + m[1], EPS);
    }

    @Test
    void forwardAtYawZeroPushesPlusZ() {
        double[] m = SwimBoost.extraMotion(0, 1, 0, 2.0);
        assertEquals(0.0, m[0], EPS);
        assertEquals(SwimBoost.VANILLA_ACCEL, m[1], EPS);
    }

    @Test
    void yaw90PushesMinusX() {
        double[] m = SwimBoost.extraMotion(0, 1, 90, 2.0);
        assertEquals(-SwimBoost.VANILLA_ACCEL, m[0], EPS);
        assertEquals(0.0, m[1], EPS);
    }

    @Test
    void diagonalInputIsNormalized() {
        double[] m = SwimBoost.extraMotion(1, 1, 0, 2.0);
        assertEquals(SwimBoost.VANILLA_ACCEL, Math.hypot(m[0], m[1]), EPS);
    }

    @Test
    void capLimitsSpeedAndKeepsDirection() {
        double[] c = SwimBoost.cap(0.6, 0.8);
        assertEquals(SwimBoost.MAX_SPEED, Math.hypot(c[0], c[1]), EPS);
        assertEquals(0.6 / 0.8, c[0] / c[1], EPS);
        double[] slow = SwimBoost.cap(0.05, 0.0);
        assertEquals(0.05, slow[0], EPS);
    }

    @Test
    void steadyStateStaysUnderCap() {
        // Vanilla drag is 0.8 per tick: terminal speed = accel / 0.2.
        double terminal = (SwimBoost.VANILLA_ACCEL * SwimBoost.multiplier(5.0)) / 0.2;
        assertTrue(terminal <= SwimBoost.MAX_SPEED, "terminal " + terminal);
    }

    @Test
    void fovClampOnlyTouchesOurPart() {
        assertEquals(1.0, FovClamp.fovFactor(1.4, 1.4), EPS);
        assertEquals(1.0, FovClamp.fovFactor(1.8, 1.0), EPS);
        // x6.4 from us alone: clamped to the Speed II sprint ratio.
        assertEquals((FovClamp.MAX_RATIO + 1.0) / 7.4, FovClamp.fovFactor(6.4, 6.4), EPS);
        // Another source already above the cap: keep it, drop only ours.
        assertEquals(4.0 / 7.0, FovClamp.fovFactor(6.0, 2.0), EPS);
    }
}
