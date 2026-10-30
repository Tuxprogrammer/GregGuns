package com.gregguns.icbm.silo;

/**
 * The Missile Silo as plain data: its one StructureLib piece, the block counts and the geometry the machine and the
 * Launch Control derive from the controller's position. No Minecraft or GT types, so the unit tests can pin every
 * number to the spec.
 *
 * <p>
 * Shape layout is StructureLib's: {@code piece[aisle][row][column]}, aisles running front to back (the controller's
 * wall first), rows top to bottom, columns left to right as seen from the front. Characters: {@code C} wall (GT Light
 * or Dark Concrete, see {@link SiloConcrete}), {@code F} the same concrete in the floor's outer ring (D117: hatches
 * may replace C, F, H and G cells, never B or P), {@code P} a Galacticraft launch pad block (the shaft floor, either
 * the nine unformed blocks or the formed pad), {@code G} clear glass (Tinkers' Clear Glass or EnderIO's Quite Clear
 * Glass, see {@link SiloGlass}) where a hatch or the controller may replace it (the three rings just above the floor),
 * {@code B} Gantry Beam (the rim), {@code ~} controller, a space means anything. {@code H} is a hatch-column cell:
 * the front face is five wide in its three window rows, a 3x3 clear glass window around the controller with one
 * column on each side of it, flush with the glass (the owner's build, D299). An {@code H} cell takes the same concrete
 * as {@code C}, a hatch or the Launch Control, and counts as casing. The 3x3 shaft is also spaces, not "air", so a
 * body stacked by hand in it does not unform the silo; the missile's own climb check keeps the way up clear.
 *
 * <p>
 * Usage: {@code SiloShape.SHAFT_CENTER_DEPTH} is 2. Both sides.
 */
// Spec: docs/gregguns-icbm-spec/03-missiles/missile-silo.md (Structure)
public final class SiloShape {

    /** Aisles (front to back). */
    public static final int DEPTH = 5;
    /** Columns. */
    public static final int WIDTH = 5;
    /** Rows (layers), the floor included. */
    public static final int HEIGHT = 12;
    /** The controller's column in the front aisle, counted from the left. */
    public static final int CONTROLLER_COLUMN = 2;
    /** The controller's row, counted from the top: two layers above the floor. */
    public static final int CONTROLLER_ROW = 9;
    /** How many blocks behind the controller the shaft's center column is. */
    public static final int SHAFT_CENTER_DEPTH = 2;
    /**
     * How far below the controller the shaft floor's surface is, so the missile's feet are at {@code controllerY - 1}.
     */
    public static final int FLOOR_SURFACE_BELOW_CONTROLLER = 1;
    /**
     * The hatch column on the right of a viewer facing the controller (counted from the left of the shape). Both the
     * world-generation turn ({@code SiloSlot}) and a north-facing controller ({@code DevStructureBuilder}: column 0 at
     * x-2, the west, which is a south-looking viewer's right) put shape column 0 there. Generated and dev-built silos
     * carry the Input Bus (controller row) over the Energy Hatch (the row under it) in it, and the Launch Control on
     * top.
     */
    public static final int HATCH_COLUMN_RIGHT = 0;
    /** The hatch column on the viewer's left: the Input Hatch (controller row) over the Maintenance Hatch. */
    public static final int HATCH_COLUMN_LEFT = 4;
    /**
     * Where a built silo's Launch Control stands (D288, D299): the front aisle, the top of the right hatch column,
     * {@link #LAUNCH_CONTROL_RISE} rows above the controller, outside the glass window. Any hatch position takes a
     * Launch Control; this is the one the generated complex and the dev builder use.
     */
    public static final int LAUNCH_CONTROL_COLUMN = HATCH_COLUMN_RIGHT;
    /** How many rows above the controller the built silo's Launch Control stands: level with the window's top row. */
    public static final int LAUNCH_CONTROL_RISE = 1;
    /**
     * How many rows above the controller the silo's name sign hangs ("Silo N", read by the Launch Control's screen):
     * on the concrete over the glass window, in the controller's column.
     */
    public static final int NAME_SIGN_RISE = 2;
    /** Shaft side length, blocks. */
    public static final int SHAFT_WIDTH = 3;
    /** The fewest casing blocks (C, F and H) the silo needs to form; the rest of the positions may be hatches. */
    public static final int CASING_MINIMUM = 84;

    /** The silo, one fixed piece. */
    // spotless:off
    public static final String[][][] PIECES = { SILO() };

    private static String[][] SILO() {
        return new String[][] {
          { " BBB ",
            " CCC ",
            " CCC ",
            " CCC ",
            " CCC ",
            " CCC ",
            " CCC ",
            " CCC ",
            "HGGGH",
            "HG~GH",
            "HGGGH",
            " FFF " },
          { "B   B",
            "C   C",
            "C   C",
            "C   C",
            "C   C",
            "C   C",
            "C   C",
            "C   C",
            "G   G",
            "G   G",
            "G   G",
            "FPPPF" },
          { "B   B",
            "C   C",
            "C   C",
            "C   C",
            "C   C",
            "C   C",
            "C   C",
            "C   C",
            "G   G",
            "G   G",
            "G   G",
            "FPPPF" },
          { "B   B",
            "C   C",
            "C   C",
            "C   C",
            "C   C",
            "C   C",
            "C   C",
            "C   C",
            "G   G",
            "G   G",
            "G   G",
            "FPPPF" },
          { " BBB ",
            " CCC ",
            " CCC ",
            " CCC ",
            " CCC ",
            " CCC ",
            " CCC ",
            " CCC ",
            " GGG ",
            " GGG ",
            " GGG ",
            " FFF " }
        };
    }
    // spotless:on

    private SiloShape() {}

    /** @return the single piece, {@code [aisle][row][column]} */
    public static String[][] piece() {
        return PIECES[0];
    }

    /**
     * Counts the cells of the piece whose character is one of {@code chars}.
     *
     * @param chars the characters to count
     * @return the number of matching cells
     */
    public static int count(String chars) {
        int total = 0;
        for (String[] aisle : piece()) {
            for (String row : aisle) {
                for (int i = 0; i < row.length(); i++) {
                    if (chars.indexOf(row.charAt(i)) >= 0) {
                        total++;
                    }
                }
            }
        }
        return total;
    }

    /** @return the number of casing positions (C, F and the hatch columns H) */
    public static int casingPositions() {
        return count("CFH");
    }

    /**
     * @return the number of positions where a hatch may go (D117): walls, hatch columns, glass, the floor ring and the
     *         controller
     */
    public static int hatchPositions() {
        return count("C") + count("H") + count("G") + count("F") + count("~");
    }

    /**
     * Whether the shape is non-convex: at least one cell on the outer layer of its bounding box is empty.
     *
     * @return true when an outer-layer cell is a space
     */
    public static boolean hasEmptyOuterCell() {
        String[][] p = piece();
        for (int a = 0; a < p.length; a++) {
            for (int r = 0; r < p[a].length; r++) {
                for (int c = 0; c < p[a][r].length(); c++) {
                    boolean outer = a == 0 || a == p.length - 1
                        || r == 0
                        || r == p[a].length - 1
                        || c == 0
                        || c == p[a][r].length() - 1;
                    if (outer && p[a][r].charAt(c) == ' ') {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
