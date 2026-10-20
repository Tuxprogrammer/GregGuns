package com.gregguns.worldgen.camp;

import java.util.ArrayList;
import java.util.List;

import com.gregguns.worldgen.layout.Layout;
import com.gregguns.worldgen.layout.LayoutFormatException;
import com.gregguns.worldgen.layout.LayoutParser;

/**
 * Placeholder part layouts: filled boxes with the part's marks, built from generated text in the layout format. They
 * stand in until the camp-parts builder authors the real layouts, and exist for unit tests and the
 * {@code worldgen.camp_flag_init} scenario only; nothing ships them. Layer 0 is solid stone; layer 1 is a row of planks
 * along the south edge, so the entrance side is visible at every rotation.
 */
// Spec: PLAN-M8 section 4 (camp parts are authored later by a separate builder)
public final class CampPlaceholders {

    private CampPlaceholders() {}

    /** Footprint {@code {x, z}} of a placeholder; towers are 3 and 4 square, the rest grow 2 per tier. */
    public static int[] size(CampPart part, int tier) {
        if (part == CampPart.TOWER) {
            return new int[] { tier + 2, tier + 2 };
        }
        return new int[] { part.minW() + 2 * (tier - 1), part.minD() + 2 * (tier - 1) };
    }

    /** The placeholder layout for a part and tier. */
    public static Layout layout(CampPart part, int tier) {
        int[] s = size(part, tier);
        String id = part.layoutId(tier);
        StringBuilder t = new StringBuilder();
        t.append("gregguns-layout 1\n")
            .append("id: ")
            .append(id)
            .append("\nsize: ")
            .append(s[0])
            .append(" 2 ")
            .append(s[1])
            .append("\nground: 0\n\npalette:\nS = minecraft:stone\nP = minecraft:planks\n\n");
        if (part == CampPart.CLOSED_BUNKER) {
            t.append("marks:\nchest.weapon 0 0 0\n\n");
        }
        t.append("layers:\nlayer 0\n");
        for (int z = 0; z < s[1]; z++) {
            t.append(repeat('S', s[0]))
                .append('\n');
        }
        t.append("layer 1\n");
        for (int z = 0; z < s[1]; z++) {
            t.append(repeat(z == s[1] - 1 ? 'P' : '.', s[0]))
                .append('\n');
        }
        try {
            return LayoutParser.parse(id, t.toString());
        } catch (LayoutFormatException e) {
            throw new IllegalStateException("placeholder layout " + id + " is malformed", e);
        }
    }

    /** Every tier of every part, no color variants. */
    public static PartLayouts all() {
        return (part, color) -> choices(part);
    }

    /** As {@link #all} and also tells {@code seen} each (part, color) asked for. */
    public static PartLayouts recording(final List<String> seen) {
        return (part, color) -> {
            seen.add(part + ":" + color);
            return choices(part);
        };
    }

    private static List<Layout> choices(CampPart part) {
        List<Layout> out = new ArrayList<>();
        int tiers = part == CampPart.TOWER ? 2 : CampPart.TIERS;
        for (int tier = 1; tier <= tiers; tier++) {
            out.add(layout(part, tier));
        }
        return out;
    }

    private static String repeat(char c, int n) {
        StringBuilder b = new StringBuilder(n);
        for (int i = 0; i < n; i++) {
            b.append(c);
        }
        return b.toString();
    }
}
