package com.gregguns.icbm.complex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** A room shell: a kind, its footprint, the wing it hangs off and its marks. */
public final class Room {

    private final int id;
    private final RoomKind kind;
    private final Box bounds;
    private final int wing;
    private final List<Mark> marks = new ArrayList<Mark>();

    Room(int id, RoomKind kind, Box bounds, int wing) {
        this.id = id;
        this.kind = kind;
        this.bounds = bounds;
        this.wing = wing;
    }

    void add(MarkKind k, int x, int y, int z, String key) {
        marks.add(new Mark(k, x, y, z, key));
    }

    void add(MarkKind k, int x, int y, int z, String key, int facing, int extent) {
        marks.add(new Mark(k, x, y, z, key, facing, extent));
    }

    public int id() {
        return id;
    }

    public RoomKind kind() {
        return kind;
    }

    public Box bounds() {
        return bounds;
    }

    /** Wing index (0 N, 1 E, 2 S, 3 W), or -1 for the hall and silo annex. */
    public int wing() {
        return wing;
    }

    public List<Mark> marks() {
        return Collections.unmodifiableList(marks);
    }
}
