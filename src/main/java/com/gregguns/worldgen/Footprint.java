package com.gregguns.worldgen;

/**
 * The built size of a structure, in blocks, as the layout file declares it. The layout file is the only source: a
 * hand-typed register of sizes put the rotation center and terrain check off by 1 to 2 blocks (WGEN-30).
 */
// Spec: docs/techguns-spec/11-worldgen/index.md (WGEN-30)
public final class Footprint {

    private final int sizeX;
    private final int sizeY;
    private final int sizeZ;

    public Footprint(int sizeX, int sizeY, int sizeZ) {
        if (sizeX < 1 || sizeY < 1 || sizeZ < 1) {
            throw new IllegalArgumentException(
                "Footprint must be at least 1x1x1: " + sizeX + "x" + sizeY + "x" + sizeZ);
        }
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
    }

    public int sizeX() {
        return sizeX;
    }

    public int sizeY() {
        return sizeY;
    }

    public int sizeZ() {
        return sizeZ;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Footprint)) return false;
        Footprint f = (Footprint) o;
        return sizeX == f.sizeX && sizeY == f.sizeY && sizeZ == f.sizeZ;
    }

    @Override
    public int hashCode() {
        return (sizeX * 31 + sizeY) * 31 + sizeZ;
    }

    @Override
    public String toString() {
        return sizeX + "x" + sizeY + "x" + sizeZ;
    }
}
