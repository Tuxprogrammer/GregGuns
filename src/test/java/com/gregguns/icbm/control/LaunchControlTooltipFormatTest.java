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

    @Test
    void wrapAllSplitsLongLinesAndKeepsOrder() {
        LaunchControlTooltipFormat.Wrapper halves = line -> line.length() > 4
            ? Arrays.asList(line.substring(0, 4), line.substring(4))
            : Arrays.asList(line);
        assertEquals(
            Arrays.asList("ab", "abcd", "ef", "x"),
            LaunchControlTooltipFormat.wrapAll(Arrays.asList("ab", "abcdef", "x"), halves));
    }

    @Test
    void wrapAllKeepsALineTheWrapperReturnsNothingFor() {
        assertEquals(
            Arrays.asList("", "a"),
            LaunchControlTooltipFormat.wrapAll(
                Arrays.asList("", "a"),
                line -> line.isEmpty() ? Arrays.<String>asList() : Arrays.asList(line)));
    }
}
