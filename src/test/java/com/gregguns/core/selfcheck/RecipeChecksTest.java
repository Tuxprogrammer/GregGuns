package com.gregguns.core.selfcheck;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.gregguns.api.Tier;

class RecipeChecksTest {

    private static StackDescriptor gg(String name, Tier tier) {
        return StackDescriptor.item("gregguns:" + name, true, true, tier);
    }

    private static StackDescriptor part(String name) {
        return StackDescriptor.item("gregtech:" + name, true, false, null);
    }

    private static StackDescriptor missingItem(boolean gregGuns) {
        return StackDescriptor.item("null", false, gregGuns, null);
    }

    private static RecipeDescriptor recipe(long eut, boolean shaped, List<StackDescriptor> in,
        List<StackDescriptor> out) {
        return new RecipeDescriptor("test", eut, shaped, in, out);
    }

    @Test
    void cleanMachineRecipePasses() {
        RecipeDescriptor r = recipe(
            30,
            false,
            Arrays.asList(part("plate"), gg("barrel", Tier.LV)),
            Arrays.asList(gg("pistol", Tier.LV)));
        assertTrue(
            RecipeChecks.check(r)
                .isEmpty());
    }

    @Test
    void nullInputIsReported() {
        RecipeDescriptor r = recipe(30, false, Arrays.asList(missingItem(false)), Arrays.asList(gg("pistol", Tier.LV)));
        List<String> violations = RecipeChecks.check(r);
        assertEquals(1, violations.size());
        assertTrue(
            violations.get(0)
                .contains("null or empty"));
    }

    @Test
    void emptyOreDictInputIsReported() {
        RecipeDescriptor r = recipe(
            0,
            true,
            Arrays.asList(StackDescriptor.oreDict("list of 0", false)),
            Arrays.asList(gg("pistol", Tier.LV)));
        assertTrue(
            RecipeChecks.check(r)
                .get(0)
                .contains("ore-dict"));
    }

    @Test
    void unresolvedFluidIsReported() {
        RecipeDescriptor r = recipe(
            30,
            false,
            Arrays.asList(StackDescriptor.fluid("null", false)),
            Arrays.asList(gg("pistol", Tier.LV)));
        assertTrue(
            RecipeChecks.check(r)
                .get(0)
                .contains("fluid"));
    }

    @Test
    void emptySlotIsFineInAShapedGridButNotInAShapelessRecipe() {
        List<StackDescriptor> in = Arrays.asList(StackDescriptor.emptySlot(), part("plate"));
        List<StackDescriptor> out = Arrays.asList(gg("knife", Tier.STEAM));
        assertTrue(
            RecipeChecks.check(recipe(0, true, in, out))
                .isEmpty());
        assertEquals(
            1,
            RecipeChecks.check(recipe(0, false, in, out))
                .size());
    }

    @Test
    void nullOutputIsReported() {
        RecipeDescriptor r = recipe(
            30,
            false,
            Collections.singletonList(part("plate")),
            Arrays.asList(missingItem(true)));
        assertEquals(
            1,
            RecipeChecks.check(r)
                .size());
    }

    @Test
    void machineAboveOutputTierIsReported() {
        RecipeDescriptor r = recipe(
            120,
            false,
            Collections.singletonList(part("plate")),
            Arrays.asList(gg("pistol", Tier.LV)));
        assertTrue(
            RecipeChecks.check(r)
                .get(0)
                .contains("MV"));
    }

    @Test
    void machineExactlyAtOutputTierIsFineAndOneAboveIsNot() {
        List<StackDescriptor> in = Collections.singletonList(part("plate"));
        List<StackDescriptor> out = Arrays.asList(gg("pistol", Tier.LV));
        assertTrue(
            RecipeChecks.check(recipe(32, false, in, out))
                .isEmpty());
        assertEquals(
            1,
            RecipeChecks.check(recipe(33, false, in, out))
                .size());
    }

    @Test
    void inputAboveOutputTierIsReported() {
        RecipeDescriptor r = recipe(
            0,
            false,
            Arrays.asList(gg("receiver", Tier.HV)),
            Arrays.asList(gg("pistol", Tier.LV)));
        assertTrue(
            RecipeChecks.check(r)
                .get(0)
                .contains("HV input"));
    }

    @Test
    void gregGunsOutputWithoutTierIsReported() {
        RecipeDescriptor r = recipe(
            30,
            false,
            Collections.singletonList(part("plate")),
            Arrays.asList(gg("pistol", null)));
        assertTrue(
            RecipeChecks.check(r)
                .get(0)
                .contains("no tier"));
    }

    @Test
    void nonGregGunsOutputsAreNotTierChecked() {
        RecipeDescriptor r = recipe(30720, false, Arrays.asList(gg("scrap", Tier.EV)), Arrays.asList(part("dust")));
        assertTrue(
            RecipeChecks.check(r)
                .isEmpty());
    }

    @Test
    void collectsAllViolationsOfOneRecipe() {
        RecipeDescriptor r = recipe(
            480,
            false,
            Arrays.asList(missingItem(false), gg("receiver", Tier.EV)),
            Arrays.asList(gg("pistol", Tier.LV)));
        // null input, machine above, input above
        assertEquals(
            3,
            RecipeChecks.check(r)
                .size());
    }
}
