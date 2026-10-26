package com.gregguns.icbm.blast;

/**
 * Packs a block offset from the blast center into one {@code int} (10 bits per axis, offsets -512 to 511), so a
 * 100,000-block crater saves as 400 KB. No Minecraft types, both sides.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/blast-engine.md (save and load)
public final class BlastPacking {

    private static final int BIAS = 512;
    private static final int MASK = 1023;

    private BlastPacking() {}

    /**
     * @param ox X offset, -512 to 511
     * @param oy Y offset
     * @param oz Z offset
     * @return the packed offset
     */
    public static int pack(int ox, int oy, int oz) {
        return ((ox + BIAS) & MASK) << 20 | ((oy + BIAS) & MASK) << 10 | ((oz + BIAS) & MASK);
    }

    /**
     * @param packed a packed offset
     * @return the X offset
     */
    public static int x(int packed) {
        return ((packed >>> 20) & MASK) - BIAS;
    }

    /**
     * @param packed a packed offset
     * @return the Y offset
     */
    public static int y(int packed) {
        return ((packed >>> 10) & MASK) - BIAS;
    }

    /**
     * @param packed a packed offset
     * @return the Z offset
     */
    public static int z(int packed) {
        return (packed & MASK) - BIAS;
    }
}
