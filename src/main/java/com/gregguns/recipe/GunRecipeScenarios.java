package com.gregguns.recipe;

import net.minecraft.item.ItemStack;

import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;

import gregtech.api.enums.GTValues;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.api.util.GTUtility;

/**
 * In-game scenario for the pistol's final assembly recipe: it is in GT's assembler map ({@code
 * RecipeMaps.assemblerRecipes}, which GT's NEI handler lists), matches its sheet inputs at LV, and is refused below
 * LV. Dev environment only, registered from {@code GunsModule}. Server side.
 */
// Spec: docs/gregguns-spec/02-guns/recipes.md (LV table, 9mm Pistol)
public final class GunRecipeScenarios {

    private GunRecipeScenarios() {}

    /**
     * Registers the scenario.
     *
     * @param registry where to add it
     */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new PistolRecipe());
    }

    private static ItemStack modStack(String name, int count) {
        return ProductionRecipes.modStack(name, count);
    }

    private static ItemStack gt(String prefix, String material, int count) {
        return GTOreDictUnificator.get(OrePrefixes.getPrefix(prefix), Materials.get(material), count);
    }

    private static final class PistolRecipe implements Scenario {

        @Override
        public String name() {
            return "gun.recipe";
        }

        @Override
        public String description() {
            return "The 9mm Pistol is in GT's assembler map (so NEI lists it) at LV, 15 s, matching its sheet inputs, and not below LV.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            ItemStack barrel = modStack("part_barrel_hardened", 1);
            ItemStack mechanical = modStack("part_mechanical_2", 1);
            ItemStack[] inputs = { barrel, mechanical, gt("plate", "Steel", 2), gt("plate", "Rubber", 2),
                gt("screw", "Steel", 4), gt("springSmall", "Steel", 1), GTUtility.getIntegratedCircuit(1) };
            for (ItemStack input : inputs) {
                out.check(input != null, "every sheet input should resolve");
            }
            ItemStack pistol = modStack(GunTable.PISTOL.output(), 1);

            boolean listed = false;
            for (GTRecipe recipe : RecipeMaps.assemblerRecipes.getAllRecipes()) {
                if (recipe.mOutputs != null && recipe.mOutputs.length > 0
                    && recipe.mOutputs[0] != null
                    && pistol != null
                    && recipe.mOutputs[0].getItem() == pistol.getItem()) {
                    listed = true;
                    out.checkEquals("EU/t", (int) TierEU.RECIPE_LV, recipe.mEUt);
                    out.checkEquals("duration (ticks)", 15 * GTRecipeBuilder.SECONDS, recipe.mDuration);
                }
            }
            out.check(listed, "RecipeMaps.assemblerRecipes should hold the pistol recipe (NEI lists that map)");

            GTRecipe found = RecipeMaps.assemblerRecipes.findRecipeQuery()
                .items(inputs)
                .voltage(GTValues.V[1])
                .find();
            out.check(found != null, "an LV assembler with the sheet inputs and circuit should find the recipe");
            GTRecipe tooLow = RecipeMaps.assemblerRecipes.findRecipeQuery()
                .items(inputs)
                .voltage(GTValues.V[0] / 2)
                .find();
            out.check(tooLow == null, "a machine below LV voltage should not be offered the recipe");
            return out.build();
        }
    }
}
