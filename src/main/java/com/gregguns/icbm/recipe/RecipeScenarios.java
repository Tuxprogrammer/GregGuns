package com.gregguns.icbm.recipe;

import net.minecraft.item.ItemStack;

import com.gregguns.core.module.ModuleId;
import com.gregguns.core.module.ModuleState;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.icbm.warhead.InertWarheadSpec;
import com.gregguns.icbm.warhead.InertWarheads;
import com.gregguns.icbm.warhead.WarheadBlast;
import com.gregguns.icbm.warhead.WarheadRecipeSpec;
import com.gregguns.icbm.warhead.WarheadStacks;

import gregtech.api.enums.GTValues;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTRecipe;

/**
 * Checks that every ICBM recipe the unit-tested tables promise is really in GT's Assembler map: each part, each inert
 * warhead, and each warhead's payload and fill (D282), at the voltage of its tier. Dev environment only. Server side.
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
            return "Every ICBM part, every inert warhead and every warhead payload and fill has its Assembler recipe at"
                + " its tier's voltage.";
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
            // D282: the inert warhead of each casing, then each blast's payload and fill.
            for (InertWarheadSpec spec : InertWarheadSpec.values()) {
                GTRecipe r = findOutput(
                    o -> InertWarheads.is(o) && !o.hasTagCompound() && InertWarheads.casing(o) == spec.casing());
                out.check(r != null, "Assembler recipe of the inert " + spec + " warhead");
                if (r != null) {
                    out.checkEquals(spec + " inert voltage", GTValues.VP[spec.voltageIndex()], (long) r.mEUt);
                }
            }
            for (WarheadRecipeSpec spec : WarheadRecipeSpec.values()) {
                WarheadBlast blast = spec.blast();
                if (!spec.chargesOnly()) {
                    GTRecipe p = findOutput(
                        o -> InertWarheads.is(o) && blast.id()
                            .equals(InertWarheads.payload(o)));
                    out.check(p != null, "Assembler payload recipe of " + blast.id());
                    if (p != null) {
                        out.checkEquals(
                            spec + " payload voltage",
                            GTValues.VP[spec.machineVoltageIndex()],
                            (long) p.mEUt);
                    }
                }
                GTRecipe r = find(null, blast.id(), blast.fillCap());
                out.check(r != null, "Assembler fill recipe of " + blast.id());
                if (r != null) {
                    out.checkEquals(spec + " fill voltage", GTValues.VP[spec.machineVoltageIndex()], (long) r.mEUt);
                    out.check(r.isNBTSensitive, spec + " fill recipe matches the payload exactly (NBT-sensitive)");
                }
            }
            return out.build();
        }

        /** The first Assembler recipe whose first output passes the test. */
        private static GTRecipe findOutput(java.util.function.Predicate<ItemStack> test) {
            for (GTRecipe r : RecipeMaps.assemblerRecipes.getAllRecipes()) {
                if (r.mOutputs != null && r.mOutputs.length > 0 && r.mOutputs[0] != null && test.test(r.mOutputs[0])) {
                    return r;
                }
            }
            return null;
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
