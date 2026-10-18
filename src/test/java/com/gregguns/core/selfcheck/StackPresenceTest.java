package com.gregguns.core.selfcheck;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.gregguns.api.Tier;

class StackPresenceTest {

    @Test
    void sizeZeroIsAValidNonConsumedGtInput() {
        // GT: a stack size of 0 is "required but not consumed" (programmed circuits, molds).
        assertTrue(StackPresence.isPresent(true, 0, true));
    }

    @Test
    void sizeZeroIsStillEmptyForCraftingAndOutputs() {
        assertFalse(StackPresence.isPresent(true, 0, false));
    }

    @Test
    void positiveSizesAreAlwaysPresent() {
        assertTrue(StackPresence.isPresent(true, 1, false));
        assertTrue(StackPresence.isPresent(true, 64, true));
    }

    @Test
    void aMissingItemIsNeverPresent() {
        assertFalse(StackPresence.isPresent(false, 1, true));
        assertFalse(StackPresence.isPresent(false, 0, true));
    }

    @Test
    void aNegativeSizeIsAHoleEvenForGtInputs() {
        assertFalse(StackPresence.isPresent(true, -1, true));
    }

    @Test
    void aGtRecipeWithACircuitInputPassesTheRecipeChecks() {
        StackDescriptor circuit = StackDescriptor
            .item("gregtech:gt.integrated_circuit", StackPresence.isPresent(true, 0, true), false, null);
        StackDescriptor output = StackDescriptor.item("gregguns:part_mechanical_2", true, true, Tier.LV);
        RecipeDescriptor recipe = new RecipeDescriptor(
            "gt:assembler",
            30,
            false,
            Arrays.asList(circuit),
            Arrays.asList(output));
        assertTrue(
            RecipeChecks.check(recipe)
                .isEmpty());
    }
}
