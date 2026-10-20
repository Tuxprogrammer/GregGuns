package com.gregguns.worldgen.layout;

import net.minecraft.block.Block;

/** A palette key resolved to a block and its default meta. */
public final class ResolvedBlock {

    public final Block block;
    public final int meta;
    /** True when the vanilla fallback stands in for a Chisel block (Chisel absent or its field null). */
    public final boolean fallback;

    public ResolvedBlock(Block block, int meta, boolean fallback) {
        this.block = block;
        this.meta = meta;
        this.fallback = fallback;
    }
}
