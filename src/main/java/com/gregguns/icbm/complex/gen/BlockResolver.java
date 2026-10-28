package com.gregguns.icbm.complex.gen;

import com.gregguns.worldgen.layout.ResolvedBlock;

/** Maps a raster {@link com.gregguns.icbm.complex.Cell} to the block written there; null skips the cell. */
public interface BlockResolver {

    ResolvedBlock resolve(byte cell, int x, int y, int z);

    /**
     * @return a surface or shaft-lining block for the above-level parts: "ring", "cap", "ladder", "trapdoor", "rail"
     */
    ResolvedBlock named(String what, int x, int y, int z);

    /**
     * @param meta ladder metadata 2 to 5 (the side it clings to)
     * @return a ladder block clinging to the wall behind the escape column
     */
    default ResolvedBlock ladder(int meta, int x, int y, int z) {
        return named("ladder", x, y, z);
    }

    /** @return the air block */
    net.minecraft.block.Block air();
}
