package com.gregguns.core.testing;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

class ScenarioAreaTest {

    @Test
    void slotIsTheSortedIndexAndUnknownNamesFallBackToZero() {
        List<String> names = Arrays.asList("a.one", "b.two", "c.three");
        assertEquals(0, ScenarioArea.slotOf(names, "a.one"));
        assertEquals(2, ScenarioArea.slotOf(names, "c.three"));
        assertEquals(0, ScenarioArea.slotOf(names, "nope"));
    }

    @Test
    void slotsFillTheGridRowByRow() {
        assertArrayEquals(new int[] { 0, 0 }, ScenarioArea.offsetOf(0));
        assertArrayEquals(new int[] { ScenarioArea.STRIDE, 0 }, ScenarioArea.offsetOf(1));
        assertArrayEquals(
            new int[] { (ScenarioArea.GRID_COLUMNS - 1) * ScenarioArea.STRIDE, 0 },
            ScenarioArea.offsetOf(ScenarioArea.GRID_COLUMNS - 1));
        assertArrayEquals(new int[] { 0, ScenarioArea.STRIDE }, ScenarioArea.offsetOf(ScenarioArea.GRID_COLUMNS));
    }

    @Test
    void everyOriginIsAtLeast64BlocksFromEveryOther() {
        List<int[]> offsets = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            offsets.add(ScenarioArea.offsetOf(i));
        }
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < offsets.size(); i++) {
            assertTrue(seen.add(offsets.get(i)[0] + "," + offsets.get(i)[1]), "slot " + i + " repeats a place");
            for (int j = i + 1; j < offsets.size(); j++) {
                int dx = Math.abs(offsets.get(i)[0] - offsets.get(j)[0]);
                int dz = Math.abs(offsets.get(i)[1] - offsets.get(j)[1]);
                assertTrue(Math.max(dx, dz) >= 64, "slots " + i + " and " + j + " are too close");
            }
        }
    }

    @Test
    void defaultAreasOfNeighboursDoNotOverlap() {
        assertTrue(ScenarioArea.STRIDE > 2 * ScenarioArea.RADIUS + 1);
    }

    @Test
    void radiusIsClamped() {
        assertEquals(1, ScenarioArea.clampRadius(-5));
        assertEquals(20, ScenarioArea.clampRadius(20));
        assertEquals(ScenarioArea.MAX_RADIUS, ScenarioArea.clampRadius(10_000));
    }

    @Test
    void chunkSpanFloorsNegativeCoordinates() {
        assertArrayEquals(new int[] { -2, 1 }, ScenarioArea.chunkSpan(0, 17));
        assertArrayEquals(new int[] { -1, 0 }, ScenarioArea.chunkSpan(-1, 1));
        assertArrayEquals(new int[] { 0, 0 }, ScenarioArea.chunkSpan(8, 7));
        assertArrayEquals(new int[] { 0, 1 }, ScenarioArea.chunkSpan(8, 8));
    }

    @Test
    void chunksCoveringListsTheWholeSquareOnce() {
        List<int[]> chunks = ScenarioArea.chunksCovering(100, -40, 32);
        // x 68..132 -> chunks 4..8; z -72..-8 -> chunks -5..-1
        assertEquals(25, chunks.size());
        Set<String> unique = new HashSet<>();
        for (int[] c : chunks) {
            assertTrue(c[0] >= 4 && c[0] <= 8 && c[1] >= -5 && c[1] <= -1);
            unique.add(c[0] + "," + c[1]);
        }
        assertEquals(25, unique.size());
    }

    @Test
    void aChunkAlignedOriginNeedsFewerChunks() {
        // x -32..32 around 0 -> chunks -2..2 (block 32 is in chunk 2): 5 per axis even when aligned
        assertEquals(
            25,
            ScenarioArea.chunksCovering(0, 0, 32)
                .size());
        assertEquals(
            1,
            ScenarioArea.chunksCovering(8, 8, 7)
                .size());
    }
}
