package com.gregguns.icbm.launcher;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

class LauncherRulesTest {

    @Test
    void firstLoadGivesOneItemsCharges() {
        assertEquals(4, LauncherRules.combine(null, 0, "he"));
        assertEquals(4, LauncherRules.combine("he", 0, "he"));
    }

    @Test
    void sameTypeAddsAndCaps() {
        assertEquals(8, LauncherRules.combine("he", 4, "he"));
        assertEquals(16, LauncherRules.combine("he", 13, "he"));
        assertEquals(-1, LauncherRules.combine("he", 16, "he"));
    }

    @Test
    void anotherTypeReplaces() {
        assertEquals(4, LauncherRules.combine("he", 12, "emp"));
    }

    @Test
    void shotsSpendOneChargeAndStopAtZero() {
        assertEquals(3, LauncherRules.afterShot(4));
        assertEquals(0, LauncherRules.afterShot(0));
    }

    @Test
    void tooltipHidesDetailsWithoutShift() {
        assertEquals(
            Arrays.asList("t", "s", "hint"),
            LauncherTooltipFormat.lines("t", "s", Arrays.asList("d1", "d2"), "hint", false));
        assertEquals(
            Arrays.asList("t", "s", "d1", "d2"),
            LauncherTooltipFormat.lines("t", "s", Arrays.asList("d1", "d2"), "hint", true));
        assertEquals("rocket launcher, grim reaper", LauncherTooltipFormat.gunList(LauncherKind.ROCKET.guns()));
    }
}
