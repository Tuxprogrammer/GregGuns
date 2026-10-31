package com.gregguns.core.testing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class FpsWindowTest {

    @Test
    void countsFramesUntilTheWindowEnds() {
        FpsWindow w = new FpsWindow("room", 10.0, 1_000L);
        for (int i = 1; i < 200; i++) {
            assertNull(w.frame(1_000L + i * 50_000_000L));
        }
        assertEquals("FPS room frames=200 secs=10.0 avgFps=20.0 avgMs=50.00", w.frame(1_000L + 10_000_000_000L));
    }
}
