package com.gregguns.icbm.complex.palette;

import java.util.List;

/**
 * A floor or ceiling design: which block sits at a room-local cell. Pure and stateless, so a pattern is a value. It
 * also lists every block chain it can place, so the role check (D265) can count every block a patterned floor or
 * ceiling uses without sampling cells.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/palette.md (Patterns); D240; D265
public interface CellPattern {

    /**
     * @param x     room-local x, 0-based from the room box's minimum corner
     * @param z     room-local z
     * @param sizeX the room's x extent in blocks
     * @param sizeZ the room's z extent in blocks
     * @return the block there
     */
    BlockRef at(int x, int z, int sizeX, int sizeZ);

    /** @return every chain {@link #at} can return, in any room size; never empty */
    List<BlockRef> refs();
}
