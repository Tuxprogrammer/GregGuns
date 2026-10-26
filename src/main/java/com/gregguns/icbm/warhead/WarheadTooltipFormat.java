package com.gregguns.icbm.warhead;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Pure layout of a warhead tooltip (D41): explosive, tier, fill, blast radius and center damage (or the annihilation
 * line), then the details behind Shift. It takes already localized templates and names, so it has no Minecraft types
 * and is unit tested.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/warheads-and-fill.md (Tooltip)
public final class WarheadTooltipFormat {

    private WarheadTooltipFormat() {}

    /** The localized templates; {@code %s} slots are filled by this class. */
    public static final class Labels {

        /** Explosive line, one slot: the explosive name. */
        public String explosive;
        /** Tier line, one slot: the colored tier text. */
        public String tier;
        /** Fill line, two slots: fill and cap. */
        public String fill;
        /** Radius line, one slot: the radius. */
        public String radius;
        /** Center damage line, one slot: the damage. */
        public String damage;
        /** The annihilation line, no slots. */
        public String annihilation;
        /** The hint shown without Shift. */
        public String shiftHint;
        /** Detail: cap, one slot. */
        public String detailCap;
        /** Detail: the curve, no slots. */
        public String detailCurve;
        /** Detail: the hand-lit fuse, one slot: seconds. */
        public String detailFuse;
        /** Detail: the demolition use, no slots. */
        public String detailCharge;
        /** Detail: the explosive's special effect text, already localized; may be null. */
        public String detailEffect;
    }

    /** The values of one warhead stack. */
    public static final class Facts {

        /** The localized explosive name. */
        public String explosiveName;
        /** The tier text, already colored. */
        public String tierText;
        /** Charges loaded. */
        public int fill;
        /** The fill cap. */
        public int cap;
        /** The blast radius, blocks. */
        public double radius;
        /** The center damage; ignored when annihilating. */
        public double centerDamage;
        /** True for nuclear and higher: shows the annihilation line instead of damage. */
        public boolean annihilates;
        /** The hand-lit fuse, ticks (D51). */
        public int fuseTicks;
    }

    /**
     * The dynamic display name (D58), such as {@code Condensed T1 Warhead}. One template keeps the order translatable.
     *
     * @param template      the localized format, two slots: the explosive name, then the casing number
     * @param explosiveName the localized explosive name
     * @param casingNumber  1 for T1, 2 for T2
     * @return the name
     */
    public static String displayName(String template, String explosiveName, int casingNumber) {
        return String.format(template, explosiveName, Integer.toString(casingNumber));
    }

    /**
     * @param value a number
     * @return the number with a thousands comma and no decimals, such as {@code 1,200}
     */
    public static String number(double value) {
        return String.format(Locale.ROOT, "%,d", Math.round(value));
    }

    /**
     * @param radius blocks
     * @return one decimal, such as {@code 7.6}
     */
    public static String radius(double radius) {
        return String.format(Locale.ROOT, "%.1f", radius);
    }

    /**
     * @param ticks a fuse in ticks
     * @return seconds, with one decimal only when not whole, such as {@code 6} or {@code 0.5}
     */
    public static String fuseSeconds(int ticks) {
        return ticks % 20 == 0 ? Integer.toString(ticks / 20) : String.format(Locale.ROOT, "%.1f", ticks / 20.0);
    }

    /**
     * @param l     the templates
     * @param f     the warhead's values
     * @param shift whether Shift is held
     * @return the tooltip lines
     */
    public static List<String> lines(Labels l, Facts f, boolean shift) {
        List<String> out = new ArrayList<>();
        out.add(String.format(l.explosive, f.explosiveName));
        out.add(String.format(l.tier, f.tierText));
        out.add(String.format(l.fill, f.fill, f.cap));
        out.add(String.format(l.radius, radius(f.radius)));
        if (f.annihilates) {
            out.add(l.annihilation);
        } else if (f.centerDamage > 0.0) {
            out.add(String.format(l.damage, number(f.centerDamage)));
        }
        if (!shift) {
            out.add(l.shiftHint);
            return out;
        }
        out.add(String.format(l.detailCap, f.cap));
        out.add(l.detailCurve);
        if (l.detailEffect != null && !l.detailEffect.isEmpty()) {
            out.add(l.detailEffect);
        }
        if (l.detailFuse != null && f.fuseTicks > 0) {
            out.add(String.format(l.detailFuse, fuseSeconds(f.fuseTicks)));
        }
        out.add(l.detailCharge);
        return out;
    }
}
