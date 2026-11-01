package com.gregguns.icbm.defense;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

class RadarCasingTooltipFormatTest {

    /** Echoes the key, and gives the tier line its format slot. */
    private static final Function<String, String> TR = k -> "gregguns.icbm.part.tooltip.tier".equals(k) ? "Tier: %s"
        : k;

    @Test
    void withoutShiftItShowsTierSummaryAndHint() {
        assertEquals(
            Arrays.asList("Tier: HV", RadarCasingTooltipFormat.TIP, "gregguns.icbm.part.tooltip.shift"),
            RadarCasingTooltipFormat.lines(TR, "HV", false));
    }

    @Test
    void withShiftItShowsBothDetailLinesInsteadOfTheHint() {
        assertEquals(
            Arrays.asList(
                "Tier: HV",
                RadarCasingTooltipFormat.TIP,
                RadarCasingTooltipFormat.TIP + ".1",
                RadarCasingTooltipFormat.TIP + ".2"),
            RadarCasingTooltipFormat.lines(TR, "HV", true));
    }
}
