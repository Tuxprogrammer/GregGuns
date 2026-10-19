package com.gregguns.gun.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

/** The Power Hammer shockwave geometry: the box, its center, its order and its damage. */
// Spec: docs/techguns-spec/02-guns/sheets-launchers-tools-grenades.md (Power Hammer)
class ShockwaveTest {

    @Test
    void damageIsTwoAndAHalfPerRoundAndNeverUnderTheMinimum() {
        assertEquals(2.5, Shockwave.damage(1));
        assertEquals(2.5, Shockwave.damage(0));
        assertEquals(12.5, Shockwave.damage(5));
        assertEquals(25.0, Shockwave.damage(10));
    }

    @Test
    void theCenterStopsAtTheWallButNeverNearerThanHalfTheBox() {
        assertEquals(3.0, Shockwave.centerDistance(-1.0));
        assertEquals(3.0, Shockwave.centerDistance(8.0));
        assertEquals(2.6, Shockwave.centerDistance(2.6), 1e-9);
        assertEquals(2.0, Shockwave.centerDistance(0.4), 1e-9);
    }

    @Test
    void theBoxIsFourByFourByFourDistinctCellsAroundTheCenter() {
        List<int[]> cells = Shockwave.cells(0.5, 1.62, 0.5, 1, 0, 0, 3.0);
        assertEquals(64, cells.size());
        Set<String> distinct = new HashSet<>();
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        for (int[] c : cells) {
            distinct.add(c[0] + "," + c[1] + "," + c[2]);
            minX = Math.min(minX, c[0]);
            maxX = Math.max(maxX, c[0]);
        }
        assertEquals(64, distinct.size());
        // Center x = 3.5: cell centers within 2 of it are 1.5, 2.5, 3.5, 4.5 -> cells 1..4.
        assertEquals(1, minX);
        assertEquals(4, maxX);
    }

    @Test
    void cellsComeNearestToTheEyeFirst() {
        List<int[]> cells = Shockwave.cells(0.5, 1.62, 0.5, 1, 0, 0, 3.0);
        double last = -1;
        for (int[] c : cells) {
            double d = Math.pow(c[0] + 0.5 - 0.5, 2) + Math.pow(c[1] + 0.5 - 1.62, 2) + Math.pow(c[2] + 0.5 - 0.5, 2);
            assertTrue(d >= last - 1e-9, "sorted by distance");
            last = d;
        }
    }

    @Test
    void theEntityBoxIsTheSameCube() {
        double[] b = Shockwave.box(0.5, 1.62, 0.5, 1, 0, 0, 3.0);
        assertEquals(1.5, b[0], 1e-9);
        assertEquals(5.5, b[3], 1e-9);
        assertEquals(b[4] - b[1], 4.0, 1e-9);
    }
}
