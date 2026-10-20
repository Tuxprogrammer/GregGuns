package com.gregguns.worldgen;

/**
 * Ground and water heights, readable only inside one populate window (GGS-11-08, GGS-11-09). Every method returns
 * {@link #NONE} for a column outside the window instead of reading it, because a read there can force a chunk to
 * generate (the WGEN-04 cascade).
 */
public interface HeightProbe {

    /** Returned for "no answer": outside the window, no ground, or the wrong surface type. */
    int NONE = -1;

    PopulateWindow window();

    /**
     * Y of the top natural ground block, looking down through air, foliage, ice and wood. Trunks are skipped, so a
     * forest reads as its dirt, not its trees (WGEN-33). {@link #NONE} when the first thing met is a liquid (a
     * water surface is not land) or the column is outside the window.
     */
    int landY(int x, int z);

    /** Y of the top liquid block when the column's surface is a liquid; {@link #NONE} otherwise. */
    int waterSurfaceY(int x, int z);

    /** Y of the first non-liquid block below the water (or the ground itself when dry); {@link #NONE} if none. */
    int seaFloorY(int x, int z);
}
