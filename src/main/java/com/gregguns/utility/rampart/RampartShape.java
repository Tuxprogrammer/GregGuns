package com.gregguns.utility.rampart;

/**
 * The Rampart as plain data: the gatehouse, bay and cap pieces as StructureLib shape strings, the mount limits and the
 * block counts. No Minecraft or GT types, so the unit tests can pin every number to the spec.
 *
 * <p>
 * Shape layout is StructureLib's: {@code piece[aisle][row][column]}, aisles running front to back (the controller's
 * face first), rows top to bottom, columns left to right as seen from the front. The wall runs along the columns, so
 * the gatehouse is on the left and every bay and the cap follow to the right. Characters: {@code T} Bastion Plating
 * of the gatehouse (hatches allowed), {@code P} Bastion Plating of the wall, {@code G} glass, {@code H} the Gun Mount
 * Hatch, {@code ~} the controller, {@code -} must be air.
 *
 * <p>
 * Usage: {@code RampartShape.totalLength(6)} is 16. Both sides.
 */
// Spec: docs/gregguns-spec/08-machines-production/multiblocks.md (Rampart: Pieces and layers, GGS-08-74, GGS-08-75)
public final class RampartShape {

    /** The fewest mounts (bays) a Rampart has. */
    public static final int MIN_MOUNTS = 2;
    /** The most mounts a Rampart can have. */
    public static final int MAX_MOUNTS = 10;
    /** The mount count of the sheet's default Rampart. */
    public static final int DEFAULT_MOUNTS = 6;
    /** Columns (blocks along the wall) of the gatehouse. */
    public static final int GATE_LENGTH = 3;
    /** Columns of one bay. */
    public static final int BAY_LENGTH = 2;
    /** Columns of the end cap. */
    public static final int CAP_LENGTH = 1;
    /** Depth of every piece, front to back. */
    public static final int DEPTH = 3;
    /** Height of the gatehouse. */
    public static final int GATE_HEIGHT = 6;
    /** Height of the wall (bays and cap). */
    public static final int WALL_HEIGHT = 5;
    /** The controller's column in the gatehouse, counted from the left. */
    public static final int CONTROLLER_COLUMN = 1;
    /** The controller's row in the gatehouse, counted from the top (layer y1 of y5..y0). */
    public static final int GATE_CONTROLLER_ROW = 4;
    /** The controller's row in a bay or the cap, counted from the top (layer y1 of y4..y0). */
    public static final int WALL_CONTROLLER_ROW = 3;

    /** The gatehouse: a tower with two glass windows, the controller in the front wall at the second layer. */
    // spotless:off
    public static final String[][] GATE = {
        { "TTT",
          "TGT",
          "TGT",
          "TTT",
          "T~T",
          "TTT" },
        { "TTT",
          "T-T",
          "T-T",
          "T-T",
          "T-T",
          "TTT" },
        { "TTT",
          "TTT",
          "TTT",
          "TTT",
          "TTT",
          "TTT" } };

    /** One bay: a merlon on the left, a crenel on the right with the Gun Mount Hatch in the front of its sill. */
    public static final String[][] BAY = {
        { "P-",
          "P-",
          "PH",
          "PP",
          "PP" },
        { "P-",
          "P-",
          "P-",
          "PP",
          "PP" },
        { "P-",
          "P-",
          "P-",
          "PP",
          "PP" } };

    /** The end cap: a closed one-block-long wall end. */
    public static final String[][] CAP = {
        { "P", "P", "P", "P", "P" },
        { "P", "P", "P", "P", "P" },
        { "P", "P", "P", "P", "P" } };
    // spotless:on

    private RampartShape() {}

    /**
     * @param mounts a requested mount count
     * @return the count limited to {@link #MIN_MOUNTS} to {@link #MAX_MOUNTS}
     */
    public static int clampMounts(int mounts) {
        return Math.max(MIN_MOUNTS, Math.min(MAX_MOUNTS, mounts));
    }

