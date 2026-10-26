package com.gregguns.icbm.warhead;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class WarheadFuseTest {

    private static final int LIT = 100;

    @Test
    void theLitFuseScalesWithTheRadius() {
        // D51: 20 * (3 + ceil(radius / 3)) ticks.
        assertEquals(120, WarheadFuse.litTicks(9.0, 1.0));
        assertEquals(200, WarheadFuse.litTicks(20.0, 1.0));
        assertEquals(80, WarheadFuse.litTicks(3.0, 1.0));
        assertEquals(100, WarheadFuse.litTicks(3.1, 1.0));
        assertEquals(60, WarheadFuse.litTicks(0.0, 1.0));
    }

    @Test
    void theMultiplierScalesTheFuseAndNeverReachesZero() {
        assertEquals(240, WarheadFuse.litTicks(9.0, 2.0));
        assertEquals(60, WarheadFuse.litTicks(9.0, 0.5));
        assertEquals(1, WarheadFuse.litTicks(9.0, 0.0));
    }

    @Test
    void theChainFuseStaysShort() {
        assertEquals(10, WarheadFuse.CHAIN_TICKS);
    }

    @Test
    void theFuseRunsDownAndDetonatesAtZero() {
        int fuse = LIT;
        int steps = 0;
        WarheadFuse.Step s;
        do {
            s = WarheadFuse.advance(fuse);
            fuse = s.remaining;
            steps++;
        } while (s.action != WarheadFuse.Action.DETONATE);
        assertEquals(LIT, steps);
        assertEquals(0, fuse);
    }

    @Test
    void itBeepsOncePerSecond() {
        int beeps = 0;
        int fuse = LIT;
        while (fuse > 0) {
            WarheadFuse.Step s = WarheadFuse.advance(fuse);
            fuse = s.remaining;
            if (s.action == WarheadFuse.Action.BEEP) {
                beeps++;
            }
        }
        assertEquals(4, beeps);
    }

    @Test
    void aFuseAboveAByteSurvives() {
        // ICBM-02: a byte fuse overflowed above 127; ours is an int.
        assertEquals(300, WarheadFuse.arm(0, 300));
        assertEquals(299, WarheadFuse.advance(300).remaining);
    }

    @Test
    void aLitFuseOnlyGetsShorter() {
        assertEquals(40, WarheadFuse.arm(40, 100));
        assertEquals(10, WarheadFuse.arm(40, 10));
    }
}
