package com.gregguns.machine.repair;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GantryShapeTest {

    private static final String[][][] PIECES = { GantryShape.HEAD, GantryShape.BAY, GantryShape.TAIL };

    @Test
    void everyPieceIsSevenWideAndSevenTall() {
        for (String[][] piece : PIECES) {
            for (String[] aisle : piece) {
                assertEquals(GantryShape.HEIGHT, aisle.length);
                for (String row : aisle) {
                    assertEquals(GantryShape.WIDTH, row.length(), row);
                }
            }
        }
    }

    @Test
    void thePiecesHaveTheSheetsLengths() {
        assertEquals(GantryShape.HEAD_LENGTH, GantryShape.HEAD.length);
        assertEquals(GantryShape.BAY_LENGTH, GantryShape.BAY.length);
        assertEquals(GantryShape.TAIL_LENGTH, GantryShape.TAIL.length);
        assertEquals(11, GantryShape.totalLength(GantryShape.DEFAULT_BAYS));
    }

    @Test
    void everyRowIsLeftRightSymmetric() {
        for (String[][] piece : PIECES) {
            for (String[] aisle : piece) {
                for (String row : aisle) {
                    assertEquals(
                        row,
                        new StringBuilder(row).reverse()
                            .toString(),
                        row);
                }
            }
        }
    }

    @Test
    void theControllerIsAloneInTheFrontWall() {
        assertEquals(1, GantryShape.count(GantryShape.HEAD, "~"));
        assertEquals(0, GantryShape.count(GantryShape.BAY, "~"));
        assertEquals(0, GantryShape.count(GantryShape.TAIL, "~"));
        String row = GantryShape.HEAD[0][GantryShape.CONTROLLER_ROW];
        assertEquals('~', row.charAt(GantryShape.CONTROLLER_COLUMN));
    }

    @Test
    void headBlockCountsMatchTheSheet() {
        // multiblocks.md "Block counts": head B 14, L 20, G 10, R 19, H 1.
        assertEquals(14, GantryShape.count(GantryShape.HEAD, "B"));
        assertEquals(20, GantryShape.count(GantryShape.HEAD, "L"));
        assertEquals(10, GantryShape.count(GantryShape.HEAD, "G"));
        assertEquals(19, GantryShape.count(GantryShape.HEAD, "R"));
        assertEquals(1, GantryShape.count(GantryShape.HEAD, "H"));
        // Interior: two booth pipe runs and the first two floor rails.
        assertEquals(2, GantryShape.count(GantryShape.HEAD, "P"));
        assertEquals(2, GantryShape.count(GantryShape.HEAD, "F"));
    }

    @Test
    void bayBlockCountsMatchTheSheet() {
        // Bay: B 14, L 10, R 6, T 6, H 1.
        assertEquals(14, GantryShape.count(GantryShape.BAY, "B"));
        assertEquals(10, GantryShape.count(GantryShape.BAY, "L"));
        assertEquals(6, GantryShape.count(GantryShape.BAY, "R"));
        assertEquals(6, GantryShape.count(GantryShape.BAY, "T"));
        assertEquals(1, GantryShape.count(GantryShape.BAY, "H"));
        // Interior: gearbox and two motors for the hoist, four rail blocks plus the hook, two pipes.
        assertEquals(1, GantryShape.count(GantryShape.BAY, "X"));
        assertEquals(2, GantryShape.count(GantryShape.BAY, "N"));
        assertEquals(5, GantryShape.count(GantryShape.BAY, "F"));
        assertEquals(2, GantryShape.count(GantryShape.BAY, "P"));
    }

    @Test
    void onlyTheDocumentedCharactersAppear() {
        String allowed = "RBHLGTFXNP~- ";
        for (String[][] piece : PIECES) {
            for (String[] aisle : piece) {
                for (String row : aisle) {
                    for (char c : row.toCharArray()) {
                        assertTrue(allowed.indexOf(c) >= 0, "unexpected '" + c + "' in " + row);
                    }
                }
            }
        }
    }

    @Test
    void interiorPartsNeverSitOnAHatchFaceOrReplaceACasing() {
        // The head's front aisle and the tail keep their casings; the bay floor keeps its casing and cradle row.
        assertEquals(0, GantryShape.count(new String[][] { GantryShape.HEAD[0] }, "FXNP"));
        assertEquals(0, GantryShape.count(GantryShape.TAIL, "FXNP"));
        assertEquals(6, GantryShape.count(GantryShape.BAY, "R"));
        assertEquals(19, GantryShape.count(GantryShape.HEAD, "R"));
    }

    @Test
    void theHoistHangsInOneUnbrokenColumnUnderTheBeam() {
        // Roof hoist, beam, gearbox, frame hook: the middle column of the portal frame.
        String[][] bay = GantryShape.BAY;
        StringBuilder column = new StringBuilder();
        for (int row = 0; row <= 3; row++) {
            column.append(bay[0][row].charAt(3));
        }
        assertEquals("HBXF", column.toString());
    }

    @Test
    void theRailsRunUnbrokenAlongTheFloorOfEveryBay() {
        // Both rails sit one above the floor, in both aisles of the bay, so they join bay to bay.
        for (String[] aisle : GantryShape.BAY) {
            assertEquals('F', aisle[5].charAt(1));
            assertEquals('F', aisle[5].charAt(5));
        }
    }

    @Test
    void tailBlockCountsMatchTheSheet() {
        // Tail: B 7, L 10, R 25.
        assertEquals(7, GantryShape.count(GantryShape.TAIL, "B"));
        assertEquals(10, GantryShape.count(GantryShape.TAIL, "L"));
        assertEquals(25, GantryShape.count(GantryShape.TAIL, "R"));
    }

    @Test
    void baysAreClampedAndSetTheParallels() {
        assertEquals(2, GantryShape.clampBays(0));
        assertEquals(8, GantryShape.clampBays(99));
        assertEquals(4, GantryShape.parallels(4));
        assertEquals(GantryShape.HEAD_LENGTH, GantryShape.bayDepth(0));
        assertEquals(GantryShape.HEAD_LENGTH + 2 * GantryShape.BAY_LENGTH, GantryShape.tailDepth(2));
    }

    @Test
    void theAssembledShapeIsNeverACuboid() {
        // GGS-08-82: an outer-layer cell is empty at every size (the open portal arches and the roof corners).
        for (int bays = GantryShape.MIN_BAYS; bays <= GantryShape.MAX_BAYS; bays++) {
            assertTrue(GantryShape.hasEmptyOuterCell(bays), "bays " + bays);
        }
    }
}
