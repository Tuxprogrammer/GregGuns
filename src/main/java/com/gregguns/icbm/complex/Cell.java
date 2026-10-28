package com.gregguns.icbm.complex;

/**
 * The cell roles of the rasterized complex (one byte per cell). The writer resolves the building roles (wall, floor,
 * ceiling, lamp, door frame, furniture) through the owning section's palette style, and the rest by themselves.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Raster)
public final class Cell {

    /** Natural rock: nothing is written. */
    public static final byte NONE = 0;
    /** A section's perimeter: the room's one wall block. */
    public static final byte WALL = 1;
    public static final byte AIR = 2;
    public static final byte RAIL_NS = 3;
    public static final byte RAIL_EW = 4;
    public static final byte SILO_CONCRETE = 5;
    public static final byte SILO_GLASS = 6;
    public static final byte SILO_BEAM = 7;
    public static final byte SILO_PAD = 8;
    public static final byte CONTROLLER = 9;
    public static final byte HATCH_BUS = 10;
    public static final byte HATCH_MAINTENANCE = 11;
    public static final byte HATCH_ENERGY = 12;
    public static final byte HATCH_POWER = 13;
    /** Air plus a ladder (the escape shaft inside the level). */
    public static final byte LADDER = 14;
    /** The HV radar controller of a radar station on the surface (a GT machine, like {@link #CONTROLLER}). */
    public static final byte RADAR_CONTROLLER = 15;
    /** The floor layer (y 0) inside a section: the style's floor pattern. */
    public static final byte FLOOR = 16;
    /** The ceiling layer (the section's top layer) inside a section: the style's ceiling pattern. */
    public static final byte CEILING = 17;
    /**
     * A lamp flush in the ceiling layer, or a light tile flush in the floor layer. The shell writes it dark; the
     * contents pass lights it by the activity level (D36).
     */
    public static final byte LAMP = 18;
    /** The DOOR role: a jamb or lintel cell around a doorway, the style's accent (its wall when it has none). */
    public static final byte DOOR_FRAME = 19;
    /** A window pane in a wall (Launch Control over the silo gallery). */
    public static final byte WINDOW = 20;
    /** Furniture: a table, a console, a bunk or a shelf, from the style. */
    public static final byte FURN_TABLE = 21;
    public static final byte FURN_CONSOLE = 22;
    public static final byte FURN_BUNK = 23;
    public static final byte FURN_SHELF = 24;
    /** A basin of standing water sunk into the floor layer (water treatment). */
    public static final byte BASIN = 25;

    private Cell() {}

    /** @return whether a player walks through the cell at foot or head height */
    public static boolean open(byte c) {
        return c == AIR || c == RAIL_NS || c == RAIL_EW || c == LADDER;
    }

    /** @return whether the cell is one of the four furniture roles */
    public static boolean furniture(byte c) {
        return c >= FURN_TABLE && c <= FURN_SHELF;
    }
}
