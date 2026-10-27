package com.gregguns.icbm.silo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.gregguns.icbm.silo.SiloDoors.State;

/** The silo door state machine and its timing (D89). */
class SiloDoorsTest {

    @Test
    void closedDoorsStayClosed() {
        SiloDoors d = new SiloDoors();
        assertEquals(State.CLOSED, d.state());
        assertEquals(0.0, d.progress(1000), 1e-9);
        assertFalse(d.update(1000));
        d.close(1000, 40);
        assertEquals(State.CLOSED, d.state());
    }

    @Test
    void openingTakesTheConfiguredTicksAndEases() {
        SiloDoors d = new SiloDoors();
        d.open(100, 40);
        assertEquals(State.OPENING, d.state());
        assertEquals(0.0, d.progress(100), 1e-9);
        assertEquals(0.5, d.progress(120), 1e-9);
        assertTrue(d.progress(110) < 0.25, "eases in");
        assertEquals(1.0, d.progress(140), 1e-9);
        assertFalse(d.update(139));
        assertTrue(d.update(140));
        assertEquals(State.OPEN, d.state());
        assertEquals(1.0, d.progress(9999), 1e-9);
    }

    @Test
    void closingAfterAnOpenRunsBackDown() {
        SiloDoors d = new SiloDoors();
        d.restore(State.OPEN, 0, 40);
        d.close(500, 40);
        assertEquals(State.CLOSING, d.state());
        assertEquals(1.0, d.progress(500), 1e-9);
        assertEquals(0.5, d.progress(520), 1e-9);
        assertTrue(d.update(540));
        assertEquals(State.CLOSED, d.state());
        assertEquals(0.0, d.progress(540), 1e-9);
    }

    @Test
    void anAbortMidOpeningTurnsAroundFromTheSameAngle() {
        SiloDoors d = new SiloDoors();
        d.open(0, 40);
        double before = d.progress(15);
        d.close(15, 40);
        assertEquals(State.CLOSING, d.state());
        assertEquals(before, d.progress(15), 1e-6, "no jump at the turn");
        assertTrue(d.progress(25) < before);
        // It takes only as long as the way back: 15 of 40 ticks.
        assertFalse(d.update(15 + 14));
        assertTrue(d.update(15 + 15));
        assertEquals(State.CLOSED, d.state());
    }

    @Test
    void reopeningMidCloseTurnsAroundAgain() {
        SiloDoors d = new SiloDoors();
        d.restore(State.OPEN, 0, 40);
        d.close(100, 40);
        double mid = d.progress(110);
        d.open(110, 40);
        assertEquals(State.OPENING, d.state());
        assertEquals(mid, d.progress(110), 1e-6);
        assertTrue(d.progress(120) > mid);
    }

    @Test
    void ticksUntilOpenCountsTheRestOfTheSwing() {
        SiloDoors d = new SiloDoors();
        assertEquals(40, d.ticksUntilOpen(0));
        d.open(0, 40);
        assertEquals(40, d.ticksUntilOpen(0));
        assertEquals(25, d.ticksUntilOpen(15));
        assertEquals(0, d.ticksUntilOpen(60));
        d.update(60);
        assertEquals(0, d.ticksUntilOpen(60));
    }

    @Test
    void aSavedMachineComesBackTheSame() {
        SiloDoors a = new SiloDoors();
        a.open(1000, 36);
        SiloDoors b = new SiloDoors();
        b.restore(a.state(), a.start(), a.duration());
        for (long t = 1000; t < 1040; t += 7) {
            assertEquals(a.progress(t), b.progress(t), 1e-12);
        }
        b.restore(null, 0, -5);
        assertEquals(State.CLOSED, b.state());
        assertEquals(1, b.duration());
    }

    @Test
    void resetSnapsShut() {
        SiloDoors d = new SiloDoors();
        d.restore(State.OPEN, 0, 40);
        d.reset();
        assertEquals(State.CLOSED, d.state());
        assertEquals(0.0, d.progress(5), 1e-9);
    }
}
