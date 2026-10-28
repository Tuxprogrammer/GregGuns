package com.gregguns.icbm;

import java.util.ArrayList;
import java.util.List;

import com.gregguns.icbm.control.LaunchControlTooltipFormat;

/**
 * Wraps item tooltip lines to a pixel width so the Shift details, which are full sentences, stay on the screen (D41).
 * {@link #wrapLines} is pure and unit tested; {@link #add} and {@link #addAll} back it with the client font and are
 * only called from {@code addInformation}, which only the client runs.
 */
// Spec: docs/gregguns-icbm-spec/DECISIONS.md (D41)
public final class TooltipWrap {

    /** Pixels a tooltip line may take before it wraps (the warhead and Launch Control tooltips use the same). */
    public static final int WIDTH = 200;

    private TooltipWrap() {}

    /**
     * @param lines   the lines to wrap
     * @param wrapper how to wrap one line
     * @return every line wrapped, in order
     */
    public static List<String> wrapLines(List<String> lines, LaunchControlTooltipFormat.Wrapper wrapper) {
        return LaunchControlTooltipFormat.wrapAll(lines, wrapper);
    }

    /** Adds {@code lines} to {@code tooltip}, each wrapped to {@link #WIDTH} with the client font. */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static void addAll(List tooltip, List<String> lines) {
        net.minecraft.client.gui.FontRenderer font = net.minecraft.client.Minecraft.getMinecraft().fontRenderer;
        tooltip.addAll(wrapLines(new ArrayList<>(lines), line -> font.listFormattedStringToWidth(line, WIDTH)));
    }

    /** Adds one line to {@code tooltip}, wrapped to {@link #WIDTH} with the client font. */
    @SuppressWarnings("rawtypes")
    public static void add(List tooltip, String line) {
        List<String> one = new ArrayList<>();
        one.add(line);
        addAll(tooltip, one);
    }
}
