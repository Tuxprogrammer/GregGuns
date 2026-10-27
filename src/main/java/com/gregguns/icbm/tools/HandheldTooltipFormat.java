package com.gregguns.icbm.tools;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure layout of the remote detonator and defuser tooltips (D41): the tier line, the summary, the numbers behind
 * Shift (or the Shift hint without it). Takes already localized strings, so it has no Minecraft types.
 */
// Spec: docs/gregguns-icbm-spec/05-detection-defense/handheld-tools.md
public final class HandheldTooltipFormat {

    private HandheldTooltipFormat() {}

    /**
     * @param tierLine the tier line, formatted
     * @param summary  one line of what the tool does
     * @param details  the lines shown behind Shift (range, link, uses)
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
}
