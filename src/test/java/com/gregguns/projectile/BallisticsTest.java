package com.gregguns.projectile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BallisticsTest {

    @Test
    void dragIsGentleInAirAndStrongInWater() {
        assertEquals(0.99, Ballistics.drag(false), 1e-12);
        assertEquals(0.8, Ballistics.drag(true), 1e-12);
    }

    @Test
    void gravityIsAppliedAfterDrag() {
        assertEquals(1.0 * 0.99 - 0.015, Ballistics.nextMotionY(1.0, 0.99, 0.015), 1e-12);
        assertEquals(0.5 * 0.99, Ballistics.nextMotionY(0.5, 0.99, 0.0), 1e-12);
    }

    @Test
    void straightBulletTravelsTheGeometricSeriesOfItsSpeed() {
        double speed = 1.5;
        double x = 0;
        double motion = speed;
        for (int tick = 0; tick < 30; tick++) {
            x += motion;
            motion *= Ballistics.drag(false);
        }
        assertEquals(speed * (1 - Math.pow(0.99, 30)) / 0.01, x, 1e-9);
        assertTrue(x < speed * 30, "drag must shorten the reach below speed times lifetime");
    }

    @Test
    void droppingBallFallsFurtherEachTick() {
        double y = 0;
        double motionY = 0;
        double previousStep = 0;
        for (int tick = 0; tick < 10; tick++) {
            motionY = Ballistics.nextMotionY(motionY, Ballistics.AIR_DRAG, 0.015);
            y += motionY;
            assertTrue(Math.abs(motionY) > previousStep);
            previousStep = Math.abs(motionY);
        }
        assertTrue(y < 0);
    }

    @Test
    void distanceIsEuclidean() {
        assertEquals(5.0, Ballistics.distance(0, 0, 0, 3, 4, 0), 1e-12);
        assertEquals(0.0, Ballistics.distance(1, 2, 3, 1, 2, 3), 0.0);
    }

    @Test
    void yawAndPitchFollowTheProjectileConvention() {
        assertEquals(0.0F, Ballistics.yawOf(0, 1), 1e-4F);
        assertEquals(90.0F, Ballistics.yawOf(1, 0), 1e-4F);
        assertEquals(-90.0F, Ballistics.yawOf(-1, 0), 1e-4F);
        assertEquals(45.0F, Ballistics.pitchOf(1, 1, 0), 1e-4F);
        assertEquals(-90.0F, Ballistics.pitchOf(0, -1, 0), 1e-4F);
    }

    @Test
    void smoothingMovesAFifthOfTheWayTheShortWayRound() {
        assertEquals(2.0F, Ballistics.smoothAngle(0.0F, 10.0F), 1e-5F);
        // From 170 to -170 the short way is +20 degrees across the seam: a fifth is 4, shown as -186 (= 174).
        assertEquals(-186.0F, Ballistics.smoothAngle(170.0F, -170.0F), 1e-4F);
        // And back the other way: -170 to 170 is -20 degrees, shown as 186 (= -174).
        assertEquals(186.0F, Ballistics.smoothAngle(-170.0F, 170.0F), 1e-4F);
    }
}
