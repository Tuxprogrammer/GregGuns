package com.gregguns.icbm.missile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ArcPathTest {

    @Test
    void theArcStartsAtTheStartAndReachesTheTargetAtItsTick() {
        ArcPath p = new ArcPath(10, 100, -20, 410, 64, 180, 0.8);
        double[] s = p.position(0);
        assertEquals(10, s[0], 1e-9);
        assertEquals(100, s[1], 1e-9);
        assertEquals(-20, s[2], 1e-9);
        double[] e = p.position(p.ticks());
        assertEquals(410, e[0], 1e-9);
        assertEquals(64, e[1], 1e-9, "ICBM-03-18: the target height is used");
        assertEquals(180, e[2], 1e-9);
    }

    @Test
    void theFlightTimeFollowsTheSpeed() {
        assertEquals(500, new ArcPath(0, 0, 0, 400, 0, 0, 0.8).ticks());
        assertEquals(334, new ArcPath(0, 0, 0, 400, 0, 0, 1.2).ticks());
    }

    @Test
    void theApexIsAboveBothEndsAndNearTheMiddle() {
        ArcPath p = new ArcPath(0, 100, 0, 500, 100, 0, 1.0);
        double top = Double.NEGATIVE_INFINITY;
        int at = 0;
        for (int n = 0; n <= p.ticks(); n++) {
            double y = p.position(n)[1];
            if (y > top) {
                top = y;
                at = n;
            }
        }
        assertEquals(100 + p.apex(), top, 1.0);
        assertTrue(Math.abs(at - p.ticks() / 2) <= 1);
        assertTrue(p.apex() <= ArcPath.APEX_MAX);
    }

    @Test
    void aTinyOrZeroDistanceDoesNotDivideByZeroOrDive() {
        // ICBM-03-17: a flat distance of 0 or under 1 broke the baseline arc.
        for (double d : new double[] { 0.0, 0.3, 1.0 }) {
            ArcPath p = new ArcPath(0, 100, 0, d, 100, 0, 0.8);
            assertTrue(p.ticks() >= 1 && p.ticks() <= 2);
            for (int n = 0; n < 5; n++) {
                for (double v : p.position(n)) {
                    assertFalse(Double.isNaN(v) || Double.isInfinite(v));
                }
            }
            assertTrue(p.position(0)[1] <= 100 + 1e-9);
            assertEquals(100.0, p.position(p.ticks())[1], 1e-9, "arrives at the target height");
        }
    }

    @Test
    void aLowerTargetIsReachedByTheCurveNotByFreeFall() {
        ArcPath p = new ArcPath(0, 200, 0, 300, 60, 0, 1.0);
        assertEquals(60, p.position(p.ticks())[1], 1e-9);
        // Past the arrival the curve keeps descending, so a missile that is higher than the ground still lands.
        assertTrue(p.position(p.ticks() + 50)[1] < 60);
    }

    @Test
    void theVelocityIsTheStepBetweenTwoTicks() {
        ArcPath p = new ArcPath(0, 0, 0, 100, 0, 0, 1.0);
        double[] v = p.velocity(10);
        assertEquals(1.0, v[0], 1e-9);
        assertEquals(0.0, v[2], 1e-9);
        assertTrue(v[1] > 0, "still climbing before the apex");
        assertTrue(p.velocity(p.ticks() - 2)[1] < 0, "falling at the end");
    }

    @Test
    void theDirectionOfFlightIsPreservedForNegativeCoordinates() {
        ArcPath p = new ArcPath(0, 0, 0, -300, 0, -400, 1.0);
        double[] e = p.position(p.ticks());
        assertEquals(-300, e[0], 1e-9);
        assertEquals(-400, e[2], 1e-9);
    }
}
