package com.gregguns.client.anim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AdsBlendTest {

    @Test
    void startsAtTheNormalHoldAndIdle() {
        AdsBlend b = new AdsBlend();
        assertEquals(0.0, b.value(0.5), 1e-12);
        assertTrue(b.isIdle());
    }

    @Test
    void risesToExactlyOneInTheConfiguredTicksAndStays() {
        AdsBlend b = new AdsBlend();
        b.setAiming(true);
        double last = 0.0;
        for (int i = 0; i < 5; i++) {
            b.tick(5);
            double v = b.value(1.0);
            assertTrue(v >= last, "monotonic");
            last = v;
        }
        assertEquals(1.0, last, 1e-12);
        b.tick(5);
        assertEquals(1.0, b.value(1.0), 1e-12);
        assertFalse(b.isIdle());
    }

    @Test
    void fallsBackToZeroAndBecomesIdle() {
        AdsBlend b = new AdsBlend();
        b.setAiming(true);
        for (int i = 0; i < 6; i++) {
            b.tick(4);
        }
        b.setAiming(false);
        for (int i = 0; i < 4; i++) {
            b.tick(4);
        }
        assertEquals(0.0, b.value(1.0), 1e-12);
        b.tick(4);
        assertTrue(b.isIdle());
    }

    @Test
    void aMidwayReversalContinuesFromWhereItIs() {
        AdsBlend b = new AdsBlend();
        b.setAiming(true);
        b.tick(4);
        b.tick(4);
        double mid = b.value(1.0);
        assertEquals(0.5, mid, 1e-12, "smoothstep of 0.5 is 0.5");
        b.setAiming(false);
        b.tick(4);
        assertTrue(b.value(1.0) < mid);
        assertTrue(b.value(1.0) > 0.0, "no jump to zero");
    }

    @Test
    void theCurveIsEasedAtBothEnds() {
        // Smoothstep: the first step is smaller than a linear step, so the gun leaves the hold gently.
        AdsBlend b = new AdsBlend();
        b.setAiming(true);
        b.tick(10);
        assertTrue(b.value(1.0) < 0.1);
        assertEquals(3 * 0.01 - 2 * 0.001, b.value(1.0), 1e-12);
    }

    @Test
    void partialTickInterpolatesBetweenTheLastTwoTicks() {
        AdsBlend b = new AdsBlend();
        b.setAiming(true);
        b.tick(4);
        assertEquals(0.0, b.value(0.0), 1e-12);
        double end = b.value(1.0);
        double half = b.value(0.5);
        assertTrue(half > 0.0 && half < end);
        // Out-of-range partial ticks are clamped, never extrapolated.
        assertEquals(end, b.value(3.0), 1e-12);
        assertEquals(0.0, b.value(-1.0), 1e-12);
    }

    @Test
    void resetSnapsToTheNormalHoldWithoutAnimation() {
        AdsBlend b = new AdsBlend();
        b.setAiming(true);
        for (int i = 0; i < 3; i++) {
            b.tick(4);
        }
        b.reset();
        assertEquals(0.0, b.value(0.5), 1e-12);
        assertFalse(b.isAiming());
        assertTrue(b.isIdle());
    }

    @Test
    void aTickCountBelowOneStillWorks() {
        AdsBlend b = new AdsBlend();
        b.setAiming(true);
        b.tick(0);
        assertEquals(1.0, b.value(1.0), 1e-12);
    }
}
