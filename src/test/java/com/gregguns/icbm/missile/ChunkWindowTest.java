package com.gregguns.icbm.missile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ChunkWindowTest {

    @Test
    void neverMoreThanTwoChunksWhateverTheHeading() {
        for (int deg = 0; deg < 360; deg += 7) {
            double vx = Math.cos(Math.toRadians(deg));
            double vz = Math.sin(Math.toRadians(deg));
            for (double x = -50.3; x < 50; x += 7.7) {
                int[][] w = ChunkWindow.wanted(x, x * 1.3 + 3.1, vx, vz);
                assertTrue(w.length >= 1 && w.length <= ChunkWindow.MAX_CHUNKS);
            }
        }
    }

    @Test
    void theFirstChunkIsTheMissilesOwnAndTheSecondIsAheadInTheNegativeDirectionToo() {
        // ICBM-03-21: toward lower coordinates the baseline never saw the chunk ahead.
        int[][] east = ChunkWindow.wanted(8, 8, 1, 0);
        assertEquals(0, east[0][0]);
        assertEquals(1, east[1][0]);
        int[][] west = ChunkWindow.wanted(8, 8, -1, 0);
        assertEquals(0, west[0][0]);
        assertEquals(-1, west[1][0]);
        int[][] north = ChunkWindow.wanted(8, 8, 0, -1);
        assertEquals(-1, north[1][1]);
    }

    @Test
    void negativeCoordinatesPickTheRightChunk() {
        // ICBM-03-35: truncating before shifting put -0.5 in chunk 0.
        assertEquals(-1, ChunkWindow.floorChunk(-0.5));
        assertEquals(-1, ChunkWindow.floorChunk(-16.0));
        assertEquals(-2, ChunkWindow.floorChunk(-16.5));
        assertEquals(0, ChunkWindow.floorChunk(15.99));
        assertEquals(1, ChunkWindow.floorChunk(16.0));
    }

    @Test
    void aStandingMissileWantsOnlyItsOwnChunk() {
        assertEquals(1, ChunkWindow.wanted(40, 40, 0, 0).length);
    }

    @Test
    void chunksAreComparedByValue() {
        // ICBM-03-32: two fresh pairs for the same chunk are the same chunk.
        assertTrue(ChunkWindow.same(new int[] { 3, -4 }, new int[] { 3, -4 }));
        assertTrue(!ChunkWindow.same(new int[] { 3, -4 }, new int[] { 3, 4 }));
    }

    @Test
    void theWindowSlidesAcrossABoundaryDroppingTheOneBehind() {
        int[][] before = ChunkWindow.wanted(15.5, 8, 1, 0);
        int[][] after = ChunkWindow.wanted(16.5, 8, 1, 0);
        assertEquals(0, before[0][0]);
        assertEquals(1, after[0][0]);
        assertEquals(2, after[1][0]);
        for (int[] c : after) {
            assertTrue(c[0] != 0, "chunk 0 is behind and released");
        }
    }
}
