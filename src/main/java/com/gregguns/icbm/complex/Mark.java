package com.gregguns.icbm.complex;

/** One typed mark at a complex-local cell. Immutable; the filler decides what is built there. */
public final class Mark {

    private final MarkKind kind;
    private final int x, y, z;
    private final String key;

    public Mark(MarkKind kind, int x, int y, int z, String key) {
        this.kind = kind;
        this.x = x;
        this.y = y;
        this.z = z;
        this.key = key;
    }

    public MarkKind kind() {
        return kind;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int z() {
        return z;
    }

    /** Loot-table key, machine role name, guard tier or part name, by kind; never null. */
    public String key() {
        return key;
    }

    @Override
    public String toString() {
        return kind + "(" + key + ")@" + x + "," + y + "," + z;
    }
}
