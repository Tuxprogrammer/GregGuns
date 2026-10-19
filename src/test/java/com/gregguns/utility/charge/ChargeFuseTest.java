package com.gregguns.utility.charge;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** The fuse clock and the arming rules. */
class ChargeFuseTest {

    @Test
    void aHundredTickFuseBeepsOncePerSecondAndDetonatesAtZero() {
        ChargeParams p = new ChargeParams(ChargeType.TNT);
        p.press(ChargeParams.BTN_ARM);
        int beeps = 0;
        int ticks = 0;
        ChargeFuse.Tick t;
        do {
            t = p.tickFuse();
            ticks++;
            if (t == ChargeFuse.Tick.BEEP) {
                beeps++;
            }
        } while (t != ChargeFuse.Tick.DETONATE && ticks < 1000);
        assertEquals(100, ticks);
        // Beeps at 80, 60, 40 and 20 ticks left; 0 is the detonation, not a beep.
        assertEquals(4, beeps);
    }

    @Test
    void anUnarmedChargeNeverTicks() {
        ChargeParams p = new ChargeParams(ChargeType.ADVANCED);
        for (int i = 0; i < 300; i++) {
            assertEquals(ChargeFuse.Tick.NONE, p.tickFuse());
        }
        assertEquals(ChargeType.FUSE_DEFAULT, p.fuse());
    }

    @Test
    void theFuseNeverGoesBelowZero() {
        ChargeFuse.Result r = ChargeFuse.advance(0);
        assertEquals(0, r.remaining);
        assertEquals(ChargeFuse.Tick.DETONATE, r.tick);
        assertEquals(ChargeFuse.Tick.DETONATE, ChargeFuse.advance(1).tick);
        assertEquals(ChargeFuse.Tick.BEEP, ChargeFuse.advance(21).tick);
        assertEquals(ChargeFuse.Tick.NONE, ChargeFuse.advance(22).tick);
    }
}
