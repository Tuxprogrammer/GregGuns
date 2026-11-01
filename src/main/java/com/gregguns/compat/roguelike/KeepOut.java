package com.gregguns.compat.roguelike;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The silo complex boxes a Roguelike dungeon keeps out of (D303), and the rectangle tests the keep-out editor runs on
 * every write. Pure: boxes are {@code {minX, minZ, maxX, maxZ}}, inclusive, at every Y.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/planets-and-placement.md (Roguelike Dungeons keep out); D303
public final class KeepOut {

    /** Blocks around a box that dungeon writes keep clear of, at every Y. */
    public static final int WRITE_MARGIN = 8;
    /** Roguelike Dungeons builds this many levels, y 50 down to 10 (its {@code DungeonGenerator}). */
    static final int LEVELS = 5;
    /** The dungeon entrance lies at most this far from its spawn chunk's corner (its {@code generateNear}). */
    static final int ENTRANCE_REACH = 100;

    private final List<int[]> boxes;

    /** @param boxes complex boxes, each {@code {minX, minZ, maxX, maxZ}}; copied */
    public KeepOut(List<int[]> boxes) {
        List<int[]> copy = new ArrayList<int[]>();
        for (int[] b : boxes) copy.add(b.clone());
        this.boxes = Collections.unmodifiableList(copy);
    }

    public boolean isEmpty() {
        return boxes.isEmpty();
    }

    public List<int[]> boxes() {
        return boxes;
    }

    /** @return whether the column lies in a box grown by {@code margin} */
    public boolean contains(int x, int z, int margin) {
        for (int[] b : boxes) {
            if (x >= b[0] - margin && x <= b[2] + margin && z >= b[1] - margin && z <= b[3] + margin) return true;
        }
        return false;
    }

    /** @return whether the rectangle (inclusive) meets a box grown by {@code margin} */
    public boolean touches(int minX, int minZ, int maxX, int maxZ, int margin) {
        for (int[] b : boxes) {
            if (minX <= b[2] + margin && maxX >= b[0] - margin && minZ <= b[3] + margin && maxZ >= b[1] - margin) {
                return true;
            }
        }
        return false;
    }

    /**
     * How far from a complex box a dungeon entrance must lie so its first level, which grows up to the level range
     * plus twice the scatter from the entrance, stays out of the box; with a chunk of slack.
     */
    public static int entranceMargin(int levelRange, int levelScatter) {
        return Math.max(0, levelRange) + 2 * Math.max(0, levelScatter) + 16;
    }

    /**
     * How far from a spawn chunk's corner any part of its dungeon can reach: the entrance, then every level drifting
     * from the end of the one above. Boxes within this reach are kept out.
     */
    public static int reach(int levelRange, int levelScatter) {
        return ENTRANCE_REACH + LEVELS * (Math.max(0, levelRange) + 2 * Math.max(0, levelScatter)) + WRITE_MARGIN;
    }
}
