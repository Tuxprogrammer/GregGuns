package com.gregguns.worldgen.spawner;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure layout of a hole spawner's tooltip (D41): the tier line and the summary, then the details behind Shift. It takes
 * already localized text, so it has no Minecraft types and is unit tested. Both sides.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-guard-spawner.md (Tooltip)
public final class HoleTooltipFormat {

    private HoleTooltipFormat() {}

    /**
     * @param tierLine the localized tier line
     * @param summary  one line saying which NPCs the block places
     * @param details  the rules behind Shift: range, cap and "break it and it stays broken"
     * @param hint     the "press Shift" hint
     * @param shift    whether Shift is held
     * @return the lines, in order
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
