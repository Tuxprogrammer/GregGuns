package com.gregguns.icbm.complex;

/** The cell classes of the rasterized complex (one byte per cell). */
public final class Cell {

    /** Natural rock: nothing is written. */
    public static final byte NONE = 0;
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

    private Cell() {}
}
