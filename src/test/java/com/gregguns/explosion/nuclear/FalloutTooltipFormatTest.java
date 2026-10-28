package com.gregguns.explosion.nuclear;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

class FalloutTooltipFormatTest {

    private static final List<String> DETAILS = Arrays.asList("d1", "d2");

    @Test
    void withoutShiftItShowsTierSummaryAndHint() {
        assertEquals(
            Arrays.asList("tier", "summary", "hint"),
            FalloutTooltipFormat.lines("tier", "summary", DETAILS, "hint", false));
    }

    @Test
    void withShiftItShowsTheDetailsInsteadOfTheHint() {
        assertEquals(
            Arrays.asList("tier", "summary", "d1", "d2"),
            FalloutTooltipFormat.lines("tier", "summary", DETAILS, "hint", true));
    }

    @Test
    void noDetailsMeansNothingBehindShift() {
        assertEquals(
            Arrays.asList("tier", "summary"),
            FalloutTooltipFormat.lines("tier", "summary", Arrays.<String>asList(), "hint", true));
    }
}
