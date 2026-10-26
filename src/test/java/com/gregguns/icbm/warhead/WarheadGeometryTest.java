package com.gregguns.icbm.warhead;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WarheadGeometryTest {

    @Test
    void theBoxIsACenteredFullHeightColumn() {
        double[] b = WarheadGeometry.bounds(0.5);
        assertEquals(0.25, b[0], 1e-9);
        assertEquals(0.75, b[3], 1e-9);
        assertEquals(0.0, b[1], 1e-9);
        assertEquals(1.0, b[4], 1e-9);
        assertEquals(b[0], b[2], 1e-9);
        assertEquals(b[3], b[5], 1e-9);
    }

    @Test
    void everyCasingFitsOneBlock() {
        for (WarheadCasing c : WarheadCasing.values()) {
            assertTrue(WarheadGeometry.boxWidth(c.boxWidth()) < 1.0);
            assertEquals(1, c.heightBlocks());
        }
    }
}
