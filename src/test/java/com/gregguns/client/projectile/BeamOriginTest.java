package com.gregguns.client.projectile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BeamOriginTest {

    @Test
    void forwardLooksAlongPlusZAtYawZero() {
        double[] w = BeamOrigin.eyeToWorld(new double[] { 0, 0, -1 }, 0, 0);
        assertEquals(0.0, w[0], 1e-9);
        assertEquals(0.0, w[1], 1e-9);
        assertEquals(1.0, w[2], 1e-9);
    }

    @Test
    void rightIsMinusXAtYawZeroAndUpIsUp() {
        double[] r = BeamOrigin.eyeToWorld(new double[] { 1, 0, 0 }, 0, 0);
        assertEquals(-1.0, r[0], 1e-9);
        double[] u = BeamOrigin.eyeToWorld(new double[] { 0, 1, 0 }, 0, 0);
        assertEquals(1.0, u[1], 1e-9);
    }

    @Test
    void yawNinetyLooksAlongMinusX() {
        double[] w = BeamOrigin.eyeToWorld(new double[] { 0, 0, -1 }, 90, 0);
        assertEquals(-1.0, w[0], 1e-9);
        assertEquals(0.0, w[2], 1e-9);
    }

    @Test
    void pitchPositiveLooksDown() {
        double[] w = BeamOrigin.eyeToWorld(new double[] { 0, 0, -1 }, 0, 90);
        assertEquals(-1.0, w[1], 1e-9);
    }

    @Test
    void beamKeepsItsEndPointWhenStartMovesToMuzzle() {
        double[] r = BeamOrigin
            .fromMuzzle(new double[] { 0, 0, 0 }, new double[] { 0, 0, 1 }, 10.0, new double[] { 0.3, -0.2, 0.8 });
        double endX = r[0] + r[3] * r[6];
        double endY = r[1] + r[4] * r[6];
        double endZ = r[2] + r[5] * r[6];
        assertEquals(0.0, endX, 1e-9);
        assertEquals(0.0, endY, 1e-9);
        assertEquals(10.0, endZ, 1e-9);
    }

    @Test
    void beamShorterThanTheMuzzleDistanceIsDropped() {
        assertNull(
            BeamOrigin.fromMuzzle(new double[] { 0, 0, 0 }, new double[] { 0, 0, 1 }, 0.5, new double[] { 0, 0, 0.5 }));
    }

    @Test
    void onlyBeamsStartingAtTheCameraAreOwnShots() {
        assertTrue(BeamOrigin.isOwnShot(new double[] { 0.2, -0.1, 0.3 }, new double[] { 0, 0, 0 }));
        assertFalse(BeamOrigin.isOwnShot(new double[] { 5, 0, 0 }, new double[] { 0, 0, 0 }));
    }

    @Test
    void ownShotInFirstPersonMovesToTheMuzzle() {
        double[] r = BeamOrigin.relocate(
            new double[] { 10, 64, 10 },
            new double[] { 10, 64, 10.2 },
            true,
            new double[] { 0.3, -0.2, 0.9 });
        assertEquals(10.3, r[0], 1.0E-9);
        assertEquals(63.8, r[1], 1.0E-9);
        assertEquals(11.1, r[2], 1.0E-9);
    }

    @Test
    void ownShotWithoutAMuzzleIsSkippedForTheShooter() {
        assertTrue(
            BeamOrigin.relocate(new double[] { 0, 0, 0 }, new double[] { 0, 0, 0 }, true, null) == BeamOrigin.SKIP);
    }

    @Test
    void otherViewersAndThirdPersonKeepTheTrueOrigin() {
        assertNull(
            BeamOrigin.relocate(new double[] { 0, 0, 0 }, new double[] { 0, 0, 0 }, false, new double[] { 1, 1, 1 }));
        assertNull(
            BeamOrigin.relocate(new double[] { 9, 0, 0 }, new double[] { 0, 0, 0 }, true, new double[] { 1, 1, 1 }));
    }
}
