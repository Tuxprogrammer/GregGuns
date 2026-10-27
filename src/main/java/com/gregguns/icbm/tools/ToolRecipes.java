package com.gregguns.icbm.tools;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;

import com.gregguns.GregGuns;
import com.gregguns.icbm.recipe.IcbmIngredients;
import com.gregguns.icbm.recipe.Ing;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.objects.ItemData;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;

/**
 * GT recipes and recycling data of the remote detonators and the defuser (D69, D122): one Assembler recipe each at its
 * tier's voltage, built from {@link ToolRecipeSpec}; the plates come back from recycling, the electronics do not.
 * Queued for GT's before-postload hook. Common side.
 */
// Spec: docs/gregguns-icbm-spec/05-detection-defense/handheld-tools.md (Recipes)
public final class ToolRecipes {

    private static final int DURATION_TICKS = 300;

    private ToolRecipes() {}

    /** Queues the recipes and the recycling data. */
    public static void init() {
        GregTechAPI.sBeforeGTPostload.add(ToolRecipes::addAll);
    }

    /** @return the stack a recipe makes */
    static ItemStack output(ToolRecipeSpec spec) {
        return spec == ToolRecipeSpec.DEFUSER ? ToolRegistration.defuserStack()
            : ToolRegistration.detonatorStack(spec.detonatorTier());
    }

    private static void addAll() {
        for (ToolRecipeSpec spec : ToolRecipeSpec.values()) {
            try {
                add(spec);
            } catch (RuntimeException e) {
                GregGuns.LOG.error("Handheld tool recipe {} failed", spec, e);
            }
        }
        addRecycling();
    }

    private static void add(ToolRecipeSpec spec) {
        List<ItemStack> items = new ArrayList<>();
        for (Ing in : spec.inputs()) {
            ItemStack stack = IcbmIngredients.stack(in);
            if (stack == null) {
                GregGuns.LOG.error("Skipping the {} recipe: {} is missing", spec, IcbmIngredients.describe(in));
                return;
            }
            items.add(stack);
        }
        GTValues.RA.stdBuilder()
            .circuit(spec.circuit())
            .itemInputs(items.toArray(new ItemStack[0]))
            .itemOutputs(output(spec))
            .duration(DURATION_TICKS)
            .eut(
                GTValues.VP[spec.machineTier()
                    .voltageIndex()])
            .addTo(RecipeMaps.assemblerRecipes);
    }

    /** GT recycling data: the shell plates come back, the electronics burn away (D44). */
    private static void addRecycling() {
        long plate = OrePrefixes.plate.getMaterialAmount();
        attach(ToolRegistration.detonatorStack(com.gregguns.api.Tier.HV), Materials.StainlessSteel, 2 * plate);
        attach(ToolRegistration.detonatorStack(com.gregguns.api.Tier.EV), Materials.Titanium, 2 * plate);
        attach(ToolRegistration.detonatorStack(com.gregguns.api.Tier.IV), Materials.TungstenSteel, 2 * plate);
        attach(ToolRegistration.defuserStack(), Materials.StainlessSteel, 2 * plate);
    }

    private static void attach(ItemStack stack, Materials material, long amount) {
        GTOreDictUnificator.addItemData(stack, new ItemData(material, amount));
    }
}
