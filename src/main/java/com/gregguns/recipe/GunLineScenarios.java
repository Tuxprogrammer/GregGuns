package com.gregguns.recipe;

import net.minecraft.item.ItemStack;

import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioResult;

import gregtech.api.util.GTRecipe;

/**
 * Scenario factories for the guns built on the Assembly Line ({@link GunLineTable}). Dev environment only. Server
 * side.
 */
// Spec: docs/gregguns-spec/02-guns/recipes.md (LuV and ZPM table)
public final class GunLineScenarios {

    private GunLineScenarios() {}

    /**
     * The recipe scenario of one gun: GT's Assembly Line list holds it with the row's EU/t, time, research item, scan
     * time and voltage, its two fluids, and its output is the gun (ours).
     *
     * @param name        the scenario name, for example {@code gun.nuke_launcher.recipe}
     * @param description what it checks, for the docs
     * @param output      the gun's registry name
     * @return the scenario
     */
    public static Scenario recipe(String name, String description, String output) {
        return new LineRecipe(name, description, output);
    }

    private static final class LineRecipe implements Scenario {

        private final String name;
        private final String description;
        private final String output;

        LineRecipe(String name, String description, String output) {
            this.name = name;
            this.description = description;
            this.output = output;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public String description() {
            return description;
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name);
            ComponentTable.Spec found = null;
            for (ComponentTable.Spec spec : GunLineTable.SPECS) {
                if (spec.row()
                    .output()
                    .equals(output)) {
                    found = spec;
                }
            }
            out.check(found != null, "GunLineTable should hold a row for " + output);
            if (found == null) {
                return out.build();
            }
            ComponentScenarios.ComponentRecipesScenario.checkAssemblyLine(out, found);
            ItemStack made = ProductionRecipes.modStack(output, 1);
            out.check(made != null, output + " should be registered");
            for (GTRecipe.RecipeAssemblyLine candidate : GTRecipe.RecipeAssemblyLine.sAssemblylineRecipes) {
                if (made != null && candidate.mOutput != null && candidate.mOutput.getItem() == made.getItem()) {
                    out.checkEquals(
                        "fluid inputs",
                        found.fluids()
                            .size(),
                        candidate.mFluidInputs == null ? 0 : candidate.mFluidInputs.length);
                    out.checkEquals(
                        "item inputs",
                        found.row()
                            .inputs()
                            .size(),
                        candidate.mInputs == null ? 0 : candidate.mInputs.length);
                }
            }
            return out.build();
        }
    }
}
