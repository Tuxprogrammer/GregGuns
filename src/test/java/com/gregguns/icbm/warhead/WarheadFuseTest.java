package com.gregguns.icbm.warhead;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class WarheadFuseTest {

    @Test
    void theFuseRunsDownAndDetonatesAtZero() {
        int fuse = WarheadFuse.FUSE_TICKS;
        int steps = 0;
        WarheadFuse.Step s;
        do {
            s = WarheadFuse.advance(fuse);
            fuse = s.remaining;
            steps++;
        } while (s.action != WarheadFuse.Action.DETONATE);
        assertEquals(WarheadFuse.FUSE_TICKS, steps);
        assertEquals(0, fuse);
    }

    @Test
    void itBeepsOncePerSecond() {
        int beeps = 0;
        int fuse = WarheadFuse.FUSE_TICKS;
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
