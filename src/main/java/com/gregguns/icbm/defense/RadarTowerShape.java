package com.gregguns.icbm.defense;

/**
 * The radar tower as plain data: its one StructureLib piece and the counts the machine and its tooltip use. No
 * Minecraft or GT types. Layout is StructureLib's: {@code piece[aisle][row][column]}, aisles front to back, rows top to
 * bottom, columns left to right as seen from the front. {@code C} is Radar Casing, {@code H} the same casing where a
 * hatch may go (the bottom ring), {@code B} Gantry Beam (the mast), {@code ~} the controller, a space means anything.
 * Both sides.
 */
// Spec: docs/gregguns-icbm-spec/05-detection-defense/radar.md (Tower)
public final class RadarTowerShape {

    /** Aisles (front to back). */
    public static final int DEPTH = 3;
    /** Columns. */
    public static final int WIDTH = 3;
    /** Rows. */
    public static final int HEIGHT = 7;
    /** The controller's column in the front aisle. */
    public static final int CONTROLLER_COLUMN = 1;
    /** The controller's row, counted from the top. */
    public static final int CONTROLLER_ROW = 5;
    /** The fewest casing blocks (C and H) the tower needs; the rest may be hatches. */
    public static final int CASING_MINIMUM = 30;

    // spotless:off
    /** The tower, one fixed piece. */
    public static final String[][][] PIECES = { {
        { "   ",
          "   ",
          "CCC",
          "CCC",
          "CCC",
          "C~C",
          "HHH" },
        { " B ",
          " B ",
          "CBC",
          "CBC",
          "CBC",
          "CBC",
          "HCH" },
        { "   ",
          "   ",
          "CCC",
          "CCC",
          "CCC",
          "CCC",
          "HHH" } } };
    // spotless:on

    private RadarTowerShape() {}

    /**
     * @param c a shape character
     * @return how many times it appears in the piece
     */
    public static int count(char c) {
        int n = 0;
        for (String[] aisle : PIECES[0]) {
            for (String row : aisle) {
                for (int i = 0; i < row.length(); i++) {
                    if (row.charAt(i) == c) {
                        n++;
                    }
                }
            }
        }
        return n;
    }

    /** @return the casing positions, plain and hatch-capable together */
    public static int casingPositions() {
        return count('C') + count('H');
    }
}
