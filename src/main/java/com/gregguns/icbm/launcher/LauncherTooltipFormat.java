package com.gregguns.icbm.launcher;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure layout of the launcher warhead tooltip (D41, D150): the tier line, the summary, the numbers behind Shift (or the
 * hint without it). Takes already localized strings, so it has no Minecraft types.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/launcher-warheads.md
public final class LauncherTooltipFormat {

    private LauncherTooltipFormat() {}

    /**
     * @param tierLine the tier line, formatted
     * @param summary  one line of what the item does
     * @param details  the lines shown behind Shift
     * @param hint     the "press Shift" hint
     * @param shift    whether Shift is held
     * @return the lines, without the AddedBy credit (the item adds it behind Shift)
     */
    public static List<String> lines(String tierLine, String summary, List<String> details, String hint,
        boolean shift) {
        List<String> out = new ArrayList<>();
        out.add(tierLine);
        out.add(summary);
        if (shift) {
            out.addAll(details);
        } else {
            out.add(hint);
        }
        return out;
    }

    /**
     * @param guns registry names of the guns, such as {@code rocket_launcher}
     * @return them joined for a tooltip, with underscores shown as spaces
     */
    public static String gunList(List<String> guns) {
        StringBuilder sb = new StringBuilder();
        for (String g : guns) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(g.replace('_', ' '));
        }
        return sb.toString();
    }
}
