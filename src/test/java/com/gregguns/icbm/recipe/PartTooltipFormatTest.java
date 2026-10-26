package com.gregguns.icbm.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

class PartTooltipFormatTest {

    @Test
    void withoutShiftItShowsTheHint() {
        assertEquals(
            Arrays.asList("Tier: HV", "A charge.", "[SHIFT]"),
            PartTooltipFormat.lines("Tier: HV", "A charge.", "Used in: x", "[SHIFT]", false));
    }

    @Test
    void withShiftItShowsWhereItIsUsed() {
        assertEquals(
            Arrays.asList("Tier: HV", "A charge.", "Used in: x"),
            PartTooltipFormat.lines("Tier: HV", "A charge.", "Used in: x", "[SHIFT]", true));
    }
}
