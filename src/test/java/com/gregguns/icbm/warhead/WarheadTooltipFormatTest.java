package com.gregguns.icbm.warhead;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class WarheadTooltipFormatTest {

    private static WarheadTooltipFormat.Labels labels() {
        WarheadTooltipFormat.Labels l = new WarheadTooltipFormat.Labels();
        l.explosive = "Explosive: %s";
        l.tier = "Tier: %s";
        l.fill = "Fill: %s / %s";
        l.radius = "Radius: %s";
        l.damage = "Damage: %s";
        l.annihilation = "ANNIHILATES";
        l.shiftHint = "Shift";
        l.detailCap = "Cap %s";
        l.detailCurve = "Curve";
        l.detailFuse = "Fuse %s s";
        l.detailCharge = "Charge";
        l.detailEffect = "Effect";
        return l;
    }

    private static WarheadTooltipFormat.Facts facts() {
        WarheadTooltipFormat.Facts f = new WarheadTooltipFormat.Facts();
        f.explosiveName = "Condensed";
        f.tierText = "HV";
        f.fill = 3;
        f.cap = 5;
        f.radius = 7.6;
        f.centerDamage = 930.4;
        return f;
    }

    @Test
    void theShortTooltipShowsTheFiveFactsAndTheShiftHint() {
        List<String> lines = WarheadTooltipFormat.lines(labels(), facts(), false);
        assertEquals("Explosive: Condensed", lines.get(0));
        assertEquals("Tier: HV", lines.get(1));
        assertEquals("Fill: 3 / 5", lines.get(2));
        assertEquals("Radius: 7.6", lines.get(3));
        assertEquals("Damage: 930", lines.get(4));
        assertEquals("Shift", lines.get(5));
        assertEquals(6, lines.size());
    }

    @Test
    void shiftShowsTheFuseTime() {
        WarheadTooltipFormat.Facts f = facts();
        f.fuseTicks = 120;
        assertTrue(
            WarheadTooltipFormat.lines(labels(), f, true)
                .contains("Fuse 6 s"));
        assertEquals("0.5", WarheadTooltipFormat.fuseSeconds(10));
    }

    @Test
    void shiftShowsTheDetailsInsteadOfTheHint() {
        List<String> lines = WarheadTooltipFormat.lines(labels(), facts(), true);
        assertFalse(lines.contains("Shift"));
        assertTrue(lines.contains("Cap 5") && lines.contains("Curve") && lines.contains("Effect"));
        assertEquals("Charge", lines.get(lines.size() - 1));
    }

    @Test
    void annihilationReplacesTheDamageLine() {
        WarheadTooltipFormat.Facts f = facts();
        f.annihilates = true;
        List<String> lines = WarheadTooltipFormat.lines(labels(), f, false);
        assertTrue(lines.contains("ANNIHILATES"));
        assertFalse(lines.contains("Damage: 930"));
    }

    @Test
    void aTypeWithoutCenterDamageShowsNoDamageLine() {
        WarheadTooltipFormat.Facts f = facts();
        f.centerDamage = 0;
        for (String line : WarheadTooltipFormat.lines(labels(), f, false)) {
            assertFalse(line.startsWith("Damage"));
        }
    }

    @Test
    void numbersUseThousandsSeparatorsWhateverTheLocale() {
        assertEquals("1,200", WarheadTooltipFormat.number(1200.0));
        assertEquals("12", WarheadTooltipFormat.number(11.6));
        assertEquals("9.0", WarheadTooltipFormat.radius(9.0));
    }
}
