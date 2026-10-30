package com.gregguns.icbm.warhead;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.gregguns.icbm.recipe.Ing;

/**
 * De-arming (D283): the grid unload mirrors the grid load, the Cutting Machine's outputs fit its slots, and a full
 * build then de-arm round trip never nets an item. The round trip runs as a ledger over the same recipe tables the
 * game registers ({@link InertWarheadSpec}, {@link WarheadRecipeSpec}) and the same unload rules the crafting recipe
 * uses, so a change to either that would mint material fails here.
 */
class WarheadDearmingTest {

    private static final String CHARGE = "part:PLASTIC_EXPLOSIVE";

    @Test
    void aGridUnloadTakesOutUpToEightLikeALoadPutsIn() {
        assertEquals(WarheadCharging.MAX_PER_CRAFT, WarheadDearming.MAX_PER_CRAFT);
        assertEquals(1, WarheadDearming.unloaded(1));
        assertEquals(5, WarheadDearming.unloaded(5));
        assertEquals(8, WarheadDearming.unloaded(8));
        assertEquals(8, WarheadDearming.unloaded(128));
        assertEquals(0, WarheadDearming.unloaded(0));
        assertEquals(0, WarheadDearming.unloaded(-3));
    }

    @Test
    void aPartialUnloadLeavesTheRestLoaded() {
        assertEquals(12, WarheadDearming.fillLeft(20));
        assertEquals(4, WarheadDearming.fillLeft(12));
        assertEquals(0, WarheadDearming.fillLeft(4));
        assertEquals(0, WarheadDearming.fillLeft(0));
        assertEquals(3, WarheadDearming.craftsToEmpty(20));
        assertEquals(16, WarheadDearming.craftsToEmpty(128));
        assertEquals(1, WarheadDearming.craftsToEmpty(5));
        assertEquals(0, WarheadDearming.craftsToEmpty(0));
    }

    @Test
    void theEmptiedCasingKeepsItsPayloadAndCondensedHasNone() {
        assertEquals("shrapnel", WarheadDearming.payloadOf("shrapnel"));
        assertEquals("redmatter", WarheadDearming.payloadOf("redmatter"));
        assertNull(WarheadDearming.payloadOf(WarheadData.DEFAULT_EXPLOSIVE));
        assertNull(WarheadDearming.payloadOf(null));
        assertNull(WarheadDearming.payloadOf(""));
        // The payload the empty casing keeps is the one its charges loaded into: explosiveFor inverts payloadOf.
        for (WarheadRecipeSpec spec : WarheadRecipeSpec.values()) {
            String explosive = spec.blast()
                .id();
            assertEquals(
                explosive,
                WarheadCharging.explosiveFor(spec.casing(), WarheadDearming.payloadOf(explosive)),
                spec + ": reloading the emptied casing makes the same blast");
        }
    }

    @Test
    void chargesComeBackInStacksOfAtMost64() {
        assertEquals(Arrays.asList(5), WarheadDearming.chargeStacks(5));
        assertEquals(Arrays.asList(64), WarheadDearming.chargeStacks(64));
        assertEquals(Arrays.asList(64, 64), WarheadDearming.chargeStacks(128));
        assertEquals(Arrays.asList(64, 1), WarheadDearming.chargeStacks(65));
        assertTrue(
            WarheadDearming.chargeStacks(0)
                .isEmpty());
    }

    @Test
    void everyCuttingMachineStepFitsItsFourOutputSlots() {
        for (WarheadRecipeSpec spec : WarheadRecipeSpec.values()) {
            assertTrue(
                WarheadDearming.unloadOutputs(spec) <= WarheadDearming.CUTTER_ITEM_OUTPUTS,
                spec + " unload outputs");
            if (!spec.chargesOnly()) {
                assertTrue(
                    WarheadDearming.payloadOutputs(spec) <= WarheadDearming.CUTTER_ITEM_OUTPUTS,
                    spec + " payload outputs");
            }
            for (Ing in : WarheadDearming.payloadReturns(spec)) {
                assertTrue(in.count() <= WarheadDearming.MAX_STACK, spec + " " + in.id() + " fits one slot");
            }
        }
        for (InertWarheadSpec spec : InertWarheadSpec.values()) {
            assertTrue(WarheadDearming.casingReturns(spec)
                .size() <= WarheadDearming.CUTTER_ITEM_OUTPUTS, spec + " scrap outputs");
        }
    }

