package com.gregguns.worldgen.spawner;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

/** D41 layout of the spawner tooltip. */
class HoleTooltipFormatTest {

    @Test
    void withoutShiftItShowsTheHint() {
        assertEquals(
            Arrays.asList("tier", "summary", "hint"),
            HoleTooltipFormat.lines("tier", "summary", Arrays.asList("d1", "d2"), "hint", false));
    }

    @Test
    void withShiftItShowsTheDetails() {
        assertEquals(
            Arrays.asList("tier", "summary", "d1", "d2"),
            HoleTooltipFormat.lines("tier", "summary", Arrays.asList("d1", "d2"), "hint", true));
    }
}
