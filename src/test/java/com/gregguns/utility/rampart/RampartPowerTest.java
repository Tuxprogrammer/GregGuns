package com.gregguns.utility.rampart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** GGS-08-81: the Rampart draw is the mounts' upkeep and nothing else. */
class RampartPowerTest {

    @Test
    void drawIsFourEuPerTickPerMount() {
        assertEquals(8L, RampartPower.upkeepPerTick(2, 4));
        assertEquals(24L, RampartPower.upkeepPerTick(6, 4));
        // "A 10-mount Rampart upkeep is 40 EU/t, inside an MV Energy Hatch."
        assertEquals(40L, RampartPower.upkeepPerTick(RampartShape.MAX_MOUNTS, 4));
        assertTrue(RampartPower.upkeepPerTick(RampartShape.MAX_MOUNTS, 4) <= 128L);
    }

    @Test
    void drawNeverGoesNegative() {
        assertEquals(0L, RampartPower.upkeepPerTick(-3, 4));
        assertEquals(0L, RampartPower.upkeepPerTick(4, -4));
        assertEquals(0L, RampartPower.upkeepPerTick(0, 4));
    }

    @Test
    void drawHasNoDiscountPerMount() {
        // "EU is the same per mount as a single Turret: there is no energy discount."
        for (int n = 2; n <= 10; n++) {
            assertEquals(n * RampartPower.upkeepPerTick(1, 4), RampartPower.upkeepPerTick(n, 4));
        }
    }

    @Test
    void anExtraChargeIsPaidOnlyWhenTheHatchesTogetherHoldIt() {
        assertTrue(RampartPower.canPay(new long[] { 100, 50 }, 120));
        assertFalse(RampartPower.canPay(new long[] { 100, 50 }, 151));
        assertTrue(RampartPower.canPay(new long[] {}, 0));
        assertFalse(RampartPower.canPay(new long[] {}, 1));
        assertTrue(RampartPower.canPay(new long[] { -5, 10 }, 10));
        assertFalse(RampartPower.canPay(new long[] { -5, 10 }, 11));
    }

    @Test
    void theCycleIsOneSecond() {
        assertEquals(20, RampartPower.CYCLE_TICKS);
    }
}
