package com.gregguns.core.selfcheck;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.gregguns.api.Tier;

import gregtech.api.enums.TierEU;

class RecipeTierCheckTest {

    private static final List<Tier> NO_INPUTS = Collections.emptyList();

    @Test
    void recipeAtExactlyTheOutputVoltageIsFine() {
        assertTrue(
            RecipeTierCheck.check(Tier.LV, Tier.LV.voltage(), NO_INPUTS)
                .isEmpty());
        assertTrue(
            RecipeTierCheck.check(Tier.LV, TierEU.RECIPE_LV, NO_INPUTS)
                .isEmpty());
    }

    @Test
    void oneEuAboveTheOutputVoltageNeedsTheNextTier() {
        List<RecipeTierCheck.Violation> violations = RecipeTierCheck.check(Tier.LV, Tier.LV.voltage() + 1, NO_INPUTS);
        assertEquals(1, violations.size());
        assertEquals(
            RecipeTierCheck.Kind.MACHINE_ABOVE_OUTPUT,
            violations.get(0)
                .kind());
        assertTrue(
            violations.get(0)
                .message()
                .contains("MV"));
    }

    @Test
    void lowerVoltageMachinesAreFine() {
        assertTrue(
            RecipeTierCheck.check(Tier.HV, TierEU.RECIPE_LV, NO_INPUTS)
                .isEmpty());
    }

    @Test
    void steamOutputsRunOnlyAtUlvVoltage() {
        assertTrue(
            RecipeTierCheck.check(Tier.STEAM, TierEU.RECIPE_ULV, NO_INPUTS)
                .isEmpty());
        assertTrue(
            RecipeTierCheck.check(Tier.STEAM, Tier.STEAM.voltage(), NO_INPUTS)
                .isEmpty());
        assertEquals(
            RecipeTierCheck.Kind.MACHINE_ABOVE_OUTPUT,
            RecipeTierCheck.check(Tier.STEAM, TierEU.RECIPE_LV, NO_INPUTS)
                .get(0)
                .kind());
    }

    @Test
    void craftingTableRecipesNeedNoMachine() {
        assertTrue(
            RecipeTierCheck.check(Tier.STEAM, 0, NO_INPUTS)
                .isEmpty());
        assertTrue(
            RecipeTierCheck.check(Tier.LV, 0, Arrays.asList(Tier.STEAM, Tier.LV))
                .isEmpty());
    }

    @Test
    void craftingTableStillChecksInputTiers() {
        List<RecipeTierCheck.Violation> violations = RecipeTierCheck.check(Tier.STEAM, 0, Arrays.asList(Tier.LV));
        assertEquals(1, violations.size());
        assertEquals(
            RecipeTierCheck.Kind.INPUT_ABOVE_OUTPUT,
            violations.get(0)
                .kind());
    }

    @Test
    void inputOfTheSameTierIsFineAndUnknownInputsAreSkipped() {
        assertTrue(
            RecipeTierCheck.check(Tier.MV, TierEU.RECIPE_MV, Arrays.asList(Tier.MV, null, Tier.LV))
                .isEmpty());
    }

    @Test
    void reportsEveryBreachNotJustTheFirst() {
        List<RecipeTierCheck.Violation> violations = RecipeTierCheck
            .check(Tier.LV, TierEU.RECIPE_HV, Arrays.asList(Tier.HV, Tier.EV));
        assertEquals(3, violations.size());
    }

    @Test
    void voltageAboveZpmIsAboveEveryOutput() {
        List<RecipeTierCheck.Violation> violations = RecipeTierCheck.check(Tier.ZPM, Tier.ZPM.voltage() + 1, NO_INPUTS);
        assertEquals(1, violations.size());
        assertEquals(
            RecipeTierCheck.Kind.MACHINE_ABOVE_OUTPUT,
            violations.get(0)
                .kind());
    }
}
