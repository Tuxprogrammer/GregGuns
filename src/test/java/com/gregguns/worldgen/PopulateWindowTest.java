package com.gregguns.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** GGS-11-08: the window is [cx*16+8, cx*16+23] on each axis. */
class PopulateWindowTest {

    @Test
    void originChunk() {
        PopulateWindow w = new PopulateWindow(0, 0);
        assertEquals(8, w.minX());
        assertEquals(23, w.maxX());
        assertEquals(8, w.minZ());
        assertEquals(23, w.maxZ());
    }

    @Test
    void negativeChunkCoordinates() {
        PopulateWindow w = new PopulateWindow(-1, -3);
        assertEquals(-8, w.minX());
        assertEquals(7, w.maxX());
        assertEquals(-40, w.minZ());
        assertEquals(-25, w.maxZ());
        assertTrue(w.contains(-8, -40));
        assertTrue(w.contains(7, -25));
        assertFalse(w.contains(-9, -30));
        assertFalse(w.contains(8, -30));
    }

    @Test
    void edgesAreInclusive() {
        PopulateWindow w = new PopulateWindow(2, 5);
        assertTrue(w.contains(40, 88));
        assertTrue(w.contains(55, 103));
        assertFalse(w.contains(39, 88));
        assertFalse(w.contains(56, 88));
        assertFalse(w.contains(40, 87));
        assertFalse(w.contains(40, 104));
    }

    @Test
    void windowSpansExactlyTheFourChunksVanillaGuarantees() {
        for (int cx = -3; cx <= 3; cx++) {
            for (int cz = -3; cz <= 3; cz++) {
                PopulateWindow w = new PopulateWindow(cx, cz);
                assertEquals(cx, PopulateWindow.chunkOf(w.minX()));
                assertEquals(cx + 1, PopulateWindow.chunkOf(w.maxX()));
                assertEquals(cz, PopulateWindow.chunkOf(w.minZ()));
                assertEquals(cz + 1, PopulateWindow.chunkOf(w.maxZ()));
                assertTrue(w.overlapsChunk(cx + 1, cz + 1));
                assertFalse(w.overlapsChunk(cx - 1, cz));
                assertFalse(w.overlapsChunk(cx, cz + 2));
            }
        }
    }

    @Test
    void chunkOfFloorsNegativeBlocks() {
        assertEquals(-1, PopulateWindow.chunkOf(-1));
        assertEquals(-1, PopulateWindow.chunkOf(-16));
        assertEquals(-2, PopulateWindow.chunkOf(-17));
        assertEquals(0, PopulateWindow.chunkOf(15));
        assertEquals(1, PopulateWindow.chunkOf(16));
    }

    @Test
    void neighborWindowsTileWithoutGapOrOverlap() {
        PopulateWindow a = new PopulateWindow(-1, 0);
        PopulateWindow b = new PopulateWindow(0, 0);
        assertEquals(a.maxX() + 1, b.minX());
        assertFalse(a.contains(b.minX(), b.minZ()));
    }

    @Test
    void overlapsBox() {
        PopulateWindow w = new PopulateWindow(0, 0);
        assertTrue(w.overlapsBox(0, 0, 8, 8));
        assertTrue(w.overlapsBox(23, 23, 40, 40));
        assertTrue(w.overlapsBox(10, 10, 12, 12));
        assertFalse(w.overlapsBox(0, 0, 7, 30));
        assertFalse(w.overlapsBox(24, 0, 40, 30));
        assertFalse(w.overlapsBox(0, 24, 30, 40));
    }

    @Test
    void equalsAndHash() {
        assertEquals(new PopulateWindow(4, -2), new PopulateWindow(4, -2));
        assertEquals(new PopulateWindow(4, -2).hashCode(), new PopulateWindow(4, -2).hashCode());
        assertFalse(new PopulateWindow(4, -2).equals(new PopulateWindow(-2, 4)));
    }
}
