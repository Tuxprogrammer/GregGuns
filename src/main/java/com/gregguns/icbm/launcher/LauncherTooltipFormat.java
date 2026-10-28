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
     * @param gunNames the localized display names of the guns (from {@code item.gregguns.<gun>.name})
     * @return them joined for a tooltip
     */
    public static String gunList(List<String> gunNames) {
        return String.join(", ", gunNames);
    }
}
