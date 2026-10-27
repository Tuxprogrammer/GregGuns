package com.gregguns.icbm.missile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The launch sequence's timing math (D88). */
class LaunchTimerTest {

    @Test
    void theCountdownIsFullWhileTheDoorsOpenThenRunsDown() {
        // 40 ticks of doors, then 200 of countdown.
        assertEquals(200, LaunchTimer.countdownLeft(0, 40, 240));
        assertEquals(200, LaunchTimer.countdownLeft(40, 40, 240));
        assertEquals(100, LaunchTimer.countdownLeft(140, 40, 240));
        assertEquals(0, LaunchTimer.countdownLeft(240, 40, 240));
        assertEquals(0, LaunchTimer.countdownLeft(999, 40, 240));
    }

    @Test
    void aPadHasNoLead() {
        assertEquals(200, LaunchTimer.countdownLeft(0, 0, 200));
        assertEquals(199, LaunchTimer.countdownLeft(1, 0, 200));
        assertFalse(LaunchTimer.waitingForDoors(0, 0));
        assertTrue(LaunchTimer.waitingForDoors(39, 40));
        assertFalse(LaunchTimer.waitingForDoors(40, 40));
    }

    @Test
    void abortIsPossibleUntilTheLastTick() {
        assertTrue(LaunchTimer.abortable(0, 240));
        assertTrue(LaunchTimer.abortable(239, 240));
        assertFalse(LaunchTimer.abortable(240, 240));
    }

    @Test
    void smokeStartsWithTheCountdownAndGrowsToIgnition() {
        assertEquals(0.0, LaunchTimer.smokeIntensity(10, 40, 240), 1e-9);
        double early = LaunchTimer.smokeIntensity(41, 40, 240);
        double mid = LaunchTimer.smokeIntensity(140, 40, 240);
        double late = LaunchTimer.smokeIntensity(239, 40, 240);
        assertTrue(early > 0.0 && early < mid && mid < late);
        assertEquals(1.0, LaunchTimer.smokeIntensity(240, 40, 240), 1e-9);
        assertEquals(1.0, LaunchTimer.smokeIntensity(500, 40, 240), 1e-9);
    }

    @Test
    void puffsAndSpreadGrowInTheLastSeconds() {
        assertEquals(0, LaunchTimer.puffsPerTick(0.0));
        assertEquals(1, LaunchTimer.puffsPerTick(0.1));
        assertEquals(6, LaunchTimer.puffsPerTick(1.0));
        assertTrue(LaunchTimer.spreadSpeed(1.0) > LaunchTimer.spreadSpeed(0.0));
        assertEquals(0.05, LaunchTimer.spreadSpeed(-3.0), 1e-9);
        assertEquals(0.2, LaunchTimer.spreadSpeed(9.0), 1e-9);
    }

    @Test
    void secondsRoundUp() {
        assertEquals(10, LaunchTimer.seconds(200));
        assertEquals(10, LaunchTimer.seconds(181));
        assertEquals(1, LaunchTimer.seconds(1));
        assertEquals(0, LaunchTimer.seconds(0));
        assertEquals(0, LaunchTimer.seconds(-5));
    }
}
