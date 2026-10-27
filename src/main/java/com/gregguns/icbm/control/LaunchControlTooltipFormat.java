package com.gregguns.icbm.control;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure layout of the Launch Control tooltip (D41): a one-line summary, the Shift hint and "Added by" without Shift; the
 * detail lines and "Added by" with it. It takes already localized text, so it has no Minecraft types and is unit
 * tested. Both sides.
 */
// Spec: docs/gregguns-icbm-spec/04-launch-control/index.md (Tooltip)
public final class LaunchControlTooltipFormat {

    private LaunchControlTooltipFormat() {}

    /**
     * @param summary   the one-line summary
     * @param shiftHint the hint to hold Shift
     * @param addedBy   the "Added by" line
     * @return the lines shown without Shift
     */
    public static List<String> primary(String summary, String shiftHint, String addedBy) {
        List<String> out = new ArrayList<>();
        out.add(summary);
        out.add(shiftHint);
        out.add(addedBy);
        return out;
    }

    /**
     * @param summary the one-line summary
     * @param details the detail lines, in order
     * @param addedBy the "Added by" line
     * @return the lines shown with Shift held: the summary, every detail, then "Added by"
     */
    public static List<String> secondary(String summary, List<String> details, String addedBy) {
        List<String> out = new ArrayList<>();
        out.add(summary);
        out.addAll(details);
        out.add(addedBy);
        return out;
    }

    /** Splits one line into pieces that fit a pixel width; the client backs it with the font renderer. */
    public interface Wrapper {

        /**
         * @param line one line of text
         * @return the line cut into rows that fit
         */
        List<String> wrap(String line);
    }

    /**
     * @param lines   the tooltip lines
     * @param wrapper how to wrap one line
     * @return every line wrapped, in order; a line that wraps to nothing (an empty line) is kept as it is
     */
    public static List<String> wrapAll(List<String> lines, Wrapper wrapper) {
        List<String> out = new ArrayList<>();
        for (String line : lines) {
            List<String> rows = wrapper.wrap(line);
            if (rows == null || rows.isEmpty()) {
                out.add(line);
            } else {
                out.addAll(rows);
            }
        }
        return out;
    }
}
