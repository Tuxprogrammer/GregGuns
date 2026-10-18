package com.gregguns.gun.sound;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ReloadSoundGateTest {

    @Test
    void firstStartAlwaysPlays() {
        assertTrue(new ReloadSoundGate().tryStart(0));
    }

    @Test
    void startWithinTheWindowIsSuppressed() {
        ReloadSoundGate gate = new ReloadSoundGate();
        assertTrue(gate.tryStart(100));
        assertFalse(gate.tryStart(100));
        assertFalse(gate.tryStart(100 + ReloadSoundGate.WINDOW_TICKS - 1));
    }

    @Test
    void startAfterTheWindowPlaysAndRestartsIt() {
        ReloadSoundGate gate = new ReloadSoundGate();
        assertTrue(gate.tryStart(100));
        assertTrue(gate.tryStart(100 + ReloadSoundGate.WINDOW_TICKS));
        assertFalse(gate.tryStart(100 + ReloadSoundGate.WINDOW_TICKS + 1));
    }

    @Test
    void tickCounterGoingBackwardsDoesNotSuppress() {
        ReloadSoundGate gate = new ReloadSoundGate();
        assertTrue(gate.tryStart(500));
        assertTrue(gate.tryStart(3));
    }

    @Test
    void resetForgetsTheLastStart() {
        ReloadSoundGate gate = new ReloadSoundGate();
        gate.tryStart(7);
        gate.reset();
        assertTrue(gate.tryStart(7));
    }
}
