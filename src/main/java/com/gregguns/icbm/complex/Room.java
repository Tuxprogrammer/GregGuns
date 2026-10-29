package com.gregguns.icbm.complex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A room shell: a kind, its footprint and height (the box's y range), the section it opens from and its marks. The hub
 * is a room too (the assembly bay), with no parent and no primary door.
 */
public final class Room {

    private final int id;
    private final RoomKind kind;
    private Box bounds;
    private final int parent;
    private Door door;
    private final List<Mark> marks = new ArrayList<Mark>();

    Room(int id, RoomKind kind, Box bounds, int parent) {
        this.id = id;
        this.kind = kind;
        this.bounds = bounds;
        this.parent = parent;
    }

    void add(MarkKind k, int x, int y, int z, String key) {
        marks.add(new Mark(k, x, y, z, key));
    }

    void add(MarkKind k, int x, int y, int z, String key, int facing, int extent) {
        marks.add(new Mark(k, x, y, z, key, facing, extent));
    }

    void bounds(Box b) {
        this.bounds = b;
    }

    void door(Door d) {
        this.door = d;
    }

    /** @return the section id (see {@link ComplexPlan#section}); the hub is 0 */
    public int id() {
        return id;
    }

    public RoomKind kind() {
        return kind;
    }

    /** @return the footprint; {@code maxY} is the room's height (floor layer 0, ceiling layer {@code maxY - 1}) */
    public Box bounds() {
        return bounds;
    }

    /** @return the section the room was entered from (a hall, the hub or another room), or -1 for the hub */
    public int parent() {
        return parent;
    }

    /** @return the doorway to the parent, which the room is furnished around (D208), or null for the hub */
    public Door door() {
        return door;
    }

    /** @return the side of the room its doorway is in (0 north, 1 east, 2 south, 3 west), or -1 for the hub */
    public int doorSide() {
        if (door == null) return -1;
        return door.a == id ? door.side : (door.side + 2) & 3;
    }

    public List<Mark> marks() {
        return Collections.unmodifiableList(marks);
    }
}
