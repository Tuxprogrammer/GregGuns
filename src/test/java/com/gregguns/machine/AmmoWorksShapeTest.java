package com.gregguns.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.gregguns.api.Tier;

class AmmoWorksShapeTest {

    private static final String[][][] PIECES = { AmmoWorksShape.FEEDER, AmmoWorksShape.BAY, AmmoWorksShape.STATION };

    @Test
    void everyPieceIsFiveWideAndSevenTall() {
        for (String[][] piece : PIECES) {
            for (String[] aisle : piece) {
                assertEquals(AmmoWorksShape.HEIGHT, aisle.length);
                for (String row : aisle) {
                    assertEquals(AmmoWorksShape.WIDTH, row.length(), row);
                }
            }
        }
    }

    @Test
    void thePiecesHaveTheSheetsLengths() {
        assertEquals(AmmoWorksShape.FEEDER_LENGTH, AmmoWorksShape.FEEDER.length);
        assertEquals(AmmoWorksShape.BAY_LENGTH, AmmoWorksShape.BAY.length);
        assertEquals(AmmoWorksShape.STATION_LENGTH, AmmoWorksShape.STATION.length);
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
    void theControllerIsAloneInTheFrontAisleAtTheDocumentedCell() {
        // multiblocks.md: front, middle, second layer from the bottom.
        assertEquals(1, AmmoWorksShape.count(AmmoWorksShape.FEEDER, "~"));
        assertEquals(0, AmmoWorksShape.count(AmmoWorksShape.BAY, "~"));
        assertEquals(0, AmmoWorksShape.count(AmmoWorksShape.STATION, "~"));
        String row = AmmoWorksShape.FEEDER[0][AmmoWorksShape.CONTROLLER_ROW];
        assertEquals('~', row.charAt(AmmoWorksShape.CONTROLLER_COLUMN));
        assertEquals(AmmoWorksShape.HEIGHT - 2, AmmoWorksShape.CONTROLLER_ROW);
    }

    @Test
    void feederBlockCountsMatchTheSheet() {
        // multiblocks.md "Block counts": feeder 18 F, 36 A, 2 I, 4 G, 4 P, 2 N.
        assertEquals(18, AmmoWorksShape.count(AmmoWorksShape.FEEDER, "F"));
        assertEquals(36, AmmoWorksShape.count(AmmoWorksShape.FEEDER, "A"));
        assertEquals(2, AmmoWorksShape.count(AmmoWorksShape.FEEDER, "I"));
        assertEquals(4, AmmoWorksShape.count(AmmoWorksShape.FEEDER, "G"));
        assertEquals(4, AmmoWorksShape.count(AmmoWorksShape.FEEDER, "P"));
        assertEquals(2, AmmoWorksShape.count(AmmoWorksShape.FEEDER, "N"));
    }

    @Test
    void bayBlockCountsMatchTheSheet() {
        // Each bay: 38 A, 3 M, 3 I, 6 G, 2 R, 6 P, 2 B.
        assertEquals(38, AmmoWorksShape.count(AmmoWorksShape.BAY, "A"));
        assertEquals(3, AmmoWorksShape.count(AmmoWorksShape.BAY, "M"));
        assertEquals(3, AmmoWorksShape.count(AmmoWorksShape.BAY, "I"));
        assertEquals(6, AmmoWorksShape.count(AmmoWorksShape.BAY, "G"));
        assertEquals(2, AmmoWorksShape.count(AmmoWorksShape.BAY, "R"));
        assertEquals(6, AmmoWorksShape.count(AmmoWorksShape.BAY, "P"));
        assertEquals(2, AmmoWorksShape.count(AmmoWorksShape.BAY, "B"));
    }

    @Test
    void stationBlockCountsMatchTheSheet() {
        // Station: 16 O, 55 A, 3 I, 12 G, 2 R, 6 P, 4 B, 3 Y.
        assertEquals(16, AmmoWorksShape.count(AmmoWorksShape.STATION, "O"));
        assertEquals(55, AmmoWorksShape.count(AmmoWorksShape.STATION, "A"));
        assertEquals(3, AmmoWorksShape.count(AmmoWorksShape.STATION, "I"));
        assertEquals(12, AmmoWorksShape.count(AmmoWorksShape.STATION, "G"));
        assertEquals(2, AmmoWorksShape.count(AmmoWorksShape.STATION, "R"));
        assertEquals(6, AmmoWorksShape.count(AmmoWorksShape.STATION, "P"));
        assertEquals(4, AmmoWorksShape.count(AmmoWorksShape.STATION, "B"));
        assertEquals(3, AmmoWorksShape.count(AmmoWorksShape.STATION, "Y"));
    }

    @Test
    void oneLampHangsUnderTheCeilingOfTheFeederEveryBayAndTheStation() {
        // Interior lighting: one 'S' per section, in the open air above the belt, never on a wall.
        assertEquals(1, AmmoWorksShape.count(AmmoWorksShape.FEEDER, "S"));
        assertEquals(1, AmmoWorksShape.count(AmmoWorksShape.BAY, "S"));
        assertEquals(1, AmmoWorksShape.count(AmmoWorksShape.STATION, "S"));
        for (String[][] piece : PIECES) {
            for (String[] aisle : piece) {
                for (String row : aisle) {
                    int at = row.indexOf('S');
                    if (at >= 0) {
                        assertEquals(AmmoWorksShape.CONTROLLER_COLUMN, at, "lamp is on the center line: " + row);
                        assertEquals("APSPA", row);
                    }
                }
            }
        }
    }

    @Test
    void onlyTheDocumentedCharactersAppear() {
        String allowed = "FAOMIGRPBNYS~- ";
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
    void windowsSitInTheSideWallsAndNeverInTheFrontOrBackWall() {
        for (String[][] piece : PIECES) {
            for (int i = 0; i < piece.length; i++) {
                for (int r = 0; r < AmmoWorksShape.HEIGHT; r++) {
                    String row = piece[i][r];
                    for (int c = 1; c < AmmoWorksShape.WIDTH - 1; c++) {
                        assertTrue(row.charAt(c) != 'G', "glass inside the hall at " + r + "," + c);
                    }
                }
            }
        }
        // The controller's aisle and the output wall stay hatch faces: no glass there.
        assertEquals(0, AmmoWorksShape.count(new String[][] { AmmoWorksShape.FEEDER[0] }, "G"));
        assertEquals(0, AmmoWorksShape.count(new String[][] { AmmoWorksShape.STATION[3] }, "G"));
    }

    @Test
    void hatchFacesKeepOnlyHatchCapableCasings() {
        // Hatches may only replace F (feeder) and O (packing) cells: the interior parts never sit in those.
        assertEquals(18, AmmoWorksShape.count(AmmoWorksShape.FEEDER, "F"));
        assertEquals(16, AmmoWorksShape.count(AmmoWorksShape.STATION, "O"));
        assertEquals(0, AmmoWorksShape.count(AmmoWorksShape.BAY, "FO"));
    }

    @Test
    void theBayPressHangsFromTheRamHousingOverTheBelt() {
        // Roof ram housing, then a steel rod, then the press head, then the belt: one unbroken column at the middle.
        String[][] bay = AmmoWorksShape.BAY;
        assertEquals(
            "MMRRMI",
            column(bay[1], 1, 2) + column(bay[1], 2, 2)
                + column(bay[1], 3, 2)
                + column(bay[1], 4, 2)
                + column(bay[1], 5, 2)
                + column(bay[1], 6, 2));
    }

    private static String column(String[] aisle, int row, int col) {
        return String.valueOf(aisle[row].charAt(col));
    }

    @Test
    void theBeltRunsDownTheMiddleOfTheFloorOfEveryPieceButTheBackWall() {
        for (String[][] piece : PIECES) {
            for (int i = 0; i < piece.length; i++) {
                String floor = piece[i][AmmoWorksShape.HEIGHT - 1];
                boolean backWall = piece == AmmoWorksShape.STATION && i == piece.length - 1;
                char expected = backWall ? 'A' : 'I';
                if (piece == AmmoWorksShape.FEEDER && i == 0) {
                    // The feeder's front aisle is the face with the controller; its floor has no belt.
                    expected = 'A';
                }
                assertEquals(expected, floor.charAt(2), "floor of aisle " + i);
            }
        }
    }

    @Test
    void theDefaultHallIs19LongAndNeeds277Casings() {
        // multiblocks.md: n = 4 is 5 x 7 x 19; 289 casing positions, check is 277.
        assertEquals(19, AmmoWorksShape.totalLength(4));
        assertEquals(289, AmmoWorksShape.casingPositions(4));
        assertEquals(277, AmmoWorksShape.casingMinimum(4));
    }

    @Test
    void theHallRunsFrom13To31Long() {
        assertEquals(13, AmmoWorksShape.totalLength(AmmoWorksShape.MIN_BAYS));
        assertEquals(31, AmmoWorksShape.totalLength(AmmoWorksShape.MAX_BAYS));
    }

    @Test
    void casingPositionsGrowByOneBayEach() {
        for (int n = AmmoWorksShape.MIN_BAYS; n < AmmoWorksShape.MAX_BAYS; n++) {
            assertEquals(41, AmmoWorksShape.casingPositions(n + 1) - AmmoWorksShape.casingPositions(n));
        }
    }

    @Test
    void piecesTileTheHallWithoutGapsOrOverlap() {
        for (int n = AmmoWorksShape.MIN_BAYS; n <= AmmoWorksShape.MAX_BAYS; n++) {
            assertEquals(AmmoWorksShape.FEEDER_LENGTH, AmmoWorksShape.bayDepth(0));
            assertEquals(AmmoWorksShape.bayDepth(n - 1) + AmmoWorksShape.BAY_LENGTH, AmmoWorksShape.stationDepth(n));
            assertEquals(AmmoWorksShape.stationDepth(n) + AmmoWorksShape.STATION_LENGTH, AmmoWorksShape.totalLength(n));
        }
    }

    @Test
    void parallelsAreFourTimesBaysTimesTier() {
        // multiblocks.md: HV with 4 bays is 48, HV with 8 bays is 96.
        int hv = Tier.HV.voltageIndex();
        assertEquals(48, AmmoWorksShape.parallels(4, hv));
        assertEquals(96, AmmoWorksShape.parallels(8, hv));
        assertEquals(4 * 2 * 1, AmmoWorksShape.parallels(2, Tier.LV.voltageIndex()));
    }

    @Test
    void parallelsAreNeverBelowOne() {
        assertEquals(1, AmmoWorksShape.parallels(0, 0));
        assertEquals(1, AmmoWorksShape.parallels(AmmoWorksShape.MIN_BAYS, 0));
    }

    @Test
    void beltTierTwoGivesTheSheetsDurationMultiplier() {
        // multiblocks.md: "Belt tier 2 gives 0.571".
        assertEquals(0.571, AmmoWorksShape.speedMultiplier(2), 0.001);
    }

    @Test
    void aFasterBeltAlwaysShortensTheRecipe() {
        double previous = Double.MAX_VALUE;
        for (int tier = 1; tier <= 8; tier++) {
            double multiplier = AmmoWorksShape.speedMultiplier(tier);
            assertTrue(multiplier < previous && multiplier > 0.0 && multiplier < 1.0, "tier " + tier);
            previous = multiplier;
        }
    }

    @Test
    void anUnreadBeltDoesNotSpeedUpMoreThanTheSlowestBelt() {
        // The belt tier is -1 until a check reads it; it must not give a multiplier above 1 or below the tier 0 value.
        assertEquals(AmmoWorksShape.speedMultiplier(0), AmmoWorksShape.speedMultiplier(-1), 1e-12);
    }

    @Test
    void clampKeepsTheBayCountInRange() {
        assertEquals(AmmoWorksShape.MIN_BAYS, AmmoWorksShape.clampBays(0));
        assertEquals(AmmoWorksShape.MIN_BAYS, AmmoWorksShape.clampBays(1));
        assertEquals(5, AmmoWorksShape.clampBays(5));
        assertEquals(AmmoWorksShape.MAX_BAYS, AmmoWorksShape.clampBays(64));
    }

    @Test
    void theWorkedExampleOfSixteenRunsFitsEightBaysAtHv() {
        // multiblocks.md worked example: HV, 8 bays, 16 LV runs at once is below the 96 parallel cap.
        assertTrue(16 <= AmmoWorksShape.parallels(8, Tier.HV.voltageIndex()));
    }
}
