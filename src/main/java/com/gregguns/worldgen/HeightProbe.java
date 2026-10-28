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

    /**
     * {@link #seaFloorY} that also answers for a column one block outside the window. The window lies 8 blocks inside
     * the four chunks the populate call has loaded ({@link PopulateWindow}), so a column one block past any edge is in
     * one of them and reading it never forces a chunk. {@link #NONE} further out. A probe that cannot see past its
     * window answers only inside it.
     */
    default int seaFloorYBeside(int x, int z) {
        return seaFloorY(x, z);
    }
}
