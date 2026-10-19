package com.gregguns.utility.rampart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** GGS-08-74, GGS-08-75 and GGS-08-82: the Rampart's pieces, counts and silhouette. */
class RampartShapeTest {

    @Test
    void theGatehouseHasTheSheetsBlockCounts() {
        // multiblocks.md: Gatehouse T 47, G 2; the controller is one more cell.
        assertEquals(47, RampartShape.count(RampartShape.GATE, "T"));
        assertEquals(2, RampartShape.count(RampartShape.GATE, "G"));
        assertEquals(1, RampartShape.count(RampartShape.GATE, "~"));
        assertEquals(0, RampartShape.count(RampartShape.GATE, "H"));
    }

    @Test
    void aBayHasOneHatchAndTwentyOnePlatingBlocks() {
        assertEquals(21, RampartShape.count(RampartShape.BAY, "P"));
        assertEquals(1, RampartShape.count(RampartShape.BAY, "H"));
    }

    @Test
    void theCapIsFifteenPlatingBlocks() {
        assertEquals(15, RampartShape.count(RampartShape.CAP, "P"));
        assertEquals(0, RampartShape.count(RampartShape.CAP, "H"));
    }

    @Test
    void theWallLengthIsFourPlusTwoPerMount() {
        // GGS-08-74: (4 + 2n) long; the default n = 6 is 16 x 6 x 3.
        assertEquals(8, RampartShape.totalLength(2));
        assertEquals(16, RampartShape.totalLength(RampartShape.DEFAULT_MOUNTS));
        assertEquals(24, RampartShape.totalLength(10));
        assertEquals(6, RampartShape.GATE_HEIGHT);
        assertEquals(3, RampartShape.DEPTH);
    }

    @Test
    void theDefaultWallIsAboutOneHundredNinetyPlatingBlocks() {
        // recipes.md: "the default Rampart is about 190 plating blocks"
        assertEquals(188, RampartShape.platingPositions(RampartShape.DEFAULT_MOUNTS));
    }

    @Test
    void mountCountsAreClampedToTwoToTen() {
        assertEquals(2, RampartShape.clampMounts(0));
        assertEquals(2, RampartShape.clampMounts(1));
        assertEquals(7, RampartShape.clampMounts(7));
        assertEquals(10, RampartShape.clampMounts(64));
    }

    @Test
    void everyPieceIsARectangleOfItsDeclaredSize() {
        check(RampartShape.GATE, RampartShape.GATE_HEIGHT, RampartShape.GATE_LENGTH);
        check(RampartShape.BAY, RampartShape.WALL_HEIGHT, RampartShape.BAY_LENGTH);
        check(RampartShape.CAP, RampartShape.WALL_HEIGHT, RampartShape.CAP_LENGTH);
    }

    private static void check(String[][] piece, int rows, int columns) {
        assertEquals(RampartShape.DEPTH, piece.length);
        for (String[] aisle : piece) {
            assertEquals(rows, aisle.length);
            for (String row : aisle) {
                assertEquals(columns, row.length());
            }
        }
    }

    @Test
    void theControllerSitsInTheFrontWallAtTheSecondLayer() {
        String row = RampartShape.GATE[0][RampartShape.GATE_CONTROLLER_ROW];
        assertEquals('~', row.charAt(RampartShape.CONTROLLER_COLUMN));
        // Layer y1 of y5..y0 is row 4 of 6; in a wall piece (y4..y0) the same layer is row 3 of 5.
        assertEquals(RampartShape.GATE_HEIGHT - 2, RampartShape.GATE_CONTROLLER_ROW);
        assertEquals(RampartShape.WALL_HEIGHT - 2, RampartShape.WALL_CONTROLLER_ROW);
    }

    @Test
    void theHatchIsOnTheSillOfTheFrontRowOneLayerAboveTheController() {
        // y2 against y1: row 2 of the bay (y4, y3, y2), front aisle, second column.
        assertEquals('H', RampartShape.BAY[0][2].charAt(1));
        assertEquals(1, RampartShape.hatchRise());
        assertEquals(RampartShape.WALL_CONTROLLER_ROW - 1, 2);
    }

    @Test
    void offsetsWalkTheWallToTheRight() {
        assertEquals(-2, RampartShape.bayOffset(0));
        assertEquals(-4, RampartShape.bayOffset(1));
        assertEquals(-2 - 2 * 5, RampartShape.bayOffset(5));
        assertEquals(-2 - 2 * 6, RampartShape.capOffset(6));
        assertEquals(3, RampartShape.hatchDistance(0));
        assertEquals(5, RampartShape.hatchDistance(1));
    }

    @Test
    void theWallIsNeverACube() {
        // GGS-08-82: crenels and the lower wall leave face cells of the bounding box empty.
        for (int n = RampartShape.MIN_MOUNTS; n <= RampartShape.MAX_MOUNTS; n++) {
            assertTrue(RampartShape.isNonConvex(n), n + " mounts");
        }
        // The cap and the bays are one layer lower than the gatehouse: the top face past the tower is open.
        assertTrue(RampartShape.emptyFaceCells(10) > RampartShape.emptyFaceCells(2));
    }

    @Test
    void crenelsAreAirAboveTheSillSoATurretHasRoom() {
        // Column 1 of a bay is air in the two top layers of every aisle.
        for (String[] aisle : RampartShape.BAY) {
            assertEquals('-', aisle[0].charAt(1));
            assertEquals('-', aisle[1].charAt(1));
        }
    }
}
