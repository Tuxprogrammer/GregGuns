package com.gregguns.worldgen.camp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * The kinds of building a Military Base leaf can hold, with the size rules that decide which kinds fit a leaf. Pure.
 * Sizes are the minimum footprint per the notes (camp_parts.md); a part may turn 90 degrees to fit, so the fit test
 * tries both orientations. A leaf may be larger than a part's maximum: the part is then placed inside it.
 *
 * <p>
 * Layout ids follow {@code camp_<part>_<tier>} with tier 1..{@link #TIERS}, optionally with a color suffix for parts
 * that use camo nets ({@code camp_tent_2_snow}); the placer takes the largest tier that fits.
 */
// Spec: docs/techguns-spec/11-worldgen/structure-notes/camp_parts.md (Choosing a part for a leaf; parts 1..9)
public enum CampPart {

    /** Crate piles, sandbag rings and rows. Regular and border. */
    PROPS(1, 1, true, true, false),
    /** Rows of shipping containers. Regular. */
    CONTAINERS(2, 2, true, false, false),
    /** Closed bunker with crate interior. Regular and (small) border. */
    CLOSED_BUNKER(3, 5, true, true, false),
    /** Wooden barracks with beds. Regular. */
    BARRACKS(7, 7, true, false, false),
    /** Helipad, 7 to 10 square. Regular. */
    HELIPAD(7, 7, true, false, false),
    /** Fuel tank farm. Regular. */
    TANK_FARM(4, 4, true, false, false),
    /** Open small bunker with firing slits. Regular and border. */
    SMALL_BUNKER(5, 5, true, true, false),
    /** The small factory building. Regular. */
    FACTORY(9, 6, true, false, false),
    /** Camo tent or pavilion. Regular. */
    TENT(3, 3, true, false, false),
    /** Watch tower, 3 or 4 square. Corner only. */
    TOWER(3, 3, false, false, true);

    /** Size variants a part may ship, picked by leaf size. */
    public static final int TIERS = 3;

    private static final List<CampPart> REGULAR = select(true);
    private static final List<CampPart> BORDER = select(false);

    private final int minW;
    private final int minD;
    private final boolean regular;
    private final boolean border;
    private final boolean corner;

    CampPart(int minW, int minD, boolean regular, boolean border, boolean corner) {
        this.minW = minW;
        this.minD = minD;
        this.regular = regular;
        this.border = border;
        this.corner = corner;
    }

    public int minW() {
        return minW;
    }

    public int minD() {
        return minD;
    }

    /** True when the part fits a {@code w x d} leaf in either orientation. */
    public boolean fits(int w, int d) {
        return w >= minW && d >= minD || w >= minD && d >= minW;
    }

    /** Layout id for a size tier, for example {@code camp_barracks_2}. */
    public String layoutId(int tier) {
        return "camp_" + name().toLowerCase(Locale.ROOT) + "_" + tier;
    }

    /** Layout id for a size tier and camo color suffix, for example {@code camp_tent_2_snow}. */
    public String layoutId(int tier, String colorSuffix) {
        return layoutId(tier) + "_" + colorSuffix;
    }

    public boolean isCorner() {
        return corner;
    }

    /** The parts that can roll for a regular leaf, in a fixed order (the roll indexes this list). */
    public static List<CampPart> regularParts() {
        return REGULAR;
    }

    /** The parts that can roll for a border leaf, in a fixed order. */
    public static List<CampPart> borderParts() {
        return BORDER;
    }

    private static List<CampPart> select(boolean regularKind) {
        List<CampPart> out = new ArrayList<>();
        for (CampPart p : values()) {
            if (regularKind ? p.regular : p.border) {
                out.add(p);
            }
        }
        return Collections.unmodifiableList(out);
    }
}
