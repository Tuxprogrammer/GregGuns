package com.gregguns.worldgen.layout;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A parsed, validated structure layout (see docs/gregguns-spec/11-worldgen/layout-format.md). Immutable. Cells are
 * addressed {@code (x, y, z)} in layout space: x east, y up, z south, as authored (rotation 0). {@code ground} is the
 * layer that sits at the plan anchor's Y, so layers below it are buried.
 */
public final class Layout {

    /** Cell value for "leave whatever is there". */
    public static final int SKIP = -1;

    /** A named point in layout space (chest, spawner, flag) that the generator acts on after the blocks are down. */
    public static final class Mark {

        public final String tag;
        public final int x;
        public final int y;
        public final int z;

        public Mark(String tag, int x, int y, int z) {
            this.tag = tag;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    private final String id;
    private final int sizeX;
    private final int sizeY;
    private final int sizeZ;
    private final int ground;
    private final List<PaletteEntry> entries;
    private final Map<Character, Integer> paletteIndex;
    private final short[] cells; // [y][z][x], palette index or SKIP
    private final List<Mark> marks;

    Layout(String id, int sizeX, int sizeY, int sizeZ, int ground, List<PaletteEntry> entries,
        Map<Character, Integer> paletteIndex, short[] cells, List<Mark> marks) {
        this.id = id;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.ground = ground;
        this.entries = Collections.unmodifiableList(entries);
        this.paletteIndex = paletteIndex;
        this.cells = cells;
        this.marks = Collections.unmodifiableList(marks);
    }

    public String id() {
        return id;
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

    public int ground() {
        return ground;
    }

    public List<Mark> marks() {
        return marks;
    }

    public List<PaletteEntry> entries() {
        return entries;
    }

    /** Footprint width along world x at the given rotation. */
    public int footprintX(int rotation) {
        return Rotations.width(rotation, sizeX, sizeZ);
    }

    /** Footprint depth along world z at the given rotation. */
    public int footprintZ(int rotation) {
        return Rotations.depth(rotation, sizeX, sizeZ);
    }

    /** Palette index at the cell, or {@link #SKIP}. */
    public int cell(int x, int y, int z) {
        return cells[(y * sizeZ + z) * sizeX + x];
    }

    public PaletteEntry entry(int index) {
        return entries.get(index);
    }

    /** Palette index for a palette character, or SKIP when undefined. */
    public int indexOf(char c) {
        Integer i = paletteIndex.get(c);
        return i == null ? SKIP : i;
    }

    /** Number of cells that write something (not SKIP). */
    public int solidCells() {
        int n = 0;
        for (short s : cells) {
            if (s != SKIP) {
                n++;
            }
        }
        return n;
    }
}
