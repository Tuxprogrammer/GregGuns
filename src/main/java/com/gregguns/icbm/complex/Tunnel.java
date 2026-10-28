package com.gregguns.icbm.complex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** An axis-aligned tunnel segment (a wing corridor or a silo link). Its marks are lights, signs and blast doors. */
public final class Tunnel {

    private final Box bounds;
    private final int wing;
    private final List<Mark> marks = new ArrayList<Mark>();

    Tunnel(Box bounds, int wing) {
        this.bounds = bounds;
        this.wing = wing;
    }

    void add(MarkKind k, int x, int y, int z, String key) {
        marks.add(new Mark(k, x, y, z, key));
    }

    public Box bounds() {
        return bounds;
    }

    /** Wing index (0 N, 1 E, 2 S, 3 W), or -1 for a silo link. */
    public int wing() {
        return wing;
    }

    public List<Mark> marks() {
        return Collections.unmodifiableList(marks);
    }
}
