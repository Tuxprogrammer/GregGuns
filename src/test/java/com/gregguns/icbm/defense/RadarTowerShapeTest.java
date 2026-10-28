package com.gregguns.icbm.defense;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RadarTowerShapeTest {

    private static String[][] shape() {
        return RadarTowerShape.PIECES[0];
    }

    @Test
    void dimensionsMatchTheDeclaredSize() {
        assertEquals(RadarTowerShape.DEPTH, shape().length);
        for (String[] aisle : shape()) {
            assertEquals(RadarTowerShape.HEIGHT, aisle.length);
            for (String row : aisle) {
                assertEquals(RadarTowerShape.WIDTH, row.length());
            }
        }
    }

    @Test
    void exactlyOneControllerAtTheDeclaredCell() {
        assertEquals(1, RadarTowerShape.count('~'));
        assertEquals('~', shape()[0][RadarTowerShape.CONTROLLER_ROW].charAt(RadarTowerShape.CONTROLLER_COLUMN));
    }

    @Test
    void hatchesSitInTheBottomRingOnly() {
        assertTrue(RadarTowerShape.count('H') >= 3, "room for an energy and a maintenance hatch");
        assertEquals(0, countOutsideBottom(), "every H is in the bottom row");
    }

    private static int countOutsideBottom() {
        int n = 0;
        for (String[] aisle : shape()) {
            for (int r = 0; r < RadarTowerShape.HEIGHT - 1; r++) {
                for (char c : aisle[r].toCharArray()) {
                    n += c == 'H' ? 1 : 0;
                }
            }
        }
        return n;
    }

    @Test
    void casingMinimumIsReachableAndLeavesRoomForHatches() {
        assertEquals(RadarTowerShape.count('C') + RadarTowerShape.count('H'), RadarTowerShape.casingPositions());
        assertTrue(RadarTowerShape.CASING_MINIMUM <= RadarTowerShape.casingPositions());
        assertTrue(
            RadarTowerShape.casingPositions() - RadarTowerShape.CASING_MINIMUM >= 2,
            "two hatches may replace casing");
    }

    @Test
    void theMastIsAContinuousColumnThroughTheMiddleAisle() {
        String[] middle = shape()[1];
        for (int r = 0; r < RadarTowerShape.HEIGHT - 1; r++) {
            assertEquals('B', middle[r].charAt(1), "mast row " + r);
        }
    }

    @Test
    void onlyKnownShapeCharactersAppear() {
        for (String[] aisle : shape()) {
            for (String row : aisle) {
                for (char c : row.toCharArray()) {
                    assertTrue("CHB~ ".indexOf(c) >= 0, "unexpected '" + c + "'");
                }
            }
        }
    }

    @Test
    void theDishStandsOnTheTopOfTheMast() {
        // The controller's floor is CONTROLLER_ROW rows below the top row's floor; the plane is one more up.
        assertEquals(RadarTowerShape.CONTROLLER_ROW + 1, RadarTowerShape.DISH_HEIGHT);
    }
}
