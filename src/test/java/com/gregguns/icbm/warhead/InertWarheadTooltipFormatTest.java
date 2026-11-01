package com.gregguns.icbm.warhead;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/** The inert warhead's name and tooltip (D41, D282). */
class InertWarheadTooltipFormatTest {

    private static InertWarheadTooltipFormat.Labels labels() {
        InertWarheadTooltipFormat.Labels l = new InertWarheadTooltipFormat.Labels();
        l.payload = "Payload: %s";
        l.payloadNoneT1 = "none (Condensed)";
        l.payloadNone = "none";
        l.tier = "Tier: %s";
        l.charges = "Charges: %s / %s";
        l.inert = "Inert";
        l.shiftHint = "Shift";
        l.nextBareT1 = "NextT1";
        l.nextBare = "NextBare";
        l.nextFitted = "NextFitted";
        l.howToCharge = "Up to %s per craft";
        l.dearmFitted = "DearmFitted";
        l.dearmBare = "DearmBare";
        return l;
    }

    private static InertWarheadTooltipFormat.Facts facts(String payload, boolean t1, int cap) {
        InertWarheadTooltipFormat.Facts f = new InertWarheadTooltipFormat.Facts();
        f.payloadName = payload;
        f.t1 = t1;
        f.tierText = t1 ? "HV" : "EV-IV";
        f.cap = cap;
        return f;
    }

    @Test
    void theNameShowsThePayloadWhenThereIsOne() {
        assertEquals(
            "Inert T1 Warhead",
            InertWarheadTooltipFormat.displayName("Inert T%s Warhead", "Inert %s T%s Warhead", null, 1));
        assertEquals(
            "Inert Shrapnel T1 Warhead",
            InertWarheadTooltipFormat.displayName("Inert T%s Warhead", "Inert %s T%s Warhead", "Shrapnel", 1));
    }

    @Test
    void theShortTooltipIsPayloadTierChargesAndInert() {
        List<String> lines = InertWarheadTooltipFormat.lines(labels(), facts(null, true, 5), false);
        assertEquals("Payload: none (Condensed)", lines.get(0));
        assertEquals("Tier: HV", lines.get(1));
        assertEquals("Charges: 0 / 5", lines.get(2));
        assertEquals("Inert", lines.get(3));
        assertEquals("Shift", lines.get(4));
        assertEquals(5, lines.size());
    }

    @Test
    void aBareHeavyCasingSaysItNeedsAPayload() {
        List<String> lines = InertWarheadTooltipFormat.lines(labels(), facts(null, false, 20), true);
        assertEquals("Payload: none", lines.get(0));
        assertTrue(lines.contains("NextBare"));
        assertTrue(lines.contains("Up to 8 per craft"));
    }

    @Test
    void aSmallCasingNeverOffersMoreChargesPerCraftThanItHolds() {
        // The T1 casing holds five: a load of six to eight makes no recipe, so the tooltip must not offer it.
        List<String> lines = InertWarheadTooltipFormat.lines(labels(), facts(null, true, 5), true);
        assertTrue(lines.contains("Up to 5 per craft"));
        assertFalse(lines.contains("Up to 8 per craft"));
    }

    @Test
    void aFittedCasingSaysToLoadCharges() {
        List<String> lines = InertWarheadTooltipFormat.lines(labels(), facts("Thermobaric", false, 20), true);
        assertEquals("Payload: Thermobaric", lines.get(0));
        assertTrue(lines.contains("NextFitted"));
        assertEquals("DearmFitted", lines.get(lines.size() - 1));
        assertFalse(lines.contains("DearmBare"));
    }

    @Test
    void aBareCasingSaysHowToScrapIt() {
        List<String> lines = InertWarheadTooltipFormat.lines(labels(), facts(null, true, 5), true);
        assertEquals("DearmBare", lines.get(lines.size() - 1));
        assertFalse(
            InertWarheadTooltipFormat.lines(labels(), facts(null, true, 5), false)
                .contains("DearmBare"));
    }
}
