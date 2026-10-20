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

    /**
     * One "name: N rounds" entry.
     *
     * @param name       the localized name
     * @param count      the rounds
     * @param oneFormat  the localized template for exactly one round, for example "%s: 1 round"
     * @param manyFormat the localized template for any other count, for example "%s: %s rounds"
     * @return the entry text
     */
    public static String roundsEntry(String name, int count, String oneFormat, String manyFormat) {
        return count == 1 ? String.format(oneFormat, name) : String.format(manyFormat, name, count);
    }

    /**
     * A label followed by one indented line per entry; cut to the first few and a hint unless Shift is held.
     *
     * @param label    the localized label, for example "Used by:"
     * @param entries  localized entries, for example "PDW: 20 rounds"
     * @param shift    whether Shift is held
     * @param moreHint the localized hint added when the list is cut
     * @return the tooltip lines; empty when there are no entries
     */
    public static List<String> block(String label, List<String> entries, boolean shift, String moreHint) {
        List<String> out = new ArrayList<>();
        if (entries.isEmpty()) {
            return out;
        }
        out.add(label);
        boolean cut = !shift && entries.size() > COMPACT_LIMIT;
        int shown = cut ? COMPACT_LIMIT - 1 : entries.size();
        for (int i = 0; i < shown; i++) {
            out.add("  " + entries.get(i));
        }
        if (cut) {
            out.add("  ...");
            out.add(moreHint);
        }
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
