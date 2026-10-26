package com.gregguns.icbm.warhead;

/**
 * Holds the registered warhead blocks. Set once by {@link WarheadRegistration}; null before it runs. Both sides.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/warhead-blocks.md
public final class WarheadBlocks {

    private static BlockWarhead t1;
    private static BlockWarhead t2;
    private static BlockWarheadFiller filler;

    private WarheadBlocks() {}

    static void set(BlockWarhead one, BlockWarhead two, BlockWarheadFiller fill) {
        t1 = one;
        t2 = two;
        filler = fill;
    }

    /** @return the block of a casing; null before registration */
    public static BlockWarhead of(WarheadCasing casing) {
        return casing == WarheadCasing.T1 ? t1 : t2;
    }

    /** @return the invisible filler block; null before registration */
    public static BlockWarheadFiller filler() {
        return filler;
    }
}
