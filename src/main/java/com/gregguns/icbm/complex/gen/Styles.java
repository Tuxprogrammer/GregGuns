package com.gregguns.icbm.complex.gen;

import com.gregguns.icbm.complex.ComplexRaster;
import com.gregguns.icbm.worldgen.ActivityLevel;
import com.gregguns.worldgen.layout.ResolvedBlock;

/**
 * The look of each section of one complex: resolves a building role (wall, floor, ceiling, lamp, door frame,
 * furniture) at a complex-local cell to a block. The production source is {@link PaletteStyles} (the palette, D240);
 * unit tests pass {@link #NONE}, which leaves every role to the {@link BlockResolver}.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Palette); palette.md
public interface Styles {

    /** No styles: every role falls through to the block resolver. */
    Styles NONE = new Styles() {

        @Override
        public ResolvedBlock block(byte role, int x, int y, int z) {
            return null;
        }

        @Override
        public ResolvedBlock litLamp(int x, int y, int z) {
            return null;
        }
    };

    /**
     * @param role a {@link com.gregguns.icbm.complex.Cell} building role
     * @return the block of the role in the section that owns the column, or null to let the resolver decide
     */
    ResolvedBlock block(byte role, int x, int y, int z);

    /** @return the lit lamp of the section that owns the column, or null for the resolver's */
    ResolvedBlock litLamp(int x, int y, int z);

    /**
     * Names the code path that decides a building role's block at a cell, for the role audit's log line (D267).
     *
     * @param role a {@link com.gregguns.icbm.complex.Cell} building role
     * @return a one-line description; the block resolver's when no style decides the cell
     */
    default String path(byte role, int x, int y, int z) {
        return "the block resolver (no palette style)";
    }

    /** Makes the styles of one complex. */
    interface Factory {

        Factory NONE = (raster, dimensionKey, level) -> Styles.NONE;

        Styles of(ComplexRaster raster, String dimensionKey, ActivityLevel level);
    }
}
