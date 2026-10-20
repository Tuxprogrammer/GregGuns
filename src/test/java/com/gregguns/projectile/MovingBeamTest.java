package com.gregguns.projectile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class MovingBeamTest {

    @AfterEach
    void clear() {
        MovingBeams.clear();
    }

    @Test
    void firstRefreshStartsTheBeamAndDealsDamage() {
        MovingBeam beam = new MovingBeam();
        assertFalse(beam.isActive(100));
        MovingBeam.Step s = beam.refresh(100);
        assertTrue(s.started());
        assertTrue(s.dealsDamage());
        assertFalse(s.loopDue());
        assertTrue(beam.isActive(100));
    }

    @Test
    void heldTriggerRefreshesEveryTickWithoutRestarting() {
        MovingBeam beam = new MovingBeam();
        beam.refresh(100);
        for (long t = 101; t < 140; t++) {
            MovingBeam.Step s = beam.refresh(t);
            assertFalse(s.started(), "tick " + t);
            assertTrue(s.dealsDamage(), "tick " + t);
        }
    }

    @Test
    void beamEndsTenTicksAfterTheLastRefresh() {
        MovingBeam beam = new MovingBeam();
        beam.refresh(100);
        assertTrue(beam.isActive(100 + MovingBeam.LIFETIME - 1));
        assertFalse(beam.isActive(100 + MovingBeam.LIFETIME));
        assertTrue(
            beam.refresh(100 + MovingBeam.LIFETIME)
                .started(),
            "a new press starts the sound again");
    }

    @Test
    void secondRefreshInTheSameTickDealsNoDamage() {
        MovingBeam beam = new MovingBeam();
        beam.refresh(100);
        beam.refresh(101);
        MovingBeam.Step again = beam.refresh(101);
        assertFalse(again.dealsDamage());
        assertFalse(again.started());
        assertFalse(again.loopDue());
    }

    @Test
    void loopSoundIsDueOncePerLoopLength() {
        MovingBeam beam = new MovingBeam();
        beam.refresh(0);
        int due = 0;
        for (long t = 1; t <= 60; t++) {
            MovingBeam.Step s = beam.refresh(t);
            if (s.loopDue()) {
                due++;
                assertEquals(0, t % MovingBeam.LOOP_TICKS, "tick " + t);
            }
        }
        assertEquals(3, due);
    }

    @Test
    void tableKeepsOneBeamPerShooterAndDropsExpiredOnes() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        MovingBeams.refresh(a, 10);
        MovingBeams.refresh(b, 10);
        assertEquals(2, MovingBeams.size());
        assertFalse(
            MovingBeams.refresh(a, 11)
                .started());
        // b let go; at tick 30 it has expired and is dropped, a was refreshed and stays.
        MovingBeams.refresh(a, 25);
        MovingBeams.refresh(a, 30);
        assertEquals(1, MovingBeams.size());
        assertTrue(
            MovingBeams.refresh(b, 31)
                .started());
    }
}
