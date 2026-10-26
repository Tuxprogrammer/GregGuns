package com.gregguns.icbm.missile;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FuelMathTest {

    @Test
    void theUnburnedShareFallsLinearlyWithDistance() {
        assertEquals(1.0, FuelMath.unburned(0, 1000), 1e-9);
        assertEquals(0.75, FuelMath.unburned(250, 1000), 1e-9);
        assertEquals(0.0, FuelMath.unburned(1000, 1000), 1e-9);
    }

    @Test
    void theShareIsClampedAndSafe() {
        assertEquals(0.0, FuelMath.unburned(5000, 1000), 1e-9);
        assertEquals(1.0, FuelMath.unburned(-5, 1000), 1e-9);
        assertEquals(0.0, FuelMath.unburned(10, 0), 1e-9);
        assertEquals(0.0, FuelMath.unburned(Double.NaN, 1000), 1e-9);
    }

    @Test
    void thePointBlankBonusIsTheConfiguredCap() {
        // D20: radius * (1 + bonusMax * f); at f = 1 and bonusMax 0.25 the radius grows by a quarter.
        assertEquals(2.5, FuelMath.bonusRadius(10, 0, 1000, 0.25), 1e-9);
        assertEquals(0.0, FuelMath.bonusRadius(10, 1000, 1000, 0.25), 1e-9);
        assertEquals(1.25, FuelMath.bonusRadius(10, 500, 1000, 0.25), 1e-9);
    }

    @Test
    void aZeroBonusConfigOrNegativeInputsGiveNoBonus() {
        assertEquals(0.0, FuelMath.bonusRadius(10, 0, 1000, 0.0), 1e-9);
        assertEquals(0.0, FuelMath.bonusRadius(-10, 0, 1000, 0.25), 1e-9);
        assertEquals(0.0, FuelMath.bonusRadius(10, 0, 1000, -1), 1e-9);
    }

    @Test
    void theTankScalesWithTheFactorAndNeverReachesZero() {
        assertEquals(2000, FuelMath.tank(2000, 1.0));
        assertEquals(4000, FuelMath.tank(2000, 2.0));
        assertEquals(1, FuelMath.tank(2000, 0.0));
    }
}
