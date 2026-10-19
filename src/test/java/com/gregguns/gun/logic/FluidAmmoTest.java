package com.gregguns.gun.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Tests of the fluid-tank ammo arithmetic: what counts as a magazine and what a round costs. */
class FluidAmmoTest {

    @Test
    void onlyAFullTankIsAMagazine() {
        assertTrue(FluidAmmo.isFullTank(1000, 1000));
        assertTrue(FluidAmmo.isFullTank(1200, 1000));
        assertFalse(FluidAmmo.isFullTank(999, 1000), "a partly filled tank cannot be topped up, so it is not ammo");
        assertFalse(FluidAmmo.isFullTank(0, 1000));
        assertFalse(FluidAmmo.isFullTank(1000, 0));
    }

    @Test
    void aRoundIsACapacityOverTheRoundsPerTank() {
        assertEquals(10.0, FluidAmmo.mbPerRound(1000, 100), 1e-9);
        assertEquals(3.3333, FluidAmmo.mbPerRound(1000, 300), 1e-3);
        assertThrows(IllegalArgumentException.class, () -> FluidAmmo.mbPerRound(1000, 0));
    }

    @Test
    void fuelLeftRoundsDownAndNeverGoesNegative() {
        assertEquals(333, FluidAmmo.fuelLeftMb(100, 1000, 300));
        assertEquals(0, FluidAmmo.fuelLeftMb(-5, 1000, 100));
    }
}
