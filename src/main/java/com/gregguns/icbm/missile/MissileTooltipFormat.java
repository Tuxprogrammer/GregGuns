package com.gregguns.icbm.missile;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure layout of a missile body tooltip (D41): tier, fuel, range and guidance, then the details behind Shift (the
 * warhead it carries, the fuels it burns, how to arm it). It takes
 * already localized templates, so it has no Minecraft types and is unit tested. Both sides.
 */
// Spec: docs/gregguns-icbm-spec/03-missiles/missile-bodies.md (Tooltip)
public final class MissileTooltipFormat {

    private MissileTooltipFormat() {}

    /** The localized templates; {@code %s} slots are filled by this class. */
    public static final class Labels {

        /** Tier line, one slot: the colored tier text. */
        public String tier;
        /** Fuel line, one slot: mB. */
        public String fuel;
        /** Range line, two slots: minimum and maximum. */
        public String range;
        /** Guidance line, one slot: tiers. Shown only when above 0. */
        public String guidance;
        /** The hint shown without Shift. */
        public String shiftHint;
        /** Detail: the warhead it carries, one slot: the heaviest warhead tier. */
        public String detailWarhead;
        /** Detail: the fuels the tank burns, one slot: the localized fluid names, comma separated. */
        public String detailFuels;
        /** Detail: how to stack and arm it. */
        public String detailStack;
        /** Detail: the unburned fuel bonus. */
        public String detailBonus;
    }

    /** The values of one body stack. */
    public static final class Facts {

        /** The tier text, already colored. */
        public String tierText;
        /** The tank, mB. */
        public int fuelMb;
        /** Shortest range, blocks. */
        public int minRange;
        /** Longest range, blocks. */
        public int maxRange;
        /** Guidance tiers above the body's own. */
        public int guidance;
        /** The heaviest warhead tier it carries. */
        public int warheadTier;
        /** The localized names of the fuels the tier burns, worst-accepted first; may be empty. */
        public List<String> fuels = new ArrayList<>();
    }

    /**
     * @param l     the templates
     * @param f     the values
     * @param shift whether Shift is held
     * @return the lines, in order
     */
    public static List<String> lines(Labels l, Facts f, boolean shift) {
        List<String> out = new ArrayList<>();
        out.add(String.format(l.tier, f.tierText));
        out.add(String.format(l.fuel, f.fuelMb));
        out.add(String.format(l.range, f.minRange, f.maxRange));
        if (f.guidance > 0) {
            out.add(String.format(l.guidance, f.guidance));
        }
        if (!shift) {
            out.add(l.shiftHint);
            return out;
        }
        out.add(String.format(l.detailWarhead, f.warheadTier));
        if (f.fuels != null && !f.fuels.isEmpty()) {
            out.add(String.format(l.detailFuels, String.join(", ", f.fuels)));
        }
        out.add(l.detailStack);
        out.add(l.detailBonus);
        return out;
    }
}
