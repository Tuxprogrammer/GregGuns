package com.gregguns.icbm.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CountdownSnapshotTest {

    @Test
    void countdownKeepsTheRealNumbers() {
        LaunchSnapshot s = LaunchSnapshot.inProgress(40, 20, 1000, 612.4, 125, 3);
        assertEquals(ControlStatus.COUNTDOWN, s.status);
        assertEquals(612, s.distance);
        assertEquals(20, s.minRange);
        assertEquals(1000, s.maxRange);
        assertEquals(125, s.radiusTenths);
        assertEquals(40, s.countdown);
    }

    @Test
    void ignitionKeepsThemToo() {
        LaunchSnapshot s = LaunchSnapshot.inProgress(-1, 20, 1000, 50, 10, 1);
        assertEquals(ControlStatus.IGNITED, s.status);
        assertTrue(s.maxRange > 0);
    }
}
