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
        assertEquals(12 * 7, SiloShape.count("C"));
        assertEquals(9, SiloShape.count("P"));
        assertEquals(12, SiloShape.count("F"));
        assertEquals(12, SiloShape.count("B"));
        assertEquals(96, SiloShape.casingPositions());
        assertTrue(SiloShape.CASING_MINIMUM < SiloShape.casingPositions());
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
