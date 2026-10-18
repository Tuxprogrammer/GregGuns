package com.gregguns.client.anim;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class GunCurvesTest {

    private static final double EPS = 1e-9;

    @Test
    void triangleAndDips() {
        assertEquals(0.0, GunCurves.tri(0.0), EPS);
        assertEquals(1.0, GunCurves.tri(0.5), EPS);
        assertEquals(0.0, GunCurves.tri(1.0), EPS);
        assertEquals(-0.5, GunCurves.yF(0.5), EPS);
        assertEquals(-1.0, GunCurves.yR(0.5), EPS);
        assertEquals(-0.25, GunCurves.yF(0.25), EPS);
    }

    @Test
    void plateauCurves() {
        assertEquals(0.99, GunCurves.p66(0.5), EPS);
        assertEquals(0.99, GunCurves.p66(0.3), EPS);
        assertEquals(0.0, GunCurves.p66(0.0), EPS);
        assertEquals(0.99975, GunCurves.p75(0.5), EPS);
        assertEquals(1.0, GunCurves.p50(0.5), EPS);
        assertEquals(1.0, GunCurves.p50(0.3), EPS);
    }

    @Test
    void kickSnapsThenEasesAndEndsAtHalf() {
        assertEquals(0.0, GunCurves.kick(0.0), EPS);
        assertEquals(1.0, GunCurves.kick(0.125 - 1e-12), 1e-9);
        assertEquals(0.9375, GunCurves.kick(0.125), EPS);
        assertEquals(1.25 * (1.0 - 0.5), GunCurves.kick(0.25), EPS);
        assertEquals(0.0, GunCurves.kick(0.5), EPS);
        assertEquals(0.0, GunCurves.kick(0.9), EPS);
        assertEquals(1.0, GunCurves.kickFull(0.25 - 1e-12), 1e-9);
        assertEquals(0.0, GunCurves.kickFull(1.0), EPS);
    }

    @Test
    void thirdPersonRecoilRampsThenSnapsBack() {
        assertEquals(0.5, GunCurves.d30(0.15), EPS);
        assertEquals(0.0, GunCurves.d30(0.3), EPS);
        assertEquals(0.0, GunCurves.d30(0.8), EPS);
    }

    @Test
    void bounceAndWave() {
        assertEquals(0.0, GunCurves.bounce(0.0), EPS);
        assertEquals(1.0, GunCurves.bounce(0.25), EPS);
        assertEquals(0.5, GunCurves.bounce(0.0625), EPS);
        assertEquals(0.0, GunCurves.bounce(1.0), EPS);
        assertEquals(0.0, GunCurves.wave(0.0), EPS);
        assertEquals(1.0, GunCurves.wave(0.25), EPS);
        assertEquals(-1.0, GunCurves.wave(0.75), EPS);
    }

    @Test
    void scopeRecoilPeaksAtOneFifth() {
        assertEquals(0.0, GunCurves.scopeRecoil(0.0), EPS);
        assertEquals(1.0, GunCurves.scopeRecoil(0.2), EPS);
        assertEquals(0.0, GunCurves.scopeRecoil(1.0), EPS);
    }
}
