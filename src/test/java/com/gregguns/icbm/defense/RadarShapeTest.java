package com.gregguns.icbm.defense;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RadarShapeTest {

    private static String[][] shape() {
        return RadarShape.PIECES[0];
    }

    @Test
    void dimensionsMatchTheDeclaredSize() {
        assertEquals(RadarShape.DEPTH, shape().length);
        for (String[] aisle : shape()) {
            assertEquals(RadarShape.HEIGHT, aisle.length);
            for (String row : aisle) {
                assertEquals(RadarShape.WIDTH, row.length());
            }
        }
    }

    @Test
    void exactlyOneControllerOnTheFrontFaceAtTheDeclaredCell() {
        assertEquals(1, RadarShape.count('~'));
        assertEquals('~', shape()[0][RadarShape.CONTROLLER_ROW].charAt(RadarShape.CONTROLLER_COLUMN));
    }

    @Test
    void theRadarIsSmall() {
        assertTrue(RadarShape.WIDTH <= 3 && RadarShape.DEPTH <= 3 && RadarShape.HEIGHT <= 3);
        assertTrue(RadarShape.casingPositions() <= 20, "a small radar costs a small pile of casing");
    }

    @Test
    void casingMinimumIsReachableAndLeavesRoomForTheHatches() {
        assertEquals(RadarShape.count('C') + RadarShape.count('H'), RadarShape.casingPositions());
        assertTrue(RadarShape.CASING_MINIMUM <= RadarShape.casingPositions());
        assertTrue(RadarShape.casingPositions() - RadarShape.CASING_MINIMUM >= 2, "two hatches may replace casing");
    }

    @Test
    void thePedestalStandsInTheMiddleOfTheTopAndNothingElseDoes() {
        assertEquals(1, RadarShape.count('C'));
        assertEquals('C', shape()[1][0].charAt(1), "the pedestal is in the middle aisle, top row, middle column");
        // The dish plane is the top of the top row; the controller floor is CONTROLLER_ROW rows below it.
        assertEquals(RadarShape.CONTROLLER_ROW + 1, RadarShape.DISH_HEIGHT);
    }

    @Test
    void onlyKnownShapeCharactersAppear() {
        for (String[] aisle : shape()) {
            for (String row : aisle) {
                for (char c : row.toCharArray()) {
                    assertTrue("CH~ ".indexOf(c) >= 0, "unexpected '" + c + "'");
                }
            }
        }
    }
}