    /**
     * @param mounts the number of mounts (bays)
     * @return the Rampart's length in blocks, {@code 4 + 2n}
     */
    public static int totalLength(int mounts) {
        return GATE_LENGTH + mounts * BAY_LENGTH + CAP_LENGTH;
    }

    /**
     * StructureLib's horizontal offset for a bay: the controller's column counted from the bay's left edge, which is
     * negative because the bay lies to the right of the controller.
     *
     * @param bayIndex which bay, counting from 0 at the one next to the gatehouse
     * @return the offset to pass to {@code checkPiece}
     */
    public static int bayOffset(int bayIndex) {
        return CONTROLLER_COLUMN - (GATE_LENGTH + bayIndex * BAY_LENGTH);
    }

    /**
     * @param mounts the number of mounts
     * @return the horizontal offset of the end cap
     */
    public static int capOffset(int mounts) {
        return CONTROLLER_COLUMN - (GATE_LENGTH + mounts * BAY_LENGTH);
    }

    /**
     * @param bayIndex which bay, counting from 0
     * @return how many blocks along the wall, from the controller, that bay's Gun Mount Hatch is
     */
    public static int hatchDistance(int bayIndex) {
        return GATE_LENGTH + bayIndex * BAY_LENGTH + 1 - CONTROLLER_COLUMN;
    }

    /** @return how many layers above the controller's layer the hatch sits (y2 against y1) */
    public static int hatchRise() {
        return 1;
    }

    /**
     * @param mounts the number of mounts
     * @return the number of Bastion Plating positions ({@code T} and {@code P}) of the whole wall
     */
    public static int platingPositions(int mounts) {
        return count(GATE, "T") + mounts * count(BAY, "P") + count(CAP, "P");
    }

    /**
     * Counts the cells of a piece whose character is one of {@code chars}.
     *
     * @param piece the shape
     * @param chars the characters to count
     * @return the number of matching cells
     */
    public static int count(String[][] piece, String chars) {
        int total = 0;
        for (String[] aisle : piece) {
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

    /**
     * Whether the assembled shape is non-convex (GGS-08-82): some cell on a face of its bounding box (length
     * {@code 4 + 2n}, height 6, depth 3) holds no block, because it is a crenel or lies above the lower wall.
     *
     * @param mounts the number of mounts
     * @return true when at least one face cell of the bounding box is empty
     */
    public static boolean isNonConvex(int mounts) {
        return emptyFaceCells(mounts) > 0;
    }

    /**
     * @param mounts the number of mounts
     * @return the number of cells on the faces of the bounding box that hold no block
     */
    public static int emptyFaceCells(int mounts) {
        int length = totalLength(mounts);
        boolean[][][] solid = new boolean[length][GATE_HEIGHT][DEPTH];
        fill(solid, GATE, 0);
        for (int i = 0; i < mounts; i++) {
            fill(solid, BAY, GATE_LENGTH + i * BAY_LENGTH);
        }
        fill(solid, CAP, GATE_LENGTH + mounts * BAY_LENGTH);
        int empty = 0;
        for (int x = 0; x < length; x++) {
            for (int y = 0; y < GATE_HEIGHT; y++) {
                for (int z = 0; z < DEPTH; z++) {
                    boolean face = x == 0 || x == length - 1
                        || y == 0
                        || y == GATE_HEIGHT - 1
                        || z == 0
                        || z == DEPTH - 1;
                    if (face && !solid[x][y][z]) {
                        empty++;
                    }
                }
            }
        }
        return empty;
    }

    /** Writes a piece into the grid; all pieces are bottom-aligned, so a lower piece leaves its top layers empty. */
    private static void fill(boolean[][][] solid, String[][] piece, int firstColumn) {
        for (int z = 0; z < piece.length; z++) {
            String[] rows = piece[z];
            for (int r = 0; r < rows.length; r++) {
                int layer = rows.length - 1 - r;
                for (int c = 0; c < rows[r].length(); c++) {
                    char ch = rows[r].charAt(c);
                    solid[firstColumn + c][layer][z] = ch != '-' && ch != ' ';
                }
            }
        }
    }
}
