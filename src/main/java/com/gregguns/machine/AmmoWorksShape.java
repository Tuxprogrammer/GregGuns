package com.gregguns.machine;

/**
 * The Ammunition Works as plain data and arithmetic: the three structure pieces as StructureLib shape strings, the bay
 * limits, the parallel formula, the belt speed and the casing minimum. No Minecraft or GT types, so the unit tests can
 * pin every number to the spec.
 *
 * <p>
 * Shape layout is StructureLib's: {@code piece[aisle][row][column]}, aisles running front to back (the controller's
 * face first), rows top to bottom, columns left to right as seen from the front. Characters: {@code F} feeder casing,
 * {@code A} plain casing, {@code O} packing casing, {@code M} ram housing, {@code I} belt (GT item pipe casing),
 * {@code ~} controller, {@code -} must be air, space means anything.
 *
 * <p>
 * Usage: {@code AmmoWorksShape.parallels(4, 3)} is 48 (four bays, HV). Both sides.
 */
// Spec: docs/gregguns-spec/08-machines-production/multiblocks.md (Ammunition Works: Pieces and layers, Tier and
// parallel mechanic)
// Deviation: the sheet draws "anything" as '.', which StructureLib 1.4.45 does not treat as a wildcard; the spec's own
// note asks the first prototype to confirm the characters. The reserved ones are ' ' (skip), '-' (air) and '~'
// (controller), so every '.' of the sheet is a space here. The sheet's six-row feeder and bay layers are padded with an
// empty top row to the station's seven so all pieces share one vertical offset.
public final class AmmoWorksShape {

    /** The fewest press bays a working hall has. */
    public static final int MIN_BAYS = 2;
    /** The most press bays a hall can have. */
    public static final int MAX_BAYS = 8;
    /** Aisles (blocks of length) in the feeder. */
    public static final int FEEDER_LENGTH = 3;
    /** Aisles in one press bay. */
    public static final int BAY_LENGTH = 3;
    /** Aisles in the packing station. */
    public static final int STATION_LENGTH = 4;
    /** Width of every piece. */
    public static final int WIDTH = 5;
    /** Height of every piece. */
    public static final int HEIGHT = 7;
    /** The controller's column in the front aisle, counted from the left. */
    public static final int CONTROLLER_COLUMN = 2;
    /** The controller's row in the front aisle, counted from the top (the second layer from the bottom). */
    public static final int CONTROLLER_ROW = 5;

    /** Hatches the casing minimum forgives, as the sheet fixes it. */
    public static final int HATCH_ALLOWANCE = 12;
    /** Parallels per bay per voltage tier. */
    public static final int PARALLELS_PER_BAY_PER_TIER = 4;
    /** Base speed gained per belt tier; the duration multiplier is {@code 1 / (1 + 0.25 x (beltTier + 1))}. */
    public static final double BELT_STEP = 0.25;

    /** The feeder: hopper corners on top, the controller in the front wall. */
    // spotless:off
    public static final String[][] FEEDER = {
        { "     ",
          "F   F",
          "AAAAA",
          "FFFFF",
          "FFFFF",
          "FF~FF",
          "AAAAA" },
        { "     ",
          "     ",
          "AAAAA",
          "A---A",
          "A---A",
          "A---A",
          "AAIAA" },
        { "     ",
          "F   F",
          "AAAAA",
          "A---A",
          "A---A",
          "A---A",
          "AAIAA" } };

    /** One press bay: a ram housing stands in the roof at its middle aisle. */
    public static final String[][] BAY = {
        { "     ",
          "     ",
          "AAAAA",
          "A---A",
          "A---A",
          "A---A",
          "AAIAA" },
        { "     ",
          "  M  ",
          "AAMAA",
          "A---A",
          "A---A",
          "A---A",
          "AAIAA" },
        { "     ",
          "     ",
          "AAAAA",
          "A---A",
          "A---A",
          "A---A",
          "AAIAA" } };

    /** The packing station: a taller hall, a crane column and the output wall. */
    public static final String[][] STATION = {
        { "     ",
          "AAAAA",
          "A---A",
          "A---A",
          "A---A",
          "A---A",
          "AAIAA" },
        { "  O  ",
          "AAOAA",
          "A---A",
          "A---A",
          "A---A",
          "A---A",
          "AAIAA" },
        { "  O  ",
          "AAOAA",
          "A---A",
          "A---A",
          "A---A",
          "A---A",
          "AAIAA" },
        { "     ",
          "AAAAA",
          "AOOOA",
          "AOOOA",
          "AOOOA",
          "AOOOA",
          "AAAAA" } };
    // spotless:on

    private AmmoWorksShape() {}

    /**
     * @param bays a requested bay count
     * @return the count limited to {@link #MIN_BAYS} to {@link #MAX_BAYS}
     */
    public static int clampBays(int bays) {
        return Math.max(MIN_BAYS, Math.min(MAX_BAYS, bays));
    }

    /**
     * @param bays the number of press bays
     * @return the hall's length in blocks, {@code 7 + 3n}
     */
    public static int totalLength(int bays) {
        return FEEDER_LENGTH + bays * BAY_LENGTH + STATION_LENGTH;
    }

    /**
     * @param bayIndex which bay, counting from 0 at the one next to the feeder
     * @return how many blocks behind the controller's aisle that bay starts
     */
    public static int bayDepth(int bayIndex) {
        return FEEDER_LENGTH + bayIndex * BAY_LENGTH;
    }

    /**
     * @param bays the number of press bays
     * @return how many blocks behind the controller's aisle the packing station starts
     */
    public static int stationDepth(int bays) {
        return FEEDER_LENGTH + bays * BAY_LENGTH;
    }

    /**
     * The sheet's formula: {@code 4 x n x tier}.
     *
     * @param bays the number of press bays
     * @param tier GT's voltage tier of the strongest energy hatch ({@code GTUtility.getTier}, LV is 1)
     * @return the most recipes the hall may start at once; at least 1
     */
    public static int parallels(int bays, int tier) {
        return Math.max(1, PARALLELS_PER_BAY_PER_TIER * bays * tier);
    }

    /**
     * @param beltTier the item pipe casing tier read from the belt, as GT numbers it (1 to 8)
     * @return the multiplier applied to a recipe's duration: {@code 1 / (1 + 0.25 x (beltTier + 1))}
     */
    public static double speedMultiplier(int beltTier) {
        return 1.0 / (1.0 + BELT_STEP * (Math.max(0, beltTier) + 1));
    }

    /**
     * @param bays the number of press bays
     * @return how many casing positions (F, A, O and M) the finished hall has
     */
    public static int casingPositions(int bays) {
        return count(FEEDER, "FAOM") + bays * count(BAY, "FAOM") + count(STATION, "FAOM");
    }

    /**
     * @param bays the number of press bays
     * @return the fewest casings a formed hall must show: the positions minus {@link #HATCH_ALLOWANCE}
     */
    public static int casingMinimum(int bays) {
        return casingPositions(bays) - HATCH_ALLOWANCE;
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
}
