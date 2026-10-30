package com.gregguns.icbm.warhead;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.gregguns.GregGuns;
import com.gregguns.icbm.recipe.IcbmIngredients;
import com.gregguns.icbm.recipe.IcbmPart;
import com.gregguns.icbm.recipe.IcbmParts;
import com.gregguns.icbm.recipe.Ing;

import gregtech.api.enums.GTValues;
import gregtech.api.recipe.RecipeMaps;

/**
 * GT's Cutting Machine de-arms a warhead (D283), the Assembler's steps run backwards at the same voltage: it empties a
 * full warhead (the inert warhead with its payload, and every charge), takes a payload out (the bare inert warhead and
 * the payload's items; its fluid is spent) and scraps a bare inert warhead (its plates and Fuze Assembly). Every input
 * is NBT-sensitive, so a bare casing never matches a payload's removal and a partly filled warhead never matches the
 * full one; partial loads go back through the crafting grid ({@link WarheadUnloadRecipe}). GT's cutter adds its water,
 * distilled water, lubricant and superfluid variants. Called from {@link WarheadRecipes} on GT's before-postload hook.
 * Common side.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/warheads-and-fill.md (De-arming)
// Source:
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/recipe/RecipeMaps.java#L1374-L1434
// (cutterRecipes: two item inputs, four item outputs, one fluid input; the emitter adds the cutting fluid variants
// to a recipe given none, and GTRecipeBuilder.copy keeps nbtSensitive).
// Source:
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/postload/recipes/FakeCuttingRecipes.java#L25-L75
// (NEI shows the cutter through its fake map, grouped by inputs hashed with their NBT, so each payload has a row).
public final class WarheadDearmRecipes {

    private WarheadDearmRecipes() {}

    /** Adds every de-arm recipe; a missing ingredient skips that recipe only, as the build recipes do. */
    static void addAll() {
        for (WarheadRecipeSpec spec : WarheadRecipeSpec.values()) {
            try {
                addUnload(spec);
                if (!spec.chargesOnly()) {
                    addPayloadRemoval(spec);
                }
            } catch (RuntimeException e) {
                GregGuns.LOG.error("Warhead de-arm recipe {} failed", spec, e);
            }
        }
        for (InertWarheadSpec spec : InertWarheadSpec.values()) {
            try {
                addScrap(spec);
            } catch (RuntimeException e) {
                GregGuns.LOG.error("Inert warhead scrap recipe {} failed", spec, e);
            }
        }
    }

    /** A full warhead: the inert warhead it was filled from, and the charges in stacks of at most 64. */
    private static void addUnload(WarheadRecipeSpec spec) {
        WarheadBlast blast = spec.blast();
        Item charge = IcbmParts.get(IcbmPart.PLASTIC_EXPLOSIVE);
        List<ItemStack> outputs = new ArrayList<>();
        outputs.add(WarheadRecipes.inertFor(blast));
        for (int size : WarheadDearming.chargeStacks(blast.fillCap())) {
            outputs.add(new ItemStack(charge, size));
        }
        add(WarheadRecipes.warhead(blast, blast.fillCap()), outputs, spec.machineVoltageIndex());
    }

    /** An inert warhead with a payload: the bare inert warhead and the payload's items. */
    private static void addPayloadRemoval(WarheadRecipeSpec spec) {
        List<ItemStack> outputs = new ArrayList<>();
        outputs.add(InertWarheads.stack(spec.casing(), null));
        for (Ing in : WarheadDearming.payloadReturns(spec)) {
            ItemStack stack = IcbmIngredients.stack(in);
            if (stack == null) {
                if (in.isOptional()) {
                    // The payload recipe left it out too, so there is nothing to give back.
                    continue;
                }
                GregGuns.LOG
                    .error("Skipping the {} payload removal: {} is missing", spec, IcbmIngredients.describe(in));
                return;
            }
            outputs.add(stack);
        }
        add(WarheadRecipes.inertFor(spec.blast()), outputs, spec.machineVoltageIndex());
    }

    /** A bare inert warhead: its casing plates and its Fuze Assembly. */
    private static void addScrap(InertWarheadSpec spec) {
        List<ItemStack> outputs = new ArrayList<>();
        for (Ing in : WarheadDearming.casingReturns(spec)) {
            ItemStack stack = IcbmIngredients.stack(in);
            if (stack == null) {
                GregGuns.LOG.error("Skipping the {} scrap recipe: {} is missing", spec, IcbmIngredients.describe(in));
                return;
            }
            outputs.add(stack);
        }
        add(InertWarheads.stack(spec.casing(), null), outputs, spec.voltageIndex());
    }

    private static void add(ItemStack input, List<ItemStack> outputs, int voltageIndex) {
        GTValues.RA.stdBuilder()
            .itemInputs(input)
            .itemOutputs(outputs.toArray(new ItemStack[0]))
            .nbtSensitive()
            .duration(WarheadDearming.CUTTER_TICKS)
            .eut(GTValues.VP[voltageIndex])
            .addTo(RecipeMaps.cutterRecipes);
    }
}
