package com.gregguns.worldgen;

/**
 * Read-only view of world columns. Callers must go through a {@link WindowedHeightProbe} (or check the window
 * themselves); a reader itself does not guard, so a raw reader must never be handed to structure code.
 */
public interface ColumnReader {

    /** What the block at the position is. Air above the world and below 0 is {@link BlockKind#AIR}. */
    BlockKind kindAt(int x, int y, int z);

    /** An upper bound on the highest non-air block of the column; scans start here. */
    int topY(int x, int z);
}
