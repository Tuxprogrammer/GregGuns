package com.gregguns.explosion.nuclear;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure layout of the fallout items' and blocks' tooltips (D41): the tier line, the summary, then the details behind
 * Shift (or the Shift hint without it). Takes already localized strings, so it has no Minecraft types.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/nuclear-and-fallout.md (The byproduct chain)
public final class FalloutTooltipFormat {

    private FalloutTooltipFormat() {}

    /**
     * @param tierLine the tier line, formatted
     * @param summary  one line of what the thing is
     * @param details  the lines shown behind Shift (what it turns into, where it comes from)
     * @param hint     the "press Shift" hint
     * @param shift    whether Shift is held
     * @return the lines, without the AddedBy credit (the caller adds it behind Shift)
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
