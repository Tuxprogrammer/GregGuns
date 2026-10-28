package com.gregguns.icbm.complex.palette;

/**
 * A floor or ceiling design: which block sits at a room-local cell. Pure and stateless, so a pattern is a value.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/palette.md (Patterns); D240
public interface CellPattern {

    /**
     * @param x     room-local x, 0-based from the room box's minimum corner
     * @param z     room-local z
     * @param sizeX the room's x extent in blocks
     * @param sizeZ the room's z extent in blocks
     * @return the block there
     */
    BlockRef at(int x, int z, int sizeX, int sizeZ);
}
