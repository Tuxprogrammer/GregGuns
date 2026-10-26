package com.gregguns.icbm.recipe;

import net.minecraft.item.ItemStack;

import com.gregguns.core.module.ModuleId;
import com.gregguns.core.module.ModuleState;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.icbm.warhead.WarheadBlast;
import com.gregguns.icbm.warhead.WarheadRecipeSpec;
import com.gregguns.icbm.warhead.WarheadStacks;

import gregtech.api.enums.GTValues;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTRecipe;

/**
 * Checks that every ICBM recipe the unit-tested tables promise is really in GT's Assembler map: each part and each
 * warhead, at the voltage of its tier. Dev environment only. Server side.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/warheads-and-fill.md (Recipes)
public final class RecipeScenarios {

    private RecipeScenarios() {}

    /** @param registry where to add the scenarios */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new AssemblerRecipes());
    }

    private static final class AssemblerRecipes implements Scenario {

        @Override
        public String name() {
            return "icbm.recipes.assembler";
        }

        @Override
        public String description() {
            return "Every ICBM part and every T1 and T2 warhead has its Assembler recipe at its tier's voltage.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            if (!ModuleState.isActive(ModuleId.ICBM)) {
                return ScenarioResult.skip(name(), "the ICBM module is off, so it registers no recipes");
            }
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            for (IcbmPart part : IcbmPart.values()) {
                GTRecipe r = find(IcbmParts.get(part), null, 0);
                out.check(r != null, "Assembler recipe of " + part.registryName());
                if (r != null) {
                    out.checkEquals(
                        part + " voltage",
                        GTValues.VP[part.tier()
                            .voltageIndex()],
                        (long) r.mEUt);
                    out.checkEquals(part + " output count", part.outputCount(), r.mOutputs[0].stackSize);
                }
            }
            for (WarheadRecipeSpec spec : WarheadRecipeSpec.values()) {
                WarheadBlast blast = spec.blast();
                GTRecipe r = find(null, blast.id(), spec.isStarter() ? 1 : blast.fillCap());
                out.check(r != null, "Assembler recipe of the " + blast.id() + (spec.isStarter() ? " starter" : ""));
                if (r != null) {
                    out.checkEquals(
                        spec + " voltage",
                        GTValues.VP[spec.machineTier()
                            .voltageIndex()],
                        (long) r.mEUt);
                }
            }
            return out.build();
        }

        /** A part by its item, or a warhead by its explosive id and fill. */
        private static GTRecipe find(net.minecraft.item.Item part, String explosive, int fill) {
            for (GTRecipe r : RecipeMaps.assemblerRecipes.getAllRecipes()) {
                if (r.mOutputs == null || r.mOutputs.length == 0 || r.mOutputs[0] == null) {
                    continue;
                }
                ItemStack o = r.mOutputs[0];
                if (part != null) {
                    if (o.getItem() == part) {
                        return r;
                    }
                } else if (o.getItem() instanceof com.gregguns.icbm.warhead.ItemBlockWarhead && o.hasTagCompound()
                    && explosive.equals(
                        o.getTagCompound()
                            .getString(WarheadStacks.K_EXPLOSIVE))
                    && fill == o.getTagCompound()
                        .getInteger(WarheadStacks.K_FILL)) {
                            return r;
                        }
            }
            return null;
        }
    }
}
