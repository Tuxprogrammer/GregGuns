package com.gregguns.client.anim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ZoomAnimatorTest {

    @Test
    void startsUnzoomed() {
        ZoomAnimator z = new ZoomAnimator();
        assertEquals(1.0, z.value(0.5), 1e-9);
        assertFalse(z.isZoomed());
    }

    @Test
    void easesTowardTargetAndSettlesExactly() {
        ZoomAnimator z = new ZoomAnimator();
        z.setTarget(0.35);
        assertTrue(z.isZoomed());
        double last = 1.0;
        for (int i = 0; i < 30; i++) {
            z.tick();
            double v = z.value(1.0);
            assertTrue(v <= last + 1e-12, "monotonic");
            last = v;
        }
        assertEquals(0.35, last, 1e-9);
    }

    @Test
    void partialTickInterpolatesBetweenTicks() {
        ZoomAnimator z = new ZoomAnimator();
        z.setTarget(0.5);
        z.tick();
        double a = z.value(0.0);
        double b = z.value(1.0);
        assertEquals(1.0, a, 1e-9);
        assertEquals((a + b) / 2.0, z.value(0.5), 1e-9);
    }

    @Test
    void resetEasesBackToOne() {
        ZoomAnimator z = new ZoomAnimator();
        z.setTarget(0.5);
        for (int i = 0; i < 20; i++) {
            z.tick();
        }
        z.reset();
        assertFalse(z.isZoomed());
        for (int i = 0; i < 20; i++) {
            z.tick();
        }
        assertEquals(1.0, z.value(1.0), 1e-9);
    }

    @Test
    void nonPositiveTargetIsIgnored() {
        ZoomAnimator z = new ZoomAnimator();
        z.setTarget(0.0);
        z.setTarget(-2.0);
        assertEquals(1.0, z.target(), 1e-9);
    }
}
