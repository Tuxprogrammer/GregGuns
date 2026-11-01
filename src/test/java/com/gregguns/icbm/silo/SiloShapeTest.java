package com.gregguns.icbm.silo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Pins the silo's shape to the spec sheet (missile-silo.md, Structure). */
class SiloShapeTest {

    @Test
    void everyAisleAndRowHasTheDeclaredSize() {
        String[][] p = SiloShape.piece();
        assertEquals(SiloShape.DEPTH, p.length);
        for (String[] aisle : p) {
            assertEquals(SiloShape.HEIGHT, aisle.length);
            for (String row : aisle) {
                assertEquals(SiloShape.WIDTH, row.length());
            }
        }
    }

    @Test
    void theControllerSitsInTheMiddleOfTheFrontGlassRing() {
        String[][] p = SiloShape.piece();
        assertEquals('~', p[0][SiloShape.CONTROLLER_ROW].charAt(SiloShape.CONTROLLER_COLUMN));
        assertEquals(1, SiloShape.count("~"));
        // Rows 8 to 10 (the three rings above the floor) are glass.
        for (int r = 8; r <= 10; r++) {
            assertEquals('G', p[1][r].charAt(0), "glass ring " + r);
        }
        assertEquals('C', p[1][7].charAt(0), "casing above the glass");
    }

    @Test
    void theBlockCountsMatchTheSheet() {
        assertEquals(35, SiloShape.count("G"));
        assertEquals(6, SiloShape.count("H"));
        assertEquals(0, SiloShape.count("W"));
        assertEquals(12 * 7, SiloShape.count("C"));
        assertEquals(9, SiloShape.count("P"));
        assertEquals(12, SiloShape.count("F"));
        assertEquals(12, SiloShape.count("B"));
        assertEquals(102, SiloShape.casingPositions());
        assertEquals(84, SiloShape.CASING_MINIMUM);
        assertTrue(SiloShape.CASING_MINIMUM < SiloShape.casingPositions());
        // D117: a hatch may replace a wall, hatch-column, glass or floor-ring block or the controller, never the rim or
        // the pad.
        assertEquals(84 + 6 + 35 + 12 + 1, SiloShape.hatchPositions());
    }

    /** The owner's build (D299): a 3x3 glass window around the controller, a hatch column on each side, flush. */
    @Test
    void theFrontIsFiveWideInTheWindowRows() {
        String[][] p = SiloShape.piece();
        assertEquals("HGGGH", p[0][8]);
        assertEquals("HG~GH", p[0][9]);
        assertEquals("HGGGH", p[0][10]);
        for (int r = 0; r < SiloShape.HEIGHT; r++) {
            if (r >= 8 && r <= 10) continue;
            assertEquals(' ', p[0][r].charAt(0), "front corner row " + r);
            assertEquals(' ', p[0][r].charAt(4), "front corner row " + r);
        }
        assertEquals(0, SiloShape.HATCH_COLUMN_RIGHT);
        assertEquals(4, SiloShape.HATCH_COLUMN_LEFT);
        assertEquals(SiloShape.HATCH_COLUMN_RIGHT, SiloShape.LAUNCH_CONTROL_COLUMN);
        assertEquals('H', p[0][SiloShape.CONTROLLER_ROW - SiloShape.LAUNCH_CONTROL_RISE].charAt(0));
        // The name sign hangs on the concrete over the window.
        assertEquals(
            'C',
            p[0][SiloShape.CONTROLLER_ROW - SiloShape.NAME_SIGN_RISE].charAt(SiloShape.CONTROLLER_COLUMN));
    }

    @Test
    void theShaftIsOpenAndTheFloorUnderItIsAPad() {
        String[][] p = SiloShape.piece();
        for (int a = 1; a <= 3; a++) {
            for (int r = 0; r < SiloShape.HEIGHT - 1; r++) {
                assertEquals("   ", p[a][r].substring(1, 4), "shaft aisle " + a + " row " + r);
            }
            assertEquals("PPP", p[a][SiloShape.HEIGHT - 1].substring(1, 4));
        }
    }

    @Test
    void thePlayerCanSeeInThroughGlassOnEverySide() {
        String[][] p = SiloShape.piece();
        for (int r = 8; r <= 10; r++) {
            assertTrue(
                p[0][r].contains("G") && p[4][r].contains("G") && p[2][r].startsWith("G") && p[2][r].endsWith("G"));
        }
    }

    @Test
    void theSilhouetteIsNotACube() {
        assertTrue(SiloShape.hasEmptyOuterCell());
    }

    @Test
    void thePlayerCanReachTheShaftCenterFromTheController() {
        assertEquals(2, SiloShape.SHAFT_CENTER_DEPTH);
        assertEquals(SiloShape.DEPTH / 2, SiloShape.SHAFT_CENTER_DEPTH);
    }
}
