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

    /**
     * PLAN-M8 Q5: a plains footprint with grass, flowers, leaves, trunks and snow, and no village, is never skipped.
     * Foliage and wood are not village blocks, so no pattern of them may trip the scan.
     */
    @Test
    void plainsWithoutVillagesIsNotSkipped() {
        GridColumns c = GridColumns.flat(64);
        for (int x = 10; x <= 17; x++) {
            for (int z = 10; z <= 17; z++) {
                c.put(x, 65, z, (x + z) % 3 == 0 ? BlockKind.FOLIAGE : BlockKind.AIR);
            }
        }
        // A tree: a trunk and a leaf crown over the footprint.
        for (int y = 65; y <= 69; y++) {
            c.put(12, y, 12, BlockKind.WOOD);
        }
        for (int x = 10; x <= 14; x++) {
            for (int z = 10; z <= 14; z++) {
                c.put(x, 70, z, BlockKind.FOLIAGE);
            }
        }
        assertFalse(VillageScan.containsVillageBlocks(w, c, fp, 64, 8));
    }

    /** The one-in-four sampling still catches a house: no 2x2 patch of footprint columns is entirely unsampled. */
    @Test
    void aHouseWallIsFoundWhereverItStands() {
        for (int ox = 0; ox < 2; ox++) {
            for (int oz = 0; oz < 2; oz++) {
                GridColumns c = GridColumns.flat(64)
                    .put(12 + ox, 66, 12 + oz, BlockKind.VILLAGE)
                    .put(13 + ox, 66, 12 + oz, BlockKind.VILLAGE)
                    .put(12 + ox, 66, 13 + oz, BlockKind.VILLAGE)
                    .put(13 + ox, 66, 13 + oz, BlockKind.VILLAGE);
                assertTrue(
                    VillageScan.containsVillageBlocks(w, c, fp, 64, 8),
                    "2x2 patch at offset " + ox + "," + oz + " was missed");
            }
        }
    }
}
