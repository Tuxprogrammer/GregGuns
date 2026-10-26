package com.gregguns.icbm.production;

/**
 * Where each slot of a missile page sits, in the shape of the missile (nose and warhead seat on top, body plates down
 * the middle, fins either side at the bottom, the engine at the base, the guidance computer beside the nose). One table
 * for the in-game NASA Workbench page and the NEI page, so the two always match. Coordinates are the top-left of a
 * 16 by 16 slot in a {@link #WIDTH} wide panel. No Minecraft types, both sides.
 */
// Spec: docs/gregguns-icbm-spec/03-missiles/build-and-recipes.md (NASA Workbench page)
public final class MissileBenchLayout {

    /** Width of the schematic panel. */
    public static final int WIDTH = 168;
    /** Panel x of the missile's center line. */
    public static final int CENTER = 84;
    /** Plate columns across the body. */
    public static final int PLATE_COLUMNS = 2;
    /** Top of the plate block. */
    public static final int PLATES_TOP = 30;
    /** Slot pitch. */
    public static final int STEP = 18;
    /** Height of the schematic part of the panel (enough for the tallest, the T2 body). */
    public static final int HEIGHT = 124;

    private MissileBenchLayout() {}

    /** @return the plate rows of a page */
    public static int plateRows(MissileBuildSpec spec) {
        return (MissileBenchRules.plateSlots(spec) + PLATE_COLUMNS - 1) / PLATE_COLUMNS;
    }

    /** @return x of plate slot {@code i} */
    public static int plateX(int i) {
        return CENTER - STEP + (i % PLATE_COLUMNS) * STEP;
    }

    /** @return y of plate slot {@code i} */
    public static int plateY(int i) {
        return PLATES_TOP + (i / PLATE_COLUMNS) * STEP;
    }

    /** @return y of the first row below the plates: the fins and, one row lower, the engine */
    public static int baseY(MissileBuildSpec spec) {
        return PLATES_TOP + plateRows(spec) * STEP;
    }

    /** @return x of fin slot {@code i}: two on the left of the body, two on the right */
    public static int finX(int i) {
        switch (i) {
            case 0:
                return CENTER - 62;
            case 1:
                return CENTER - 44;
            case 2:
                return CENTER + 28;
            default:
                return CENTER + 46;
        }
    }

    /** @return y of every fin slot */
    public static int finY(MissileBuildSpec spec) {
        return baseY(spec);
    }

    /** @return x of the engine slot, under the body */
    public static int engineX() {
        return CENTER - 8;
    }

    /** @return y of the engine slot */
    public static int engineY(MissileBuildSpec spec) {
        return baseY(spec) + STEP;
    }

    /** @return x of the guidance slot, beside the nose */
    public static int guidanceX() {
        return CENTER + 28;
    }

    /** @return y of the guidance slot */
    public static int guidanceY() {
        return PLATES_TOP;
    }

    /** @return x of the warhead seat, a drawn frame at the nose (the warhead is stacked on the pad, not here) */
    public static int seatX() {
        return CENTER - 8;
    }

    /** @return y of the warhead seat */
    public static int seatY() {
        return 8;
    }

    /** @return x of the result slot, at the base on the right */
    public static int resultX() {
        return CENTER + 46;
    }

    /** @return y of the result slot */
    public static int resultY(MissileBuildSpec spec) {
        return engineY(spec);
    }
}
