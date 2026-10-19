package com.gregguns.utility.camp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FlagPostGeometryTest {

    @Test
    void hasThePostPartsAndThreePanels() {
        assertEquals(5, FlagPostGeometry.POST_BOXES.size());
        assertEquals(3, FlagPostGeometry.PANELS.size());
    }

    @Test
    void postIsFourBlocksTall() {
        float top = 0;
        for (FlagPostGeometry.Box b : FlagPostGeometry.POST_BOXES) {
            top = Math.max(top, b.y + b.sizeY);
        }
        assertEquals(FlagPostGeometry.HEIGHT, top, 1e-6);
    }

    @Test
    void swingFollowsThePeriods() {
        FlagPostGeometry.Panel first = FlagPostGeometry.PANELS.get(0);
        assertEquals(0.0F, FlagPostGeometry.swingDegrees(first, 0), 1e-4);
        assertEquals(30.0F, FlagPostGeometry.swingDegrees(first, 15), 1e-3);
        assertEquals(-30.0F, FlagPostGeometry.swingDegrees(first, 45), 1e-3);
        assertEquals(0.0F, FlagPostGeometry.swingDegrees(first, 60), 1e-3);
        assertEquals(30.0F, FlagPostGeometry.swingDegrees(FlagPostGeometry.PANELS.get(2), 10), 1e-3);
    }
}
