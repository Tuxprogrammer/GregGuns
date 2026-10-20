package com.gregguns.worldgen.layout;

/**
 * Coordinate tables for stamping a layout at rotation 0..3 (quarter turns clockwise seen from above, +x east and +z
 * south). The rotated footprint is a box {@code [0, w) x [0, d)} whose minimum corner sits on the plan anchor, so the
 * anchor is always the corner with the lowest x and z whatever the rotation; this is what the terrain check and the
 * populate-window test measure (WGEN-05, 15).
 *
 * <p>
 * Pure integer math, no Minecraft types.
 */
public final class Rotations {

    private Rotations() {}

    /** Footprint width along x after rotation. */
    public static int width(int rotation, int sx, int sz) {
        return (rotation & 1) == 0 ? sx : sz;
    }

    /** Footprint depth along z after rotation. */
    public static int depth(int rotation, int sx, int sz) {
        return (rotation & 1) == 0 ? sz : sx;
    }

    /** Offset along x from the anchor of layout cell column {@code (lx, lz)}. */
    public static int offsetX(int rotation, int lx, int lz, int sx, int sz) {
        switch (rotation & 3) {
            case 0:
                return lx;
            case 1:
                return sz - 1 - lz;
            case 2:
                return sx - 1 - lx;
            default:
                return lz;
        }
    }

    /** Offset along z from the anchor of layout cell column {@code (lx, lz)}. */
    public static int offsetZ(int rotation, int lx, int lz, int sx, int sz) {
        switch (rotation & 3) {
            case 0:
                return lz;
            case 1:
                return lx;
            case 2:
                return sz - 1 - lz;
            default:
                return sx - 1 - lx;
        }
    }

    /** Inverse of {@link #offsetX}: layout x of the footprint column at offset {@code (dx, dz)}. */
    public static int layoutX(int rotation, int dx, int dz, int sx, int sz) {
        switch (rotation & 3) {
            case 0:
                return dx;
            case 1:
                return dz;
            case 2:
                return sx - 1 - dx;
            default:
                return sx - 1 - dz;
        }
    }

    /** Inverse of {@link #offsetZ}: layout z of the footprint column at offset {@code (dx, dz)}. */
    public static int layoutZ(int rotation, int dx, int dz, int sx, int sz) {
        switch (rotation & 3) {
            case 0:
                return dz;
            case 1:
                return sz - 1 - dx;
            case 2:
                return sz - 1 - dz;
            default:
                return dx;
        }
    }
}
