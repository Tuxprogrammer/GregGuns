package com.gregguns.icbm.warhead;

/**
 * Holds the registered warhead blocks. Set once by {@link WarheadRegistration}; null before it runs. Both sides.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/warhead-blocks.md
public final class WarheadBlocks {

    private static final BlockWarhead[] BLOCKS = new BlockWarhead[WarheadCasing.values().length];
    private static BlockWarheadFiller filler;

    private WarheadBlocks() {}

    static void set(WarheadCasing casing, BlockWarhead block) {
        BLOCKS[casing.ordinal()] = block;
    }

    static void setFiller(BlockWarheadFiller fill) {
        filler = fill;
    }

    /** @return the block of a casing; null before registration */
    public static BlockWarhead of(WarheadCasing casing) {
        return BLOCKS[casing.ordinal()];
    }

    /** @return the invisible filler block; null before registration */
    public static BlockWarheadFiller filler() {
        return filler;
    }
}
