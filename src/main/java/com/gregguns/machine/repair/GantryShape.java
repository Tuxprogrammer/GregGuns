package com.gregguns.machine.repair;

/**
 * The Armory Gantry as plain data: the head (booth), bay and tail pieces as StructureLib shape strings, the bay limits
 * and the block counts. No Minecraft or GT types, so the unit tests can pin every number to the spec.
 *
 * <p>
 * Shape layout is StructureLib's: {@code piece[aisle][row][column]}, aisles running front to back (the controller's
 * face first), rows top to bottom, columns left to right as seen from the front. Characters: {@code R} gantry casing
 * (hatches allowed), {@code B} gantry beam, {@code H} gantry hoist, {@code L} Titanium frame leg, {@code G} glass,
 * {@code T} repair cradle, {@code F} steel frame box (floor rails and the hoist hook), {@code X} steel gearbox casing
 * (the hoist drive), {@code N} motor casing (hoist motors), {@code P} steel pipe casing (hydraulic runs), {@code ~}
 * controller, {@code -} must be air, space means anything.
 *
 * <p>
 * Usage: {@code GantryShape.totalLength(4)} is 11. Both sides.
 */
// Spec: docs/gregguns-spec/08-machines-production/multiblocks.md (Armory Gantry: Pieces and layers, GGS-08-69)
// Deviation: the sheet draws "anything" as '.', which StructureLib does not treat as a wildcard (see AmmoWorksShape);
// every '.' of the sheet is a space here.
public final class GantryShape {

    /** The fewest bays a Gantry has. */
    public static final int MIN_BAYS = 2;
    /** The most bays a Gantry can have. */
    public static final int MAX_BAYS = 8;
    /** The bay count a hologram without a length channel shows. */
    public static final int DEFAULT_BAYS = 4;
    /** Aisles in the head (the booth). */
    public static final int HEAD_LENGTH = 2;
    /** Aisles in one bay. */
    public static final int BAY_LENGTH = 2;
    /** Aisles in the tail. */
    public static final int TAIL_LENGTH = 1;
    /** Width of every piece. */
    public static final int WIDTH = 7;
    /** Height of every piece. */
    public static final int HEIGHT = 7;
    /** The controller's column in the front aisle, counted from the left. */
    public static final int CONTROLLER_COLUMN = 3;
    /** The controller's row in the front aisle, counted from the top (y2 of 0..6). */
    public static final int CONTROLLER_ROW = 4;

    /**
     * The booth: glass windows, the controller in the front wall, a hoist on the roof, a hydraulic pipe run under the
     * roof beam and the start of the floor rails.
     */
    // spotless:off
    public static final String[][] HEAD = {
        { "   H   ",
          "BBBBBBB",
          "LGGGGGL",
          "LGGGGGL",
          "LRR~RRL",
          "LRRRRRL",
          "LRRRRRL" },
        { "       ",
          "BBBBBBB",
          "LP-S-PL",
          "L-----L",
          "L-----L",
          "LF---FL",
          "LRRRRRL" } };

    /**
     * One bay: a portal frame, then an open gap, with a cradle pad on the floor. The frame carries the hoist (a gearbox
     * between two motors under the beam, a frame-box hook hanging from it); frame-box rails run along the floor and a
     * pipe run hangs under the beam.
     */
    public static final String[][] BAY = {
        { "   H   ",
          "BBBBBBB",
          "L-NXN-L",
          "L--F--L",
          "L-----L",
          "LF---FL",
          "LRTTTRL" },
        { "       ",
          "BBBBBBB",
          "-P-S-P-",
          "-------",
          "-------",
          "-F---F-",
          "RRTTTRR" } };

    /** The tail: a closed end wall. */
    public static final String[][] TAIL = {
        { "       ",
          "BBBBBBB",
          "LRRRRRL",
          "LRRRRRL",
          "LRRRRRL",
          "LRRRRRL",
          "LRRRRRL" } };
    // spotless:on

    private GantryShape() {}

    /**
     * @param bays a requested bay count
     * @return the count limited to {@link #MIN_BAYS} to {@link #MAX_BAYS}
     */
    public static int clampBays(int bays) {
        return Math.max(MIN_BAYS, Math.min(MAX_BAYS, bays));
    }

    /**
     * @param bays the number of bays
     * @return the Gantry's length in blocks, {@code 3 + 2n}
     */
    public static int totalLength(int bays) {
        return HEAD_LENGTH + bays * BAY_LENGTH + TAIL_LENGTH;
    }

    /**
     * @param bayIndex which bay, counting from 0 at the one next to the head
     * @return how many blocks behind the controller's aisle that bay starts
     */
    public static int bayDepth(int bayIndex) {
        return HEAD_LENGTH + bayIndex * BAY_LENGTH;
    }

    /**
     * @param bays the number of bays
     * @return how many blocks behind the controller's aisle the tail starts
     */
    public static int tailDepth(int bays) {
        return HEAD_LENGTH + bays * BAY_LENGTH;
    }

    /**
     * @param bays the number of bays
     * @return how many items one cycle repairs: one per bay
     */
    public static int parallels(int bays) {
        return clampBays(bays);
    }

    /**
     * @param bays the number of bays
     * @return how many gantry casing positions (R) the finished Gantry has
     */
    public static int casingPositions(int bays) {
        return count(HEAD, "R") + bays * count(BAY, "R") + count(TAIL, "R");
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
     * Whether the assembled shape is non-convex: at least one cell inside the bounding box that must be air or is not
     * part of the structure and lies on the outer layer of the box (GGS-08-82).
     *
     * @param bays the number of bays
     * @return true when an outer-layer cell of the bounding box is empty ({@code -} or a space)
     */
    public static boolean hasEmptyOuterCell(int bays) {
        int length = totalLength(bays);
        int aisle = 0;
        String[][][] pieces = new String[bays + 2][][];
        pieces[0] = HEAD;
        for (int i = 0; i < bays; i++) {
            pieces[i + 1] = BAY;
        }
        pieces[bays + 1] = TAIL;
        for (String[][] piece : pieces) {
            for (String[] rows : piece) {
                boolean outerAisle = aisle == 0 || aisle == length - 1;
                for (int r = 0; r < rows.length; r++) {
                    for (int c = 0; c < rows[r].length(); c++) {
                        boolean outer = outerAisle || r == 0
                            || r == rows.length - 1
                            || c == 0
                            || c == rows[r].length() - 1;
                        char ch = rows[r].charAt(c);
                        if (outer && (ch == ' ' || ch == '-')) {
                            return true;
                        }
                    }
                }
                aisle++;
            }
        }
        return false;
    }
}
