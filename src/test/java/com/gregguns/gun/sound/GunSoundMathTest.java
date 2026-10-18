package com.gregguns.gun.sound;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GunSoundMathTest {

    @Test
    void audibleRangeIsSixteenTimesVolumeAboveOne() {
        assertEquals(64.0, GunSoundMath.audibleRange(GunSoundMath.FIRE_VOLUME));
        assertEquals(16.0, GunSoundMath.audibleRange(1.0f));
        assertEquals(16.0, GunSoundMath.audibleRange(0.25f));
    }

    @Test
    void forwardPointsWhereThePlayerLooks() {
        // Yaw 0 faces +Z, 90 faces -X, 180 faces -Z, 270 faces +X.
        assertEquals(1.0, GunSoundMath.forwardZ(0, 1.0), 1e-9);
        assertEquals(0.0, GunSoundMath.forwardX(0, 1.0), 1e-9);
        assertEquals(-1.0, GunSoundMath.forwardX(90, 1.0), 1e-9);
        assertEquals(-1.0, GunSoundMath.forwardZ(180, 1.0), 1e-9);
        assertEquals(1.0, GunSoundMath.forwardX(270, 1.0), 1e-9);
    }

    @Test
    void doesNotReproduceCORE21_muzzleIsInFrontNotBesideTheShooter() {
        // Techguns used (cos yaw, sin yaw), the left side: facing +Z that gave X = 1, Z = 0.
        assertEquals(0.0, GunSoundMath.forwardX(0, 1.0), 1e-9);
        assertEquals(1.0, GunSoundMath.forwardZ(0, 1.0), 1e-9);
    }

    @Test
    void channelsAreLengthTimesRateRoundedUp() {
        assertEquals(1, GunSoundMath.channelsNeeded(1.14, 0.5));
        assertEquals(2, GunSoundMath.channelsNeeded(1.14, 1.5));
        assertEquals(12, GunSoundMath.channelsNeeded(1.2, 10));
        assertEquals(1, GunSoundMath.channelsNeeded(0, 5));
        assertEquals(8, GunSoundMath.channelsNeeded(2.0, 4.0));
    }

    @Test
    void budgetIsEightChannels() {
        assertTrue(GunSoundMath.withinChannelBudget(2.0, 4.0));
        assertFalse(GunSoundMath.withinChannelBudget(2.0, 4.1));
        assertFalse(GunSoundMath.withinChannelBudget(1.2, 10));
    }

    @Test
    void negativeInputsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> GunSoundMath.channelsNeeded(-1, 1));
        assertThrows(IllegalArgumentException.class, () -> GunSoundMath.channelsNeeded(1, -1));
    }
}
