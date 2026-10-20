package com.gregguns.utility;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;

import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;

import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTRecipe;

/**
 * Scenarios of the utility core: the turret armor plates. Dev environment only. Server side.
 */
// Spec: docs/gregguns-spec/09-machines-utility/recipes.md (GGS-09-26, GGS-09-29, GGS-09-30)
public final class UtilityScenarios {

    private UtilityScenarios() {}

    /**
     * @param registry where to add the scenarios
     */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new PlatesRecipesScenario());
    }

    /** The four plates exist and each has the recipe the sheet gives. */
    private static final class PlatesRecipesScenario implements Scenario {

        @Override
        public String name() {
            return "utility.plates_recipes";
        }

        @Override
        public String description() {
            return "The four turret armor plates are registered with armor values 5/10/15/20 and the right hand or Assembler recipe.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            for (TurretPlate plate : TurretPlate.values()) {
                ItemTurretArmor item = UtilityParts.plate(plate);
                out.check(item != null, plate.registryName() + " should be registered");
                if (item == null) {
                    continue;
                }
                out.checkEquals(plate.registryName() + " armor value", plate.armorValue(), item.armorValue());
                GTRecipe recipe = findAssembler(item);
                if (plate.handMade()) {
                    out.check(hasCraftingRecipe(item), plate.registryName() + " needs a crafting-table recipe");
                    out.check(recipe == null, plate.registryName() + " must not use the Assembler");
                } else {
                    out.check(!hasCraftingRecipe(item), plate.registryName() + " must not be made by hand");
                    out.check(recipe != null, plate.registryName() + " needs an Assembler recipe");
                    if (recipe != null) {
                        out.checkEquals(plate.registryName() + " EU/t", (int) plate.recipeEut(), recipe.mEUt);
                        out.checkEquals(plate.registryName() + " ticks", plate.durationTicks(), recipe.mDuration);
                        // The plates plus the programmed circuit item (a circuit of its own avoids the pack rows
                        // holding the same plates).
                        int expectedInputs = (plate.extraMaterial() == null ? 1 : 2) + 1;
                        out.checkEquals(plate.registryName() + " inputs", expectedInputs, recipe.mInputs.length);
                    }
                }
            }
            return out.build();
        }

        private static boolean hasCraftingRecipe(Item item) {
            for (Object o : CraftingManager.getInstance()
                .getRecipeList()) {
                ItemStack made = ((IRecipe) o).getRecipeOutput();
                if (made != null && made.getItem() == item) {
                    return true;
                }
            }
            return false;
        }

        private static GTRecipe findAssembler(Item item) {
            for (GTRecipe recipe : RecipeMaps.assemblerRecipes.getAllRecipes()) {
                if (recipe.mOutputs != null && recipe.mOutputs.length > 0
                    && recipe.mOutputs[0] != null
                    && recipe.mOutputs[0].getItem() == item) {
                    return recipe;
                }
            }
            return null;
        }
    }
}
