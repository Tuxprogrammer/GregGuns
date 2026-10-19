package com.gregguns.utility.blob;

/**
 * The Bio Blob's rules as plain arithmetic: how big it is, when it bursts, how it decays and what box it fills. No
 * Minecraft types, so the tests need no world. Both sides read it (the client for the bounds); only the server changes
 * a blob's size (MUTL-44).
 */
// Spec: docs/techguns-spec/09-machines-utility/misc-tile-entities.md section 3 (Bio Blob); fixes MUTL-42, MUTL-44
public final class BlobState {

    /** Smallest size of a living blob. */
    public static final int MIN_SIZE = 1;
    /** Largest size; a hit that would take a blob past it makes it burst. */
    public static final int MAX_SIZE = 3;
    /** Sentinel for {@link #afterHit} and {@link #afterDecay}: the blob is gone. */
    public static final int GONE = 0;
    /** Sentinel for {@link #afterHit}: the blob bursts. */
    public static final int BURST = -1;

    private BlobState() {}

    /**
     * @param size  the current size, {@link #MIN_SIZE} to {@link #MAX_SIZE}
     * @param power the hit's power, at least 1
     * @return the new size, or {@link #BURST} when it would exceed {@link #MAX_SIZE}
     */
    public static int afterHit(int size, int power) {
        int next = size + Math.max(power, 0);
        return next > MAX_SIZE ? BURST : next;
    }

    /**
     * One decay step, taken when the decay timer runs out.
     *
     * @param size the current size
     * @return the size after the step, or {@link #GONE} when a size-1 blob evaporates
     */
    public static int afterDecay(int size) {
        return size > MIN_SIZE ? size - 1 : GONE;
    }

    /**
     * @param size the size, clamped into range
     * @return the box width in blocks: (10 + 6 s) / 32
     */
    public static double width(int size) {
        return (10 + 6 * clamp(size)) / 32.0;
    }

    /**
     * @param size the size, clamped into range
     * @return the box thickness in blocks: (2 + 4 s) / 32
     */
    public static double thickness(int size) {
        return (2 + 4 * clamp(size)) / 32.0;
    }

    /**
     * The box of a blob, flush against the block it sticks to. Used for both selection and collision (MUTL-42).
     *
     * @param size the size
     * @param side the metadata: the face of the supported block (0 bottom, 1 top, 2 north, 3 south, 4 west, 5 east)
     * @return {minX, minY, minZ, maxX, maxY, maxZ} in block-local coordinates
     */
    public static double[] bounds(int size, int side) {
        double w = width(size);
        double h = thickness(size);
        double lo = 0.5 - w / 2;
        double hi = 0.5 + w / 2;
        switch (side) {
            case 0:
                return new double[] { lo, 1 - h, lo, hi, 1, hi };
            case 2:
                return new double[] { lo, lo, 1 - h, hi, hi, 1 };
            case 3:
                return new double[] { lo, lo, 0, hi, hi, h };
            case 4:
                return new double[] { 1 - h, lo, lo, 1, hi, hi };
            case 5:
                return new double[] { 0, lo, lo, h, hi, hi };
            default:
                return new double[] { lo, 0, lo, hi, h, hi };
        }
    }

    private static int clamp(int size) {
        return Math.max(MIN_SIZE, Math.min(MAX_SIZE, size));
    }
}
