package com.gregguns.explosion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FlashMathTest {

    private static final double EPS = 1.0E-9;

    @Test
    void falloffIsLinearToZeroAtTheRadius() {
        assertEquals(1.0, FlashMath.falloff(0.0, 8.0), EPS);
        assertEquals(0.5, FlashMath.falloff(4.0, 8.0), EPS);
        assertEquals(0.0, FlashMath.falloff(8.0, 8.0), EPS);
        assertEquals(0.0, FlashMath.falloff(20.0, 8.0), EPS);
        assertEquals(0.0, FlashMath.falloff(Double.NaN, 8.0), EPS);
    }

    @Test
    void visibilityWeighsTheEyesMost() {
        assertEquals(1.0, FlashMath.visibility(true, true, true), EPS);
        assertEquals(0.0, FlashMath.visibility(false, false, false), EPS);
        assertEquals(0.6, FlashMath.visibility(true, false, false), EPS);
        assertEquals(0.4, FlashMath.visibility(false, true, true), EPS);
    }

    @Test
    void facingIsFullAtTheBlastAndTheFloorBehind() {
        assertEquals(1.0, FlashMath.facing(1.0), EPS);
        assertEquals(FlashMath.BEHIND_FLOOR, FlashMath.facing(-1.0), EPS);
        assertTrue(FlashMath.facing(0.0) > FlashMath.BEHIND_FLOOR && FlashMath.facing(0.0) < 0.5);
        assertEquals(1.0, FlashMath.facing(7.0), EPS);
        assertEquals(FlashMath.BEHIND_FLOOR, FlashMath.facing(-7.0), EPS);
    }

    @Test
    void facingNeverFallsAsTheViewerTurnsTowardTheBlast() {
        double last = -1.0;
        for (double dot = -1.0; dot <= 1.0; dot += 0.1) {
            double f = FlashMath.facing(dot);
            assertTrue(f >= last, "at " + dot);
            last = f;
        }
    }

    @Test
    void aPlayerFacingTheBlastLosesMoreThanOneLookingAway() {
        double front = FlashMath.intensity(4.0, 1.0, 1.0, true);
        double back = FlashMath.intensity(4.0, -1.0, 1.0, true);
        assertEquals(0.5, front, EPS);
        assertEquals(80, FlashMath.effectTicks(front));
        assertEquals(0, FlashMath.effectTicks(back), "a turned back at 4 blocks is below the minimum");
    }

    @Test
    void aMobIgnoresFacing() {
        assertEquals(FlashMath.intensity(4.0, 1.0, 1.0, false), FlashMath.intensity(4.0, -1.0, 1.0, false), EPS);
    }

    @Test
    void aWallHidesTheFlash() {
        assertEquals(0.0, FlashMath.intensity(2.0, 1.0, FlashMath.visibility(false, false, false), true), EPS);
        assertTrue(
            FlashMath.intensity(2.0, 1.0, FlashMath.visibility(true, false, false), true)
                < FlashMath.intensity(2.0, 1.0, 1.0, true));
    }

    @Test
    void effectTicksHaveAMinimumAndAMaximum() {
        assertEquals(FlashMath.MAX_TICKS, FlashMath.effectTicks(1.0));
        assertEquals(FlashMath.MAX_TICKS, FlashMath.effectTicks(5.0));
        assertEquals(0, FlashMath.effectTicks(0.0));
        assertEquals(0, FlashMath.effectTicks(-1.0));
        assertEquals(0, FlashMath.effectTicks(0.1), "16 ticks is under the 20 tick minimum");
        assertEquals(FlashMath.MIN_TICKS, FlashMath.effectTicks(0.125));
    }

    @Test
    void nauseaIsHalfTheBlindness() {
        assertEquals(40, FlashMath.nauseaTicks(80));
        assertEquals(0, FlashMath.nauseaTicks(0));
    }

    @Test
    void mobSlownessIsHeavyFromHalfIntensity() {
        assertEquals(1, FlashMath.mobSlownessLevel(0.49));
        assertEquals(3, FlashMath.mobSlownessLevel(0.5));
    }

    @Test
    void overlayHoldsThenFadesToNothing() {
        assertEquals(1.0, FlashMath.overlayAlpha(0.0, 100), EPS);
        assertEquals(1.0, FlashMath.overlayAlpha(30.0, 100), EPS);
        assertTrue(FlashMath.overlayAlpha(50.0, 100) < 1.0 && FlashMath.overlayAlpha(50.0, 100) > 0.0);
        assertEquals(0.0, FlashMath.overlayAlpha(100.0, 100), EPS);
        assertEquals(0.0, FlashMath.overlayAlpha(10.0, 0), EPS);
        double last = 1.0;
        for (double t = 30.0; t <= 100.0; t += 5.0) {
            double a = FlashMath.overlayAlpha(t, 100);
            assertTrue(a <= last, "at " + t);
            last = a;
        }
    }
}
