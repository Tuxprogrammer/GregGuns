package com.gregguns.core.text;

import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

/**
 * The "Added by: GregGuns" tooltip line every GregGuns GregTech machine carries.
 *
 * <p>
 * All our machines are {@code gregtech:gt.blockmachines} items, so NEI's mod-name search never finds them. Putting the
 * mod name in the tooltip makes a text search for "gregguns" match, which is what BartWorks and TecTech do. Common
 * side; the lang lookup happens per call because the lang file is not loaded when machines are constructed.
 */
// Source: BartWorks puts a colored mod name after a translated "Added by ... via" prefix on single-block tooltips:
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/bartworks/util/BWTooltipReference.java#L28-L33
// Source: TecTech does the same with a colored mark (we keep one color so the name is not split by a code):
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/tectech/util/CommonValues.java#L12
// Source: multiblocks pass the colored name to MultiblockTooltipBuilder.toolTipFinisher(authors), which prints
// "<GT5U.MBTT.Mod>: <authors>" as the final line:
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/MultiblockTooltipBuilder.java#L1656-L1716
public final class AddedBy {

    /**
     * Mod name in one color (so the raw tooltip text still contains "GregGuns" unbroken), ending in a reset to gray so
     * later text is not tinted.
     */
    public static final String NAME = EnumChatFormatting.GOLD + "GregGuns" + EnumChatFormatting.GRAY;

    /** GT's own "Added by" lang key, already translated for every language the pack ships. */
    private static final String PREFIX_KEY = "GT5U.MBTT.Mod";

    private AddedBy() {}

    /** The full line for single blocks and hatches: "Added by: GregGuns". */
    public static String line() {
        return StatCollector.translateToLocal(PREFIX_KEY) + ": " + NAME;
    }

    /** Returns a copy of {@code lines} with the line appended. */
    public static String[] append(String[] lines) {
        String[] out = new String[lines.length + 1];
        System.arraycopy(lines, 0, out, 0, lines.length);
        out[lines.length] = line();
        return out;
    }
}
