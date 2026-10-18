package com.gregguns.client.gun;

import java.util.regex.Pattern;

/**
 * Pure layout rules for the ammo HUD, with no Minecraft types so they are unit tested. The HUD is anchored to the
 * bottom-right corner plus the configured offsets, and text is right-aligned by the measured width of the string
 * <em>without</em> formatting codes (fixes CORE-18, where Techguns aligned by {@code String.length() * 6} and counted
 * the codes).
 *
 * <p>
 * Spec: docs/gregguns-spec/01-core/hud-overlays.md (GGS-01-32).
 */
public final class HudLayout {

    /** A section sign followed by any one character: a Minecraft formatting code. */
    private static final Pattern FORMATTING = Pattern.compile("§.");

    private HudLayout() {}

    /**
     * @param text a string that may contain formatting codes
     * @return the string with the codes removed, which is what to measure
     */
    public static String stripFormatting(String text) {
        return FORMATTING.matcher(text)
            .replaceAll("");
    }

    /**
     * The left edge that right-aligns text.
     *
     * @param screenWidth the scaled screen width
     * @param textWidth   the measured width of the text without formatting codes
     * @param offsetX     the configured distance from the right edge
     * @return the x to draw at
     */
    public static int rightAlignedX(int screenWidth, int textWidth, int offsetX) {
        return screenWidth - Math.max(0, offsetX) - textWidth;
    }

    /**
     * The top of a line in a block of lines stacked upward from the bottom corner, so the last line sits closest to
     * the offset and extra lines grow upward.
     *
     * @param screenHeight the scaled screen height
     * @param offsetY      the configured distance from the bottom edge to the bottom of the block
     * @param lineCount    how many lines the block has
     * @param lineIndex    this line, 0 for the first (top) line
     * @param lineHeight   pixels per line
     * @return the y to draw the line at
     */
    public static int lineY(int screenHeight, int offsetY, int lineCount, int lineIndex, int lineHeight) {
        int bottom = screenHeight - Math.max(0, offsetY);
        return bottom - (lineCount - lineIndex) * lineHeight;
    }

    /**
     * @param progress reload progress, clamped to {@code [0, 1]}
     * @param barWidth the full bar width in pixels
     * @return the filled width in pixels
     */
    public static int filledWidth(double progress, int barWidth) {
        double clamped = Math.max(0.0, Math.min(1.0, progress));
        return (int) Math.round(clamped * barWidth);
    }
}
