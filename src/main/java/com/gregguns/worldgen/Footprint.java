package com.gregguns.worldgen;

/**
 * The world-space cells a rotated structure covers. One class maps local cells to world columns and also gives the
 * bounding box, so the terrain check and the stamper cannot disagree about where the structure stands (WGEN-05,
 * WGEN-06: Techguns checked one box and built another).
 *
 * <p>
 * Rotation follows the documented Techguns convention: each quarter turn about the local center
 * {@code (sizeX/2, sizeZ/2)} maps offset {@code (a, b)} to {@code (b, -a)}. The planned anchor is the world corner of
 * the <em>rotated</em> box, so no second rotation shift exists.
 */
public final class Footprint {

    private final int boxMinX;
    private final int boxMinZ;
    private final int sizeX;
    private final int sizeZ;
    private final int direction;
    private final int centerX;
    private final int centerZ;
    private final int rotMinX;
    private final int rotMinZ;
    private final int rotSizeX;
    private final int rotSizeZ;

    /**
     * @param boxMinX   world X of the rotated box's minimum corner
     * @param boxMinZ   world Z of the rotated box's minimum corner
     * @param sizeX     unrotated size along local X (cells)
     * @param sizeZ     unrotated size along local Z (cells)
     * @param direction quarter turns, taken modulo 4
     */
    public Footprint(int boxMinX, int boxMinZ, int sizeX, int sizeZ, int direction) {
        if (sizeX < 1 || sizeZ < 1) {
            throw new IllegalArgumentException("footprint size must be positive");
        }
        this.boxMinX = boxMinX;
        this.boxMinZ = boxMinZ;
        this.sizeX = sizeX;
        this.sizeZ = sizeZ;
        this.direction = ((direction % 4) + 4) % 4;
        this.centerX = sizeX / 2;
        this.centerZ = sizeZ / 2;
        // Rotated bounding box from the four corner cells (cells 0..size-1, so the box has exactly size cells).
        int[][] corners = { { 0, 0 }, { sizeX - 1, 0 }, { 0, sizeZ - 1 }, { sizeX - 1, sizeZ - 1 } };
        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (int[] c : corners) {
            int rx = rotate(c[0], c[1], true);
            int rz = rotate(c[0], c[1], false);
            minX = Math.min(minX, rx);
            maxX = Math.max(maxX, rx);
            minZ = Math.min(minZ, rz);
            maxZ = Math.max(maxZ, rz);
        }
        this.rotMinX = minX;
        this.rotMinZ = minZ;
        this.rotSizeX = maxX - minX + 1;
        this.rotSizeZ = maxZ - minZ + 1;
    }

    private int rotate(int lx, int lz, boolean wantX) {
        int a = lx - centerX;
        int b = lz - centerZ;
        for (int i = 0; i < direction; i++) {
            int t = a;
            a = b;
            b = -t;
        }
        return wantX ? a + centerX : b + centerZ;
    }

    /** World X of local cell {@code (lx, lz)} after rotation. */
    public int worldX(int lx, int lz) {
        return boxMinX + rotate(lx, lz, true) - rotMinX;
    }

    /** World Z of local cell {@code (lx, lz)} after rotation. */
    public int worldZ(int lx, int lz) {
        return boxMinZ + rotate(lx, lz, false) - rotMinZ;
    }

    public int direction() {
        return direction;
    }

    public int minX() {
        return boxMinX;
    }

    public int minZ() {
        return boxMinZ;
    }

    public int maxX() {
        return boxMinX + rotSizeX - 1;
    }

    public int maxZ() {
        return boxMinZ + rotSizeZ - 1;
    }

    /** Rotated size along world X (the unrotated Z size after an odd number of turns). */
    public int sizeX() {
        return rotSizeX;
    }

    public int sizeZ() {
        return rotSizeZ;
    }

    public boolean contains(int x, int z) {
        return x >= minX() && x <= maxX() && z >= minZ() && z <= maxZ();
    }

    /** True when any footprint column is inside the window, i.e. this populate owns part of the structure. */
    public boolean overlaps(PopulateWindow window) {
        return window.overlapsBox(minX(), minZ(), maxX(), maxZ());
    }

    /** Unrotated size along local X. */
    public int localSizeX() {
        return sizeX;
    }

    /** Unrotated size along local Z. */
    public int localSizeZ() {
        return sizeZ;
    }
}
