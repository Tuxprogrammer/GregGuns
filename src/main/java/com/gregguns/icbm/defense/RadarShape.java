package com.gregguns.icbm.defense;

/**
 * The small radar (HV and EV controllers) as plain data: its one StructureLib piece and the counts the machine and its
 * tooltip use (D125). A 3x3 block two high with a one-block pedestal in the middle of its top; the dish stands on the
 * pedestal. No Minecraft or GT types. Layout is StructureLib's: {@code piece[aisle][row][column]}, aisles front to
 * back, rows top to bottom, columns left to right as seen from the front. {@code H} is Radar Casing where a hatch may
 * go (any of them), {@code C} the pedestal, {@code ~} the controller, a space means anything. Both sides.
 */
// Spec: docs/gregguns-icbm-spec/05-detection-defense/radar.md (The small radar)
public final class RadarShape {

    /** Aisles (front to back). */
    public static final int DEPTH = 3;
    /** Columns. */
    public static final int WIDTH = 3;
    /** Rows. */
    public static final int HEIGHT = 3;
    /** The controller's column in the front aisle. */
    public static final int CONTROLLER_COLUMN = 1;
    /** The controller's row, counted from the top. */
    public static final int CONTROLLER_ROW = 1;
    /** The fewest casing blocks (C and H) the radar needs; the rest may be hatches. */
    public static final int CASING_MINIMUM = 14;
    /** Blocks from the controller block's floor to the top plane, where the dish stands. */
    public static final int DISH_HEIGHT = 2;

    // spotless:off
    /** The radar, one fixed piece. */
    public static final String[][][] PIECES = { {
        { "   ",
          "H~H",
          "HHH" },
        { " C ",
          "HHH",
          "HHH" },
        { "   ",
          "HHH",
          "HHH" } } };
    // spotless:on

    private RadarShape() {}

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

    /** @return the casing positions, the pedestal and the hatch-capable ones together */
    public static int casingPositions() {
        return count('C') + count('H');
    }
}
