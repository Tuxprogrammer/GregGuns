package com.gregguns.client.anim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ChargeShiftTest {

    @Test
    void offsetIsZeroAtNoChargeAndTheSpecMaximumAtFull() {
        assertEquals(0.0, ChargeShift.offsetBlocks(0.0), 1e-12);
        assertEquals(0.25, ChargeShift.offsetBlocks(1.0), 1e-12);
        assertEquals(0.125, ChargeShift.offsetBlocks(0.5), 1e-12);
    }

    @Test
    void offsetIsMonotonicAndClamped() {
        double last = -1.0;
        for (int i = 0; i <= 30; i++) {
            double o = ChargeShift.offsetBlocks(i / 30.0);
            assertTrue(o >= last, "monotonic");
            last = o;
        }
        assertEquals(0.0, ChargeShift.offsetBlocks(-1.0), 1e-12);
        assertEquals(0.25, ChargeShift.offsetBlocks(7.0), 1e-12);
    }

    @Test
    void risingChargeIsFollowedAndInterpolatedWithThePartialTick() {
        ChargeShift s = new ChargeShift();
        s.tick(1.0 / 30.0);
        assertEquals(0.0, s.value(0.0), 1e-12);
        assertEquals(1.0 / 30.0, s.value(1.0), 1e-12);
        assertEquals(0.5 / 30.0, s.value(0.5), 1e-12);
    }

    @Test
    void releaseOrCancelEasesBackToExactlyRestAndStaysThere() {
        ChargeShift s = new ChargeShift();
        s.tick(1.0);
        double last = s.value(1.0);
        assertEquals(1.0, last, 1e-12);
        for (int i = 0; i < 10; i++) {
            s.tick(0.0);
            double v = s.value(1.0);
            assertTrue(v <= last + 1e-12, "monotonic fall");
            last = v;
        }
        assertEquals(0.0, last, 1e-12);
        s.tick(0.0);
        assertEquals(0.0, s.value(0.5), 1e-12);
    }

    @Test
    void fallTakesFiveTicksFromFullAndNeverGoesBelowTheTarget() {
        ChargeShift s = new ChargeShift();
        s.tick(1.0);
        for (int i = 0; i < 4; i++) {
            s.tick(0.0);
            assertTrue(s.value(1.0) > 0.0);
        }
        s.tick(0.0);
        assertEquals(0.0, s.value(1.0), 1e-9);
        s.tick(1.0);
        s.tick(0.7);
        assertEquals(0.8, s.value(1.0), 1e-9);
        s.tick(0.7);
        s.tick(0.7);
        assertEquals(0.7, s.value(1.0), 1e-9);
    }

    @Test
    void resetDropsToRest() {
        ChargeShift s = new ChargeShift();
        s.tick(1.0);
        s.reset();
        assertEquals(0.0, s.value(0.5), 1e-12);
    }
}
