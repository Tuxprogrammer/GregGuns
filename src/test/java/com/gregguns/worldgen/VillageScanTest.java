package com.gregguns.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** GGS-11-14: a footprint holding village-style blocks is skipped. Window of chunk (0,0): x, z 8..23. */
class VillageScanTest {

    private final PopulateWindow w = new PopulateWindow(0, 0);
    private final PlacedFootprint fp = new PlacedFootprint(10, 10, 8, 8, 0);

    @Test
    void emptyTerrainHasNoVillage() {
        assertFalse(VillageScan.containsVillageBlocks(w, GridColumns.flat(64), fp, 64, 8));
    }

    @Test
    void plankAboveTheSurfaceIsFound() {
        GridColumns c = GridColumns.flat(64)
            .put(12, 66, 14, BlockKind.VILLAGE);
        assertTrue(VillageScan.containsVillageBlocks(w, c, fp, 64, 8));
    }

    @Test
    void blocksOutsideTheHeightBandOrFootprintAreIgnored() {
        GridColumns c = GridColumns.flat(64)
            .put(12, 80, 14, BlockKind.VILLAGE) // above the scanned height
            .put(30, 66, 14, BlockKind.VILLAGE) // outside the footprint (and the window)
            .put(12, 64, 14, BlockKind.VILLAGE); // the surface block itself is not scanned
        assertFalse(VillageScan.containsVillageBlocks(w, c, fp, 64, 8));
    }

    /** The scan reads one column in four and never a column outside the window. */
    @Test
    void sampledColumnsAreAQuarterAndInsideTheWindow() {
        GridColumns c = GridColumns.flat(64);
        PlacedFootprint big = new PlacedFootprint(0, 0, 40, 40, 0);
        VillageScan.containsVillageBlocks(w, c, big, 64, 1);
        assertEquals(0, c.readsOutside(w));
        // 16 x 16 window columns, every second x and z: 64 columns, one cell each.
        assertEquals(64, c.reads.size());
    }
}
