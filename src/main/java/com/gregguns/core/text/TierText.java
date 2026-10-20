package com.gregguns.core.text;

import net.minecraft.util.EnumChatFormatting;

import com.gregguns.api.Tier;

import gregtech.api.enums.GTValues;

/**
 * The one place a tooltip gets a voltage tier's name, colored the way GT and GTNH color it (green LV, gold MV and so
 * on). Reads GT's own color table, so a color change in GT follows without an edit here. (GT's helper lives in
 * {@code GTUtility}, which cannot load in a unit test, so this repeats its one-line body over the same table.) Both
 * sides.
 *
 * <p>
 * Usage: {@code translateToLocalFormatted("gregguns.gun.tooltip.charger", TierText.colored(Tier.HV), eut)}.
 */
// Spec: docs/gregguns-spec/06-items/ammo.md (tooltips name tiers in GT colors)
public final class TierText {

    private TierText() {}

    /**
     * @param tier a GregGuns tier
     * @return its GT short name wrapped in GT's tier color and a trailing reset code
     */
    public static String colored(Tier tier) {
        // Source:
        // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTUtility.java#L502-L504
        // Colors: GTValues.TIER_COLORS, same tag, GTValues.java#L173-L191.
        int i = tier.voltageIndex();
        return GTValues.TIER_COLORS[i] + GTValues.VN[i] + EnumChatFormatting.RESET;
    }
}
