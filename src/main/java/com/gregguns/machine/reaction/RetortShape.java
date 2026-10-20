package com.gregguns.machine.reaction;

/**
 * The Retort's geometry as plain data: the three StructureLib pieces, the bulb count limits and the parallel and glass
 * formulas. No Minecraft types, so it is unit-tested against the spec's block counts.
 *
 * <p>
 * Layers run top to bottom inside a piece; rows run front to back; characters run left to right. All of {@code B},
 * {@code S} and {@code C} are Reaction Casing: {@code B} (floor and low ring) takes the input, output, energy and
 * maintenance hatches, {@code C} (the cap) takes the Muffler, {@code S} (the shoulder) takes none. {@code G}
 * borosilicate glass, {@code F} steel frame, {@code P} steel pipe casing, {@code U} pump casing, {@code H} heating coil
 * (any tier), {@code ~} the controller, {@code -} must be air, {@code .} anything. The plumbing inside is required: a
 * pump on the floor at the center feeds a pipe column that rises through every bulb (a dip tube), two pipe runs join
 * it to the four coils in the low ring's corners.
 */
// Spec: docs/gregguns-spec/08-machines-production/multiblocks.md (Retort: Layers, Tier and parallel mechanic)
public final class RetortShape {

    /** Fewest bulb layers. */
    public static final int MIN_BULBS = 2;
    /** Most bulb layers. */
    public static final int MAX_BULBS = 6;
    /** Bulb layers of the hologram when no stack size is given. */
    public static final int DEFAULT_BULBS = 3;

    /** Cap, two neck layers and the shoulder; the topmost piece. */
    public static final String[][] TOP = { { ".....", ".CCC.", ".CCC.", ".CCC.", "....." },
        { ".....", ".GGG.", ".G-G.", ".GGG.", "....." }, { ".....", ".GGG.", ".G-G.", ".GGG.", "....." },
        { ".SSS.", "SSSSS", "SS-SS", "SSSSS", ".SSS." } };
    /** One bulb layer, repeated. */
    public static final String[][] BULB = { { ".GGG.", "G---G", "G-P-G", "G---G", ".GGG." } };
    /** Low ring, floor with the controller, and the feet. */
    public static final String[][] BASE = { { ".BBB.", "BHPHB", "B-U-B", "BHPHB", ".BBB." },
        { ".B~B.", "BBBBB", "BBBBB", "BBBBB", ".BBB." }, { ".....", ".F.F.", ".....", ".F.F.", "....." } };

    /** Controller column in the base piece. */
    public static final int CONTROLLER_X = 2;
    /** Rows of the base piece above the controller's row. */
    public static final int BASE_ROWS_ABOVE = 1;
    /** Layers in the top piece. */
    public static final int TOP_LAYERS = TOP.length;

    private RetortShape() {}

    /**
     * @param bulbs bulb layers
     * @return the number of parallel runs, {@code 2 x n}
     */
    public static int parallels(int bulbs) {
        return 2 * bulbs;
    }

    /**
     * @param bulbs bulb layers
     * @return the structure height, {@code 7 + n}
     */
    public static int height(int bulbs) {
        return 7 + bulbs;
    }

    /**
     * @param bulbs any int
     * @return the value limited to the allowed bulb range
     */
    public static int clampBulbs(int bulbs) {
        return Math.max(MIN_BULBS, Math.min(MAX_BULBS, bulbs));
    }

    /**
     * @param piece  a piece
     * @param symbol the character to count
     * @return how many times it appears
     */
    public static int count(String[][] piece, char symbol) {
        int total = 0;
        for (String[] layer : piece) {
            for (String row : layer) {
                for (int i = 0; i < row.length(); i++) {
                    if (row.charAt(i) == symbol) {
                        total++;
                    }
                }
            }
        }
        return total;
    }

    /**
     * @param piece a piece
     * @return its Reaction Casing positions ({@code B}, {@code S} and {@code C})
     */
    public static int casings(String[][] piece) {
        return count(piece, 'B') + count(piece, 'S') + count(piece, 'C');
    }

    /**
     * @param bulbs bulb layers
     * @return the Reaction Casing positions in the whole structure (hatches replace some)
     */
    public static int casingPositions(int bulbs) {
        return casings(TOP) + casings(BASE) + bulbs * casings(BULB);
    }

    /** @return the casings that must be present: all positions minus the hatch allowance (spec: 10) */
    public static int casingMinimum() {
        return casingPositions(0) - 10;
    }

    /**
     * The pressure rule: the vessel runs a recipe only if its lowest glass tier reaches the recipe's voltage tier.
     *
     * @param glassTier  the lowest glass voltage index found
     * @param recipeTier the recipe's voltage index
     * @return whether the recipe may start
     */
    public static boolean glassAllows(int glassTier, int recipeTier) {
        return glassTier >= recipeTier;
    }

    /**
     * @param eut a recipe's EU per tick
     * @return the smallest GT voltage index whose voltage is at least {@code eut}
     */
    public static int voltageIndexFor(long eut) {
        long voltage = 8;
        int tier = 0;
        while (voltage < eut && tier < 14) {
            voltage *= 4;
            tier++;
        }
        return tier;
    }
}
