package com.gregguns.icbm.soak;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class SoakVerdictTest {

    private static SoakVerdict.Measured healthy() {
        SoakVerdict.Measured m = new SoakVerdict.Measured();
        m.baselineMs = 12.0;
        m.baselineTicks = 600;
        m.recoveryMs = 14.0;
        m.recoveryTicks = 600;
        m.ended = true;
        m.limitTicks = 1000;
        m.ticketsBefore = 3;
        m.ticketsAfter = 3;
        m.forcedBefore = 40;
        m.forcedAfter = 40;
        return m;
    }

    @Test
    void aBurstThatEndsAndRecoversPasses() {
        assertTrue(
            SoakVerdict.judge(healthy(), 10.0)
                .isEmpty());
    }

    @Test
    void spikesDuringTheBurstAreNotJudged() {
        // Nothing in Measured describes the burst's own ticks: lag while 10+ blasts run is accepted (D332).
        SoakVerdict.Measured m = healthy();
        m.recoveryMs = 21.9;
        assertTrue(
            SoakVerdict.judge(m, 10.0)
                .isEmpty());
    }

    @Test
    void aRunawayFails() {
        SoakVerdict.Measured m = healthy();
        m.ended = false;
        List<String> f = SoakVerdict.judge(m, 10.0);
        assertEquals(1, f.size());
        assertTrue(
            f.get(0)
                .contains("runaway"),
            f.get(0));
    }

    @Test
    void aServerThatStaysSlowFails() {
        SoakVerdict.Measured m = healthy();
        m.recoveryMs = 23.0;
        List<String> f = SoakVerdict.judge(m, 10.0);
        assertEquals(1, f.size());
        assertTrue(
            f.get(0)
                .contains("did not recover"),
            f.get(0));
        m.baselineMs = 45.0;
        m.recoveryMs = 52.0;
        f = SoakVerdict.judge(m, 10.0);
        assertEquals(1, f.size());
        assertTrue(
            f.get(0)
                .contains("not playable"),
            f.get(0));
    }

    @Test
    void leakedTicketsAndForcedChunksFail() {
        SoakVerdict.Measured m = healthy();
        m.ticketsAfter = 4;
        m.forcedAfter = 42;
        List<String> f = SoakVerdict.judge(m, 10.0);
        assertEquals(2, f.size());
        assertTrue(
            f.get(0)
                .contains("tickets leaked"),
            f.get(0));
        assertTrue(
            f.get(1)
                .contains("forced chunks leaked"),
            f.get(1));
    }

    @Test
    void noSamplesFails() {
        SoakVerdict.Measured m = healthy();
        m.recoveryTicks = 0;
        assertEquals(
            1,
            SoakVerdict.judge(m, 10.0)
                .size());
    }
}
