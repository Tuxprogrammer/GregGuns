package com.gregguns.item.ammo;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure layout of a "label: a, b, c" compatibility list. Longer than {@link #COMPACT_LIMIT} entries it shows the first
 * few and a "hold Shift" hint, unless Shift is held. Takes already localized names, so it has no Minecraft types.
 */
// Spec: docs/gregguns-spec/06-items/ammo.md (tooltip compatibility notes)
public final class AmmoTooltipFormat {

    /** Entries at which a list is shown whole; longer lists are cut. */
    public static final int COMPACT_LIMIT = 4;

    private AmmoTooltipFormat() {}

    /**
     * @param label    the localized label, for example "Fills magazines:"
     * @param names    localized entry names
     * @param shift    whether Shift is held
     * @param moreHint the localized hint added when the list is cut, for example "Hold Shift for all"
     * @return the tooltip lines; empty when there are no names
     */
    public static List<String> lines(String label, List<String> names, boolean shift, String moreHint) {
        List<String> out = new ArrayList<>();
        if (names.isEmpty()) {
            return out;
        }
        if (shift || names.size() <= COMPACT_LIMIT) {
            out.add(label + " " + join(names));
            return out;
        }
        out.add(label + " " + join(names.subList(0, COMPACT_LIMIT - 1)) + ", ...");
        out.add(moreHint);
        return out;
    }

    private static String join(List<String> names) {
        StringBuilder sb = new StringBuilder();
        for (String name : names) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(name);
        }
        return sb.toString();
    }
}
