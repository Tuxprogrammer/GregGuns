package com.gregguns.icbm.complex.palette;

/**
 * A room's aisle in floor-pattern coordinates (D321): the interior cells {@code lo..hi} across it, running the room's
 * whole length along x ({@link #alongX}) or z. A room's aisle is its doorway and the jamb either side, so a floor
 * strip drawn on it lies under the opening and its frame, as wide as the clear way in.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/palette.md (Patterns); D321
public final class Aisle {

    /** The first and last interior cell across the aisle. */
    public final int lo, hi;
    /** True when the aisle runs along x (its cells across are z), false when it runs along z. */
    public final boolean alongX;

    public Aisle(int lo, int hi, boolean alongX) {
        this.lo = Math.min(lo, hi);
        this.hi = Math.max(lo, hi);
        this.alongX = alongX;
    }

    /** @return the aisle's width across */
    public int width() {
        return hi - lo + 1;
    }

    /** @return whether the interior cell (x, z) lies on the aisle */
    public boolean contains(int x, int z) {
        int across = alongX ? z : x;
        return across >= lo && across <= hi;
    }

    @Override
    public String toString() {
        return "aisle " + lo + ".." + hi + (alongX ? " along x" : " along z");
    }
}
