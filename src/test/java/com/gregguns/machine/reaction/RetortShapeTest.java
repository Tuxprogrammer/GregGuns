package com.gregguns.machine.reaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RetortShapeTest {

    @Test
    void everyLayerIsFiveByFive() {
        for (String[][] piece : new String[][][] { RetortShape.BASE, RetortShape.BULB, RetortShape.TOP }) {
            for (String[] layer : piece) {
                assertEquals(5, layer.length);
                for (String row : layer) {
                    assertEquals(5, row.length(), row);
                }
            }
        }
    }

    @Test
    void theCasingCountsMatchTheSpec() {
        // multiblocks.md: R 61 (cap 9, shoulder 20, low ring 12, floor 20), G = 16 + 12n, F 4.
        assertEquals(61, RetortShape.casingPositions(0));
        assertEquals(61, RetortShape.casingPositions(5));
        assertEquals(4, RetortShape.count(RetortShape.BASE, 'F'));
        assertEquals(16, RetortShape.count(RetortShape.TOP, 'G'));
        assertEquals(12, RetortShape.count(RetortShape.BULB, 'G'));
        assertEquals(51, RetortShape.casingMinimum());
    }

    @Test
    void thePlumbingCountsMatchTheSpec() {
        // multiblocks.md: pump 1, coils 4, pipes 2 on the floor plus 1 per bulb layer.
        assertEquals(1, RetortShape.count(RetortShape.BASE, 'U'));
        assertEquals(4, RetortShape.count(RetortShape.BASE, 'H'));
        assertEquals(2, RetortShape.count(RetortShape.BASE, 'P'));
        assertEquals(1, RetortShape.count(RetortShape.BULB, 'P'));
        assertEquals(0, RetortShape.count(RetortShape.TOP, 'P'));
    }

    @Test
    void onlyTheDocumentedCharactersAppear() {
        String allowed = "BSCGFPUH~-.";
        for (String[][] piece : new String[][][] { RetortShape.BASE, RetortShape.BULB, RetortShape.TOP }) {
            for (String[] layer : piece) {
                for (String row : layer) {
                    for (char c : row.toCharArray()) {
                        assertTrue(allowed.indexOf(c) >= 0, "unexpected '" + c + "' in " + row);
                    }
                }
            }
        }
    }

    @Test
    void theDipTubeRisesFromThePumpThroughEveryBulbAndStopsUnderTheNeck() {
        // The pump at the floor center, the pipe in the middle of the bulb, then air in the shoulder hole.
        assertEquals('U', RetortShape.BASE[0][2].charAt(2));
        assertEquals('P', RetortShape.BULB[0][2].charAt(2));
        assertEquals('-', RetortShape.TOP[3][2].charAt(2));
    }

    @Test
    void thereIsExactlyOneControllerInTheFloorLayerFrontMiddle() {
        assertEquals(1, RetortShape.count(RetortShape.BASE, '~'));
        assertEquals(0, RetortShape.count(RetortShape.TOP, '~'));
        assertEquals('~', RetortShape.BASE[RetortShape.BASE_ROWS_ABOVE][0].charAt(RetortShape.CONTROLLER_X));
        assertEquals('~', RetortShape.BASE[1][0].charAt(2));
    }

    @Test
    void heightAndParallelsFollowTheBulbCount() {
        assertEquals(9, RetortShape.height(2));
        assertEquals(10, RetortShape.height(3));
        assertEquals(13, RetortShape.height(6));
        assertEquals(4, RetortShape.parallels(2));
        assertEquals(12, RetortShape.parallels(6));
        assertEquals(RetortShape.BASE.length + RetortShape.TOP_LAYERS + 3, RetortShape.height(3));
    }

    @Test
    void bulbCountIsClamped() {
        assertEquals(2, RetortShape.clampBulbs(0));
        assertEquals(6, RetortShape.clampBulbs(40));
        assertEquals(4, RetortShape.clampBulbs(4));
    }

    @Test
    void theVesselIsNotACube() {
        // Width 5, depth 5, height at least 9: never a cube (the roster's rule).
        assertTrue(RetortShape.height(RetortShape.MIN_BULBS) > 5);
        assertTrue(RetortShape.height(RetortShape.MAX_BULBS) > 5);
    }

    @Test
    void theShoulderAndNeckHoleLineUp() {
        // The vessel interior is a single column of air through the shoulder and neck.
        assertEquals('-', RetortShape.TOP[3][2].charAt(2));
        assertEquals('-', RetortShape.TOP[1][2].charAt(2));
        assertEquals('-', RetortShape.TOP[2][2].charAt(2));
    }

    @Test
    void pressureRuleComparesGlassTierToRecipeVoltageTier() {
        // GGS-08-66
        int laserFocus = RetortShape.voltageIndexFor(1920);
        int core = RetortShape.voltageIndexFor(7680);
        assertEquals(4, laserFocus);
        assertEquals(5, core);
        assertTrue(RetortShape.glassAllows(5, core));
        assertTrue(RetortShape.glassAllows(6, core));
        assertFalse(RetortShape.glassAllows(4, core));
        assertTrue(RetortShape.glassAllows(4, laserFocus));
    }

    @Test
    void voltageIndexIsTheSmallestTierThatCoversTheEut() {
        assertEquals(0, RetortShape.voltageIndexFor(8));
        assertEquals(1, RetortShape.voltageIndexFor(9));
        assertEquals(1, RetortShape.voltageIndexFor(32));
        assertEquals(4, RetortShape.voltageIndexFor(2048));
        assertEquals(5, RetortShape.voltageIndexFor(2049));
    }
}
