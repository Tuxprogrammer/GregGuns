package com.gregguns.projectile;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RayBoxTest {

    @Test
    void headOnSegmentEntersAtTheNearFace() {
        // Box x 4..5, segment from x=0 along +X, length 10.
        double t = RayBox.hit(0, 0.5, 0.5, 10, 0, 0, 4, 0, 0, 5, 1, 1, 0);
        assertEquals(0.4, t, 1e-12);
    }

    @Test
    void marginWidensTheBox() {
        double t = RayBox.hit(0, 0.5, 0.5, 10, 0, 0, 4, 0, 0, 5, 1, 1, 0.3);
        assertEquals(0.37, t, 1e-12);
        // A near miss by 0.2 is caught with the margin and missed without it.
        assertEquals(-1.0, RayBox.hit(0, 1.2, 0.5, 10, 0, 0, 4, 0, 0, 5, 1, 1, 0), 0.0);
        assertEquals(0.37, RayBox.hit(0, 1.2, 0.5, 10, 0, 0, 4, 0, 0, 5, 1, 1, 0.3), 1e-12);
    }

    @Test
    void segmentThatEndsShortMisses() {
        assertEquals(-1.0, RayBox.hit(0, 0.5, 0.5, 3, 0, 0, 4, 0, 0, 5, 1, 1, 0), 0.0);
    }

    @Test
    void segmentStartingInsideEntersAtZero() {
        assertEquals(0.0, RayBox.hit(4.5, 0.5, 0.5, 1, 0, 0, 4, 0, 0, 5, 1, 1, 0), 0.0);
    }

    @Test
    void segmentPointingAwayMisses() {
        assertEquals(-1.0, RayBox.hit(0, 0.5, 0.5, -10, 0, 0, 4, 0, 0, 5, 1, 1, 0), 0.0);
    }

    @Test
    void diagonalSegmentHitsTheBoxItCrosses() {
        double t = RayBox.hit(0, 0, 0, 10, 10, 0, 4, 4, -1, 6, 6, 1, 0);
        assertEquals(0.4, t, 1e-12);
    }

    @Test
    void parallelSegmentOutsideTheSlabMisses() {
        assertEquals(-1.0, RayBox.hit(0, 5, 0.5, 10, 0, 0, 4, 0, 0, 5, 1, 1, 0), 0.0);
    }
}
