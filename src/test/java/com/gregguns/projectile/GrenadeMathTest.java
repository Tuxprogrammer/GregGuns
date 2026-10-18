package com.gregguns.projectile;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GrenadeMathTest {

    private static final double EPS = 1.0E-9;

    @Test
    void chargeFollowsTheBowCurve() {
        assertEquals(0.0, GrenadeMath.charge(0), EPS);
        // f = 0.5: (0.25 + 1.0) / 3
        assertEquals(1.25 / 3.0, GrenadeMath.charge(10), EPS);
        assertEquals(1.0, GrenadeMath.charge(20), EPS);
        assertEquals(1.0, GrenadeMath.charge(500), EPS);
        assertEquals(0.0, GrenadeMath.charge(-5), EPS);
    }

    @Test
    void launchSpeedRunsFromHalfToOne() {
        assertEquals(0.5, GrenadeMath.launchSpeed(0.0), EPS);
        assertEquals(1.0, GrenadeMath.launchSpeed(1.0), EPS);
    }

    @Test
    void doesNotReproduceGUN22_gravityNeverDividesByZero() {
        double g = GrenadeMath.gravity(0.0);
        assertTrue(Double.isFinite(g));
        assertEquals(0.15, g, EPS);
        assertEquals(0.015, GrenadeMath.gravity(1.0), EPS);
        assertTrue(GrenadeMath.gravity(0.2) > GrenadeMath.gravity(0.9), "a weaker throw falls faster");
    }

    @Test
    void fuseRunsOutAfterTwoHundredTicks() {
        int left = GrenadeMath.FUSE_TICKS;
        int ticks = 0;
        while (!GrenadeMath.fuseExpired(--left)) {
            ticks++;
        }
        assertEquals(199, ticks, "199 ticks pass quietly, the 200th detonates");
        assertEquals(200, GrenadeMath.FUSE_TICKS);
    }

    @Test
    void bounceReflectsAndDampsByHalf() {
        double[] r = GrenadeMath.bounce(0.2, -0.4, 0.0, GrenadeMath.faceNormal(1));
        assertArrayEquals(new double[] { 0.1, 0.2, 0.0 }, r, EPS);
    }

    @Test
    void bounceOffAWallKeepsTheSlide() {
        // moving +x into a west face (normal -x): x flips, z is kept; both halved.
        double[] r = GrenadeMath.bounce(0.4, 0.0, 0.2, GrenadeMath.faceNormal(4));
        assertArrayEquals(new double[] { -0.2, 0.0, 0.1 }, r, EPS);
    }

    @Test
    void everyFaceNormalIsAUnitAxis() {
        for (int side = 0; side < 6; side++) {
            int[] n = GrenadeMath.faceNormal(side);
            assertEquals(1, Math.abs(n[0]) + Math.abs(n[1]) + Math.abs(n[2]), "side " + side);
        }
    }

    @Test
    void speedIntoSurfaceIgnoresMovingAway() {
        assertEquals(0.3, GrenadeMath.speedIntoSurface(0.0, -0.3, 0.0, GrenadeMath.faceNormal(1)), EPS);
        assertEquals(0.0, GrenadeMath.speedIntoSurface(0.0, 0.3, 0.0, GrenadeMath.faceNormal(1)), EPS);
    }

    @Test
    void hardContactsBounceUntilTheBouncesRunOut() {
        assertEquals(GrenadeMath.Contact.BOUNCE, GrenadeMath.contact(3, 0.3));
        assertEquals(GrenadeMath.Contact.BOUNCE, GrenadeMath.contact(1, 0.3));
        assertEquals(GrenadeMath.Contact.DETONATE, GrenadeMath.contact(0, 0.3));
    }

    @Test
    void slowContactsSettleWithoutSpendingBounces() {
        assertEquals(GrenadeMath.Contact.REST, GrenadeMath.contact(3, 0.05));
        assertEquals(GrenadeMath.Contact.REST, GrenadeMath.contact(0, 0.05));
    }

    @Test
    void aDroppedGrenadeBouncesThenRestsInsteadOfExploding() {
        // Fall from 3 blocks at 0.015 per tick squared with 0.98 drag, the way GenericProjectile steps it.
        double vy = 0.0;
        int bounces = GrenadeMath.STIELGRANATE_BOUNCES;
        double y = 3.0;
        boolean rested = false;
        for (int tick = 0; tick < 200 && !rested; tick++) {
            vy = (vy - GrenadeMath.BASE_GRAVITY) * 0.98;
            y += vy;
            if (y <= 0.0) {
                y = 0.0;
                GrenadeMath.Contact c = GrenadeMath.contact(bounces, -vy);
                assertFalse(c == GrenadeMath.Contact.DETONATE, "a dropped grenade must not detonate on the floor");
                if (c == GrenadeMath.Contact.BOUNCE) {
                    bounces--;
                    vy = GrenadeMath.bounce(0.0, vy, 0.0, GrenadeMath.faceNormal(1))[1];
                } else {
                    rested = true;
                }
            }
        }
        assertTrue(rested);
        assertTrue(bounces < GrenadeMath.STIELGRANATE_BOUNCES);
    }

    @Test
    void settlingRemovesTheNormalAndSlowsTheSlide() {
        double[] r = GrenadeMath.settle(0.1, -0.05, 0.0, GrenadeMath.faceNormal(1));
        assertArrayEquals(new double[] { 0.07, 0.0, 0.0 }, r, EPS);
    }
}
