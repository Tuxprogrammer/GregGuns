package com.gregguns.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

/** WGEN-05 / WGEN-06: the checked box and the built cells are the same rotated footprint. */
class FootprintTest {

    @Test
    void oddTurnsSwapSizes() {
        Footprint f0 = new Footprint(0, 0, 54, 21, 0);
        Footprint f1 = new Footprint(0, 0, 54, 21, 1);
        assertEquals(54, f0.sizeX());
        assertEquals(21, f0.sizeZ());
        assertEquals(21, f1.sizeX());
        assertEquals(54, f1.sizeZ());
        assertEquals(21, new Footprint(0, 0, 54, 21, 3).sizeX());
        assertEquals(54, new Footprint(0, 0, 54, 21, 2).sizeX());
        assertEquals(54, new Footprint(0, 0, 54, 21, 4).sizeX());
    }

    @Test
    void everyLocalCellLandsInsideTheBoxExactlyOnce() {
        for (int dir = 0; dir < 4; dir++) {
            Footprint f = new Footprint(-37, 12, 7, 33, dir);
            Set<Long> seen = new HashSet<>();
            for (int lx = 0; lx < 7; lx++) {
                for (int lz = 0; lz < 33; lz++) {
                    int x = f.worldX(lx, lz);
                    int z = f.worldZ(lx, lz);
                    assertTrue(f.contains(x, z), "dir " + dir + " cell " + lx + "," + lz);
                    assertTrue(seen.add(((long) x << 32) ^ (z & 0xffffffffL)), "collision at dir " + dir);
                }
            }
            assertEquals(7 * 33, seen.size());
            assertEquals(7 * 33, f.sizeX() * f.sizeZ());
            assertEquals(-37, f.minX());
            assertEquals(12, f.minZ());
        }
    }

    @Test
    void overlapUsesTheRotatedBox() {
        // 20 x 10 at (8, 8): unrotated it spans z 8..17, quarter-turned it spans z 8..27.
        PopulateWindow w = new PopulateWindow(0, 1); // x 8..23, z 24..39
        assertTrue(!new Footprint(8, 8, 20, 10, 0).overlaps(w));
        assertTrue(new Footprint(8, 8, 20, 10, 1).overlaps(w));
    }
}
