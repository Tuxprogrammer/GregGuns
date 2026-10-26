package com.gregguns.icbm.control;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

class LaunchControlTooltipFormatTest {

    @Test
    void withoutShiftTheSummaryHintAndAddedBy() {
        assertEquals(
            Arrays.asList("Fires it", "[SHIFT]", "Added by"),
            LaunchControlTooltipFormat.primary("Fires it", "[SHIFT]", "Added by"));
    }

    @Test
    void withShiftTheSummaryEveryDetailAndAddedByWithoutTheHint() {
        assertEquals(
            Arrays.asList("Fires it", "reach", "range", "Added by"),
            LaunchControlTooltipFormat.secondary("Fires it", Arrays.asList("reach", "range"), "Added by"));
    }
}
