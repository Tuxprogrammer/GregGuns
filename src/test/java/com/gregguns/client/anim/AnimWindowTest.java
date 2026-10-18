package com.gregguns.client.anim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class AnimWindowTest {

    @Test
    void progressRunsFromZeroToOneThenExpires() {
        AnimWindow w = new AnimWindow();
        w.begin(10.0, 4.0);
        assertEquals(0.0, w.progress(10.0), 1e-9);
        assertEquals(0.5, w.progress(12.0), 1e-9);
        assertEquals(0.875, w.progress(13.5), 1e-9);
        assertEquals(AnimWindow.NONE, w.progress(14.0));
        assertFalse(w.isActive(14.5), "an expired window stays cleared");
    }

    @Test
    void nonPositiveDurationClears() {
        AnimWindow w = new AnimWindow();
        w.begin(0.0, 5.0);
        w.begin(1.0, 0.0);
        assertFalse(w.isActive(1.0));
    }

    @Test
    void clockGoingBackwardsExpiresTheWindow() {
        AnimWindow w = new AnimWindow();
        w.begin(100.0, 3.0);
        assertEquals(AnimWindow.NONE, w.progress(50.0));
    }

    @Test
    void restartingReplacesTheWindow() {
        AnimWindow w = new AnimWindow();
        w.begin(0.0, 4.0);
        w.begin(3.0, 4.0);
        assertEquals(0.0, w.progress(3.0), 1e-9);
    }
}
