package com.gregguns.icbm.defense;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * The Radar Casing's tooltip lines (D41, D338): the tier, one summary line and a Shift hint; behind Shift, what the
 * casing builds and where the hatches go. Pure: the caller hands in the translator and the colored tier, so the lines
 * are testable without Minecraft. The "Added by" credit is the caller's, behind Shift.
 */
// Spec: docs/gregguns-icbm-spec/05-detection-defense/radar.md (Radar Casing)
public final class RadarCasingTooltipFormat {

    /** Lang key of the summary line; {@code .1} and {@code .2} are the details behind Shift. */
    public static final String TIP = "gregguns.icbm.radar_casing.tip";
    /** Number of detail lines behind Shift. */
    static final int DETAILS = 2;

    private RadarCasingTooltipFormat() {}

    /**
     * @param tr          translates a lang key (the game's {@code StatCollector.translateToLocal})
     * @param coloredTier the casing's tier, already colored ({@code TierText.colored(Tier.HV)})
     * @param shift       whether Shift is held
     * @return the lines, without the "Added by" credit
     */
    public static List<String> lines(Function<String, String> tr, String coloredTier, boolean shift) {
        List<String> details = new ArrayList<>();
        for (int i = 1; i <= DETAILS; i++) {
            details.add(tr.apply(TIP + "." + i));
        }
        List<String> out = new ArrayList<>();
        out.add(String.format(tr.apply("gregguns.icbm.part.tooltip.tier"), coloredTier));
        out.add(tr.apply(TIP));
        if (shift) {
            out.addAll(details);
        } else {
            out.add(tr.apply("gregguns.icbm.part.tooltip.shift"));
        }
        return out;
    }
}