    @Test
    void thePayloadGivesBackItsItemsAndSpendsOnlyItsFluid() {
        for (WarheadRecipeSpec spec : WarheadRecipeSpec.values()) {
            List<Ing> back = WarheadDearming.payloadReturns(spec);
            List<Ing> lost = WarheadDearming.payloadLosses(spec);
            assertEquals(
                spec.inputs()
                    .size(),
                back.size() + lost.size(),
                spec + ": every ingredient is either returned or a fluid");
            for (Ing in : back) {
                assertFalse(in.isFluid(), spec + " returns no fluid");
            }
            for (Ing in : lost) {
                assertTrue(in.isFluid(), spec + " loses only fluid");
            }
        }
        assertEquals(1, WarheadDearming.payloadReturns(WarheadRecipeSpec.SHRAPNEL)
            .size());
        assertEquals(1, WarheadDearming.payloadLosses(WarheadRecipeSpec.THERMOBARIC)
            .size());
    }

    /** Adds a recipe's item ingredients to a ledger: positive for what the build took, negative for what came back. */
    private static void book(Map<String, Integer> ledger, List<Ing> ings, int sign) {
        for (Ing in : ings) {
            if (!in.isFluid()) {
                ledger.merge(in.src() + ":" + in.id(), sign * in.count(), Integer::sum);
            }
        }
    }

    /** Runs a build of the given grid loads, then grid unloads to empty, then the cutter; returns the net ledger. */
    private static Map<String, Integer> roundTrip(WarheadRecipeSpec spec, int[] loads) {
        Map<String, Integer> ledger = new HashMap<>();
        InertWarheadSpec casing = InertWarheadSpec.of(spec.casing());
        // Build: the inert warhead, the payload, then each grid load (refused loads take nothing).
        book(ledger, casing.inputs(), 1);
        book(ledger, spec.inputs(), 1);
        int cap = spec.blast()
            .fillCap();
        int fill = 0;
        for (int n : loads) {
            int after = WarheadCharging.loadedFill(fill, n, cap);
            if (after > 0) {
                ledger.merge(CHARGE, n, Integer::sum);
                fill = after;
            }
        }
        // De-arm: grid unloads until the casing is inert, then the payload and the casing in the cutter.
        int guard = 0;
        while (fill > 0 && guard++ < 100) {
            ledger.merge(CHARGE, -WarheadDearming.unloaded(fill), Integer::sum);
            fill = WarheadDearming.fillLeft(fill);
        }
        assertEquals(0, fill, spec + ": the grid empties the warhead");
        book(ledger, WarheadDearming.payloadReturns(spec), -1);
        book(ledger, WarheadDearming.casingReturns(casing), -1);
        return ledger;
    }

    @Test
    void aFullBuildAndDearmRoundTripNetsNothing() {
        for (WarheadRecipeSpec spec : WarheadRecipeSpec.values()) {
            int cap = spec.blast()
                .fillCap();
            int[] loads = new int[(cap + 7) / 8];
            for (int i = 0, left = cap; i < loads.length; i++, left -= 8) {
                loads[i] = Math.min(8, left);
            }
            Map<String, Integer> net = roundTrip(spec, loads);
            for (Map.Entry<String, Integer> e : net.entrySet()) {
                assertEquals(0, e.getValue()
                    .intValue(), spec + ": " + e.getKey() + " nets to nothing");
            }
        }
    }

    @Test
    void randomPartialLoadsNeverDupeCharges() {
        Random random = new Random(283);
        for (int trial = 0; trial < 500; trial++) {
            WarheadRecipeSpec spec = WarheadRecipeSpec.values()[random.nextInt(WarheadRecipeSpec.values().length)];
            int[] loads = new int[1 + random.nextInt(20)];
            for (int i = 0; i < loads.length; i++) {
                loads[i] = 1 + random.nextInt(8);
            }
            Map<String, Integer> net = roundTrip(spec, loads);
            for (Map.Entry<String, Integer> e : net.entrySet()) {
                assertTrue(e.getValue() >= 0, spec + " " + Arrays.toString(loads) + ": " + e.getKey() + " duped");
                assertEquals(0, e.getValue()
                    .intValue(), spec + " " + Arrays.toString(loads) + ": " + e.getKey() + " lost");
            }
        }
    }
}
