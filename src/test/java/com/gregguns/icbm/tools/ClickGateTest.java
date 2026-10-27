package com.gregguns.icbm.tools;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

/** The decision rules that make one physical click one action (the I4 client finding). */
class ClickGateTest {

    private final UUID a = UUID.randomUUID();
    private final UUID b = UUID.randomUUID();

    @Test
    void anAirUseRightAfterABlockUseIsTheSameClick() {
        ClickGate g = new ClickGate();
        g.recordBlockUse(a, 100, 0, 1, 2, 3);
        assertTrue(g.isAirUseOfBlockClick(a, 100));
        assertTrue(g.isAirUseOfBlockClick(a, 100 + ClickGate.WINDOW_TICKS));
    }

    @Test
    void aLaterAirUseIsAGenuineClick() {
        ClickGate g = new ClickGate();
        g.recordBlockUse(a, 100, 0, 1, 2, 3);
        assertFalse(g.isAirUseOfBlockClick(a, 101 + ClickGate.WINDOW_TICKS));
        assertFalse(g.isAirUseOfBlockClick(a, 99));
    }

    @Test
    void otherPlayersAreNotAffected() {
        ClickGate g = new ClickGate();
        g.recordBlockUse(a, 100, 0, 1, 2, 3);
        assertFalse(g.isAirUseOfBlockClick(b, 100));
        assertFalse(g.isRepeatBlockUse(b, 100, 0, 1, 2, 3));
    }

    @Test
    void aRepeatMustHitTheSameBlockInTheSameDimension() {
        ClickGate g = new ClickGate();
        g.recordBlockUse(a, 100, 0, 1, 2, 3);
        assertTrue(g.isRepeatBlockUse(a, 101, 0, 1, 2, 3));
        assertFalse(g.isRepeatBlockUse(a, 101, 0, 1, 2, 4));
        assertFalse(g.isRepeatBlockUse(a, 101, -1, 1, 2, 3));
        assertFalse(g.isRepeatBlockUse(a, 200, 0, 1, 2, 3));
    }

    @Test
    void forgettingAPlayerEndsTheWindow() {
        ClickGate g = new ClickGate();
        g.recordBlockUse(a, 100, 0, 1, 2, 3);
        g.forget(a);
        assertFalse(g.isAirUseOfBlockClick(a, 100));
    }

    @Test
    void clientStillLetsThePacketThrough() {
        assertFalse(HandheldRules.clientConsumesUse());
    }
}
