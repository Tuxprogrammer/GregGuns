package com.gregguns.icbm.missile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class MissileTooltipFormatTest {

    private static MissileTooltipFormat.Labels labels() {
        MissileTooltipFormat.Labels l = new MissileTooltipFormat.Labels();
        l.tier = "Tier: %s";
        l.fuel = "Fuel: %s mB";
        l.range = "Range: %s to %s";
        l.guidance = "Guidance +%s";
        l.shiftHint = "[SHIFT]";
        l.detailWarhead = "Warhead up to T%s";
        l.detailStack = "Stack";
        l.detailBonus = "Bonus";
        return l;
    }

    private static MissileTooltipFormat.Facts facts(int guidance) {
        MissileTooltipFormat.Facts f = new MissileTooltipFormat.Facts();
        f.tierText = "HV";
        f.fuelMb = 2000;
        f.minRange = 20;
        f.maxRange = 1000;
        f.guidance = guidance;
        f.warheadTier = 1;
        return f;
    }

    @Test
    void withoutShiftTheFactsAndTheHint() {
        List<String> lines = MissileTooltipFormat.lines(labels(), facts(0), false);
        assertEquals(4, lines.size());
        assertEquals("Tier: HV", lines.get(0));
        assertEquals("Fuel: 2000 mB", lines.get(1));
        assertEquals("Range: 20 to 1000", lines.get(2));
        assertEquals("[SHIFT]", lines.get(3));
    }

    @Test
    void guidanceShowsOnlyWhenAboveZero() {
        assertTrue(
            MissileTooltipFormat.lines(labels(), facts(2), false)
                .contains("Guidance +2"));
        assertTrue(
            !MissileTooltipFormat.lines(labels(), facts(0), false)
                .contains("Guidance +0"));
    }

    @Test
    void shiftShowsTheDetailsInsteadOfTheHint() {
        List<String> lines = MissileTooltipFormat.lines(labels(), facts(0), true);
        assertTrue(lines.contains("Warhead up to T1"));
        assertTrue(lines.contains("Stack"));
        assertTrue(lines.contains("Bonus"));
        assertTrue(!lines.contains("[SHIFT]"));
    }
}
