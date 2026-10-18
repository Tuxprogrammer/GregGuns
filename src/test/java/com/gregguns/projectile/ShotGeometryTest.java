package com.gregguns.projectile;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ShotGeometryTest {

    private static final double EPS = 1e-9;

    @Test
    void facingSouthFliesAlongPlusZFromTheRightHand() {
        ShotGeometry.Spawn s = ShotGeometry.compute(10, 65.62, 20, 0, 0, 0, 0, false, 1.5, 0);
        assertEquals(0.0, s.motionX, EPS);
        assertEquals(0.0, s.motionY, EPS);
        assertEquals(1.5, s.motionZ, EPS);
        // Facing south the right hand is on the west side (-X) and 0.1 below the eye.
        assertEquals(10 - 0.16, s.x, EPS);
        assertEquals(65.62 - 0.1, s.y, EPS);
        assertEquals(20.0, s.z, EPS);
        assertEquals(0.0F, s.yaw, 1e-4F);
        assertEquals(0.0F, s.pitch, 1e-4F);
    }

    @Test
    void leftHandMirrorsTheSidewaysShiftOnly() {
        ShotGeometry.Spawn right = ShotGeometry.compute(0, 0, 0, 0, 0, 0, 0, false, 1.0, 0);
        ShotGeometry.Spawn left = ShotGeometry.compute(0, 0, 0, 0, 0, 0, 0, true, 1.0, 0);
        assertEquals(-right.x, left.x, EPS);
        assertEquals(right.y, left.y, EPS);
        assertEquals(right.z, left.z, EPS);
        assertEquals(right.motionZ, left.motionZ, EPS);
    }

    @Test
    void yawMinus90FacesEastAndPitchMinus90LooksUp() {
        ShotGeometry.Spawn east = ShotGeometry.compute(0, 0, 0, -90, 0, 0, 0, false, 2.0, 0);
        assertEquals(2.0, east.motionX, EPS);
        assertEquals(0.0, east.motionZ, EPS);
        ShotGeometry.Spawn up = ShotGeometry.compute(0, 0, 0, 0, -90, 0, 0, false, 2.0, 0);
        assertEquals(2.0, up.motionY, EPS);
        assertEquals(90.0F, up.pitch, 1e-4F);
    }

    @Test
    void spreadOffsetsTurnTheDirection() {
        ShotGeometry.Spawn turned = ShotGeometry.compute(0, 0, 0, 0, 0, 90, 0, false, 1.0, 0);
        // Yaw 90 faces west (-X).
        assertEquals(-1.0, turned.motionX, EPS);
        assertEquals(0.0, turned.motionZ, EPS);
        ShotGeometry.Spawn lowered = ShotGeometry.compute(0, 0, 0, 0, 0, 0, 30, false, 1.0, 0);
        assertEquals(-Math.sin(Math.toRadians(30)), lowered.motionY, EPS);
    }

    @Test
    void speedScalesAUnitDirection() {
        ShotGeometry.Spawn s = ShotGeometry.compute(0, 0, 0, 37, 12, 0, 0, false, 3.0, 0);
        double speed = Math.sqrt(s.motionX * s.motionX + s.motionY * s.motionY + s.motionZ * s.motionZ);
        assertEquals(3.0, speed, EPS);
    }

    @Test
    void forwardFractionShiftsTheStartAlongTheMotion() {
        ShotGeometry.Spawn base = ShotGeometry.compute(0, 0, 0, 0, 0, 0, 0, false, 2.0, 0);
        ShotGeometry.Spawn shifted = ShotGeometry.compute(0, 0, 0, 0, 0, 0, 0, false, 2.0, 0.5);
        assertEquals(base.z + 1.0, shifted.z, EPS);
        assertEquals(base.x, shifted.x, EPS);
    }
}
