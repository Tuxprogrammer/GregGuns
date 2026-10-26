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
    /** Plate columns across the body: one vertical column, since the compressed plates are dear (D69). */
    public static final int PLATE_COLUMNS = 1;
    /** Top of the plate block. */
    public static final int PLATES_TOP = 30;
    /** Slot pitch. */
    public static final int STEP = 18;
    /** Height of the schematic part of the panel (enough for the tallest, the T2 body of four plates). */
    public static final int HEIGHT = 142;

    private MissileBenchLayout() {}

    /** @return the plate rows of a page */
    public static int plateRows(MissileBuildSpec spec) {
        return (MissileBenchRules.plateSlots(spec) + PLATE_COLUMNS - 1) / PLATE_COLUMNS;
    }

    /** @return x of plate slot {@code i} */
    public static int plateX(int i) {
        return CENTER - 8 + (i % PLATE_COLUMNS) * STEP;
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
        return CENTER + 32;
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

    // --- caption boxes -------------------------------------------------------------------------------------------
    // Each caption sits in a free margin, never over a slot or the seat: the warhead's to the left of the seat, the
    // guidance one above its slot, the fins one above the outer left fin, the engine's left of the engine slot. A box
    // is {x, y, width, height}; width is the most a caption may take, and {@link #captionRightAligned} says which edge
    // the text hugs.

    /** Height of a caption box (one line of the default font). */
    public static final int CAPTION_HEIGHT = 8;

    /** @return the warhead caption box, left of the seat */
    public static int[] warheadCaption() {
        return new int[] { 2, seatY() + 4, seatX() - 6, CAPTION_HEIGHT };
    }

    /** @return the guidance caption box, above the guidance slot */
    public static int[] guidanceCaption() {
        return new int[] { guidanceX() - 2, guidanceY() - 10, 44, CAPTION_HEIGHT };
    }

    /** @return the fins caption box, above the outer left fin */
    public static int[] finsCaption(MissileBuildSpec spec) {
        return new int[] { finX(0), finY(spec) - 10, 30, CAPTION_HEIGHT };
    }

    /** @return the engine caption box, left of the engine slot and below the fin row */
    public static int[] engineCaption(MissileBuildSpec spec) {
        return new int[] { engineX() - 4 - 40, engineY(spec) + 4, 40, CAPTION_HEIGHT };
    }

    /** @return whether caption {@code i} (0 warhead, 1 guidance, 2 fins, 3 engine) is right-aligned in its box */
    public static boolean captionRightAligned(int i) {
        return i == 0 || i == 3;
    }

    /** @return the four caption boxes in the order warhead, guidance, fins, engine */
    public static int[][] captions(MissileBuildSpec spec) {
        return new int[][] { warheadCaption(), guidanceCaption(), finsCaption(spec), engineCaption(spec) };
    }
}
