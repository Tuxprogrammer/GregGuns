package com.gregguns.icbm.complex.blueprint;

/**
 * A walk-through opening in one of a room's walls, in the room's frame: the doorway to its parent, a child room's
 * door or a loop door (D278), or one of the hub's portals. {@code lo..hi} runs along the wall: lateral for the back and
 * door walls, depth for the side walls.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Furnishing); D278; D305
public final class Opening {

    /** The wall an opening is cut in. */
    public enum Wall {
        /** d 0's wall. */
        BACK,
        /** The door wall, past d {@code depth - 1}. */
        DOOR,
        /** The wall past a 0 (a -1). */
        LEFT,
        /** The wall past a {@code lateral - 1}. */
        RIGHT
    }

    public final Wall wall;
    public final int lo, hi;
    /** True for the room's own doorway to its parent, which the room is furnished around. */
    public final boolean primary;

    public Opening(Wall wall, int lo, int hi, boolean primary) {
        this.wall = wall;
        this.lo = Math.min(lo, hi);
        this.hi = Math.max(lo, hi);
        this.primary = primary;
    }

    /** @return the interior cells just inside the opening, as {a, d} pairs */
    int[][] inside(int lateral, int depth) {
        int n = hi - lo + 1;
        int[][] out = new int[n][];
        for (int i = 0; i < n; i++) {
            int t = lo + i;
            switch (wall) {
                case BACK:
                    out[i] = new int[] { t, 0 };
                    break;
                case DOOR:
                    out[i] = new int[] { t, depth - 1 };
                    break;
                case LEFT:
                    out[i] = new int[] { 0, t };
                    break;
                default:
                    out[i] = new int[] { lateral - 1, t };
                    break;
            }
        }
        return out;
    }

    @Override
    public String toString() {
        return wall + "[" + lo + ".." + hi + "]" + (primary ? "*" : "");
    }
}
