package com.gregguns.icbm.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.gregguns.api.Tier;
import com.gregguns.icbm.recipe.Ing;

class HandheldRulesTest {

    @Test
    void rangeGrowsWithTier() {
        assertEquals(128, HandheldRules.range(Tier.HV));
        assertEquals(256, HandheldRules.range(Tier.EV));
        assertEquals(512, HandheldRules.range(Tier.IV));
        assertEquals(0, HandheldRules.range(Tier.LV));
    }

    @Test
    void rangeIsASphereWithItsEdgeIncluded() {
        assertTrue(HandheldRules.inRange(128, 0, 0, 128));
        assertFalse(HandheldRules.inRange(129, 0, 0, 128));
        assertTrue(HandheldRules.inRange(60, 60, 60, 128));
        assertFalse(HandheldRules.inRange(80, 80, 80, 128));
        assertFalse(HandheldRules.inRange(0, 0, 0, 0));
    }

    @Test
    void onlyTheOwnerOrCreativeControlsAnOwnedWarhead() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        assertTrue(HandheldRules.mayControl(a, a, false));
        assertFalse(HandheldRules.mayControl(a, b, false));
        assertTrue(HandheldRules.mayControl(a, b, true));
        assertTrue(HandheldRules.mayControl(null, b, false));
    }

    @Test
    void tooltipHidesNumbersWithoutShift() {
        assertEquals(
            Arrays.asList("tier", "summary", "hint"),
            HandheldTooltipFormat.lines("tier", "summary", Arrays.asList("d1", "d2"), "hint", false));
        assertEquals(
            Arrays.asList("tier", "summary", "d1", "d2"),
            HandheldTooltipFormat.lines("tier", "summary", Arrays.asList("d1", "d2"), "hint", true));
    }

    @Test
    void noToolRecipeRunsBelowHvOrExceedsTheAssemblerSlots() {
        // D69: nothing ICBM runs below HV; the Assembler has nine item slots and the circuit takes one of them.
        for (ToolRecipeSpec s : ToolRecipeSpec.values()) {
            assertTrue(
                s.machineTier()
                    .voltageIndex() >= Tier.HV.voltageIndex(),
                s + " is below HV");
            assertTrue(
                s.inputs()
                    .size() + 1 <= 9,
                s + " has too many inputs");
            for (Ing in : s.inputs()) {
                assertTrue(
                    in.tier()
                        .voltageIndex()
                        <= s.machineTier()
                            .voltageIndex()
                        || in.src() != Ing.Src.GT_ITEM,
                    s + " uses an input above its machine tier: " + in.id());
            }
        }
    }

    @Test
    void everyDetonatorTierHasOneRecipeAndTheDefuserHasOne() {
        int detonators = 0;
        for (ToolRecipeSpec s : ToolRecipeSpec.values()) {
            if (s.detonatorTier() != null) {
                detonators++;
                assertTrue(HandheldRules.range(s.detonatorTier()) > 0);
            }
        }
        assertEquals(3, detonators);
        assertEquals(32, HandheldRules.DEFUSER_USES);
    }

    /** Regression guard for I4 review item 1: a true here swallows the click before the server sees it. */
    @Test
    void clientNeverConsumesTheUseClick() {
        assertEquals(false, HandheldRules.clientConsumesUse());
    }

    @Test
    void programmedCircuitsDiffer() {
        java.util.Set<Integer> seen = new java.util.HashSet<>();
        for (ToolRecipeSpec s : ToolRecipeSpec.values()) {
            assertTrue(seen.add(s.circuit()), s + " repeats a circuit number");
        }
    }
}
