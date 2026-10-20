package com.gregguns.worldgen;

/** A camp flag's world position, recorded so later windows and the camp placer agree on it. Immutable. */
// Spec: docs/gregguns-spec/11-worldgen/structure-placement.md (GGS-11-10); fixes WGEN-10 (flag offset after rotation)
public final class FlagPos {

    public final int x;
    public final int y;
    public final int z;

    public FlagPos(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FlagPos)) {
            return false;
        }
        FlagPos p = (FlagPos) o;
        return x == p.x && y == p.y && z == p.z;
    }

    @Override
    public int hashCode() {
        return (x * 31 + y) * 31 + z;
    }

    @Override
    public String toString() {
        return "(" + x + "," + y + "," + z + ")";
    }
}
