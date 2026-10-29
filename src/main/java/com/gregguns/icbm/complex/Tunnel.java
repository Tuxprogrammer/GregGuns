package com.gregguns.icbm.complex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A straight hall segment: a major hall ({@value ComplexLayout#MAJOR_WIDTH} wide, off the hub, possibly the silo
 * gallery) or a minor hallway ({@value ComplexLayout#MINOR_WIDTH} wide, between rooms and branches). Its marks are
 * lamps, signs, guard posts at branch points, damage cells and blast doors.
 */
public final class Tunnel {

    private final int id;
    private final Box bounds;
    private final boolean major;
    private final boolean alongX;
    private final int parent;
    private boolean gallery;
    private final List<Mark> marks = new ArrayList<Mark>();

    Tunnel(int id, Box bounds, boolean major, boolean alongX, int parent) {
        this.id = id;
        this.bounds = bounds;
        this.major = major;
        this.alongX = alongX;
        this.parent = parent;
    }

    void add(MarkKind k, int x, int y, int z, String key) {
        marks.add(new Mark(k, x, y, z, key));
    }

    void add(MarkKind k, int x, int y, int z, String key, int facing, int extent) {
        marks.add(new Mark(k, x, y, z, key, facing, extent));
    }

    void gallery(boolean g) {
        this.gallery = g;
    }

    /** @return the section id (see {@link ComplexPlan#section}) */
    public int id() {
        return id;
    }

    public Box bounds() {
        return bounds;
    }

    /** @return true for a major hall, false for a minor hallway */
    public boolean major() {
        return major;
    }

    /** @return true when the hall runs along x (east-west) */
    public boolean alongX() {
        return alongX;
    }

    /** @return true when silos line this hall */
    public boolean gallery() {
        return gallery;
    }

    /** @return the section the hall leaves from */
    public int parent() {
        return parent;
    }

    public List<Mark> marks() {
        return Collections.unmodifiableList(marks);
    }
}
