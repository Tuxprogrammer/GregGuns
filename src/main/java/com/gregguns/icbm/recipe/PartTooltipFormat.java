package com.gregguns.icbm.recipe;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure layout of an ICBM part tooltip (D41): the tier line, one line of what the part is, and behind Shift the warheads
 * that use it. It takes already localized templates, so it has no Minecraft types and is unit tested.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/warheads-and-fill.md (Parts)
public final class PartTooltipFormat {

    private PartTooltipFormat() {}

    /**
     * @param tierLine  the tier line, already formatted (tier colored)
     * @param summary   the one-line description
     * @param usedIn    the "used in" line, already formatted
     * @param shiftHint the hint shown without Shift
     * @param shift     whether Shift is held
     * @return the lines, without the AddedBy credit (the item adds it behind Shift)
     */
    public static List<String> lines(String tierLine, String summary, String usedIn, String shiftHint, boolean shift) {
        List<String> out = new ArrayList<>();
        out.add(tierLine);
        out.add(summary);
        out.add(shift ? usedIn : shiftHint);
        return out;
    }
}
