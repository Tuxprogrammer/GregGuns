package com.gregguns.worldgen.camp;

/** A road strip in camp coordinates. Root roads run edge to edge and a little past the fence. Immutable. */
// Spec: camp_parts.md (Roads)
public final class CampRoad {

    public final int x;
    public final int z;
    public final int w;
    public final int d;
    /** Subdivision depth that made it; 0 is a root-level road. */
    public final int level;

    CampRoad(int x, int z, int w, int d, int level) {
        this.x = x;
        this.z = z;
        this.w = w;
        this.d = d;
        this.level = level;
    }

    public boolean isRoot() {
        return level == 0;
    }

    /** The road's width: its shorter side. */
    public int width() {
        return Math.min(w, d);
    }

    public boolean contains(int lx, int lz) {
        return lx >= x && lx < x + w && lz >= z && lz < z + d;
    }

    @Override
    public String toString() {
        return "road[" + x + "," + z + " " + w + "x" + d + " L" + level + "]";
    }
}
