package com.gregguns.icbm.complex;

/** An axis-aligned block box, inclusive of min, exclusive of max, in complex-local coordinates. */
public final class Box {

    public final int minX, minY, minZ, maxX, maxY, maxZ;

    public Box(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        if (maxX <= minX || maxY <= minY || maxZ <= minZ) throw new IllegalArgumentException("empty box");
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

    public boolean contains(int x, int y, int z) {
        return x >= minX && x < maxX && y >= minY && y < maxY && z >= minZ && z < maxZ;
    }

    /** Overlap in the horizontal plane only (the complex is one level). */
    public boolean overlapsXZ(Box o) {
        return minX < o.maxX && o.minX < maxX && minZ < o.maxZ && o.minZ < maxZ;
    }

    public int sizeX() {
        return maxX - minX;
    }

    public int sizeY() {
        return maxY - minY;
    }

    public int sizeZ() {
        return maxZ - minZ;
    }

    public int centerX() {
        return (minX + maxX) / 2;
    }

    public int centerZ() {
        return (minZ + maxZ) / 2;
    }

    @Override
    public String toString() {
        return "[" + minX + "," + minY + "," + minZ + " .. " + maxX + "," + maxY + "," + maxZ + ")";
    }
}
