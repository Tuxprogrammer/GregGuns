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

    @Test
    void flagHangsFromTheWestFaceAndIsChained() {
        // Hinge world positions at rest: pole center is 0, the flag reaches west to -11 (notes: block X 7 to -11, 8 is
        // the center).
        float hinge = 0;
        float[] starts = { -1, -7, -13 };
        for (int i = 0; i < 3; i++) {
            FlagPostGeometry.Panel p = FlagPostGeometry.PANELS.get(i);
            hinge += p.hingeX;
            assertEquals(starts[i] + 0, hinge, 1e-6);
            assertEquals(hinge - 6, hinge + p.box.x, 1e-6);
            assertEquals(6, p.box.sizeX, 1e-6);
            assertEquals(0, p.box.sizeZ, 1e-6);
            assertEquals(47.5F, p.box.y, 1e-6);
            assertEquals(14, p.box.sizeY, 1e-6);
        }
        assertEquals(60, FlagPostGeometry.PANELS.get(0).periodTicks);
        assertEquals(50, FlagPostGeometry.PANELS.get(1).periodTicks);
        assertEquals(40, FlagPostGeometry.PANELS.get(2).periodTicks);
    }

    @Test
    void reachCoversTheFlagSpan() {
        assertEquals(19.0F, FlagPostGeometry.horizontalReach(), 1e-6);
    }
}
