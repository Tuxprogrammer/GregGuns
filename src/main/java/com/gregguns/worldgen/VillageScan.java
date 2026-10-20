package com.gregguns.worldgen;

/**
 * GGS-11-14: skip a footprint that already holds village-style blocks (planks, cobblestone, glass panes, doors).
 * The chunk provider cannot say where a village is (RWG answers only for strongholds), so the blocks are the
 * evidence. Called before anything is placed, and windows never share a column, so every block found is foreign.
 *
 * <p>
 * Samples one column in four (every second x of every second z, PLAN-M8 Q5) and the cells from just above the
 * surface up to {@code height}. Reads only columns inside the window.
 */
public final class VillageScan {

    private VillageScan() {}

    public static boolean containsVillageBlocks(PopulateWindow window, ColumnReader reader, PlacedFootprint fp,
        int surfaceY, int height) {
        for (int x = fp.minX(); x <= fp.maxX(); x += 2) {
            for (int z = fp.minZ(); z <= fp.maxZ(); z += 2) {
                if (!window.contains(x, z)) {
                    continue;
                }
                for (int y = surfaceY + 1; y <= surfaceY + height; y++) {
                    if (reader.kindAt(x, y, z) == BlockKind.VILLAGE) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
