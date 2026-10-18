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
        // multiblocks.md "Block counts": feeder 18 F, 40 A, 2 I.
        assertEquals(18, AmmoWorksShape.count(AmmoWorksShape.FEEDER, "F"));
        assertEquals(40, AmmoWorksShape.count(AmmoWorksShape.FEEDER, "A"));
        assertEquals(2, AmmoWorksShape.count(AmmoWorksShape.FEEDER, "I"));
    }

    @Test
    void bayBlockCountsMatchTheSheet() {
        // Each bay: 44 A, 2 M, 3 I.
        assertEquals(44, AmmoWorksShape.count(AmmoWorksShape.BAY, "A"));
        assertEquals(2, AmmoWorksShape.count(AmmoWorksShape.BAY, "M"));
        assertEquals(3, AmmoWorksShape.count(AmmoWorksShape.BAY, "I"));
    }

    @Test
    void stationBlockCountsMatchTheSheet() {
        // Station: 16 O, 67 A, 3 I.
        assertEquals(16, AmmoWorksShape.count(AmmoWorksShape.STATION, "O"));
        assertEquals(67, AmmoWorksShape.count(AmmoWorksShape.STATION, "A"));
        assertEquals(3, AmmoWorksShape.count(AmmoWorksShape.STATION, "I"));
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
    void theDefaultHallIs19LongAndNeeds313Casings() {
        // multiblocks.md: n = 4 is 5 x 7 x 19; 325 casing positions, check is 313.
        assertEquals(19, AmmoWorksShape.totalLength(4));
        assertEquals(325, AmmoWorksShape.casingPositions(4));
        assertEquals(313, AmmoWorksShape.casingMinimum(4));
    }

    @Test
    void theHallRunsFrom13To31Long() {
        assertEquals(13, AmmoWorksShape.totalLength(AmmoWorksShape.MIN_BAYS));
        assertEquals(31, AmmoWorksShape.totalLength(AmmoWorksShape.MAX_BAYS));
    }

    @Test
    void casingPositionsGrowByOneBayEach() {
        for (int n = AmmoWorksShape.MIN_BAYS; n < AmmoWorksShape.MAX_BAYS; n++) {
            assertEquals(46, AmmoWorksShape.casingPositions(n + 1) - AmmoWorksShape.casingPositions(n));
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
