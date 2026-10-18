package com.gregguns.recipe;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.gregguns.api.Tier;
import com.gregguns.core.config.ModuleFlags;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;

import gregtech.api.enums.GTValues;
import gregtech.api.enums.Materials;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.api.util.GTUtility;

/**
 * In-game scenario for the component recipes: every Assembler and Chemical Bath row of {@link ComponentTable} is found
 * in its GT map with the sheet inputs at its tier's voltage (and not one tier below), with the sheet's EU/t, duration
 * and output; every Assembly Line row is in GT's Assembly Line list with its research item and voltage. Dev
 * environment only, registered from {@code GunsModule}. Server side.
 */
// Spec: docs/gregguns-spec/06-items/recipes.md (GGS-06-34, GGS-06-35, GGS-06-43)
public final class ComponentScenarios {

    private ComponentScenarios() {}

    /**
     * Registers the component scenarios.
     *
     * @param registry where to add them
     */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new ComponentRecipesScenario());
    }

    private static final class ComponentRecipesScenario implements Scenario {

        @Override
        public String name() {
            return "production.components";
        }

        @Override
        public String description() {
            return "Every component row is in its GT map (Assembler, Chemical Bath, Assembly Line) with the sheet's inputs, EU/t and duration, found at its tier's voltage and not below.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            int checked = 0;
            for (ComponentTable.Spec spec : ComponentTable.SPECS) {
                if (spec.requiresModule() != null && !ModuleFlags.isEnabled(spec.requiresModule())) {
                    continue;
                }
                ProductionTable.Row row = spec.row();
                if (row.target() == ProductionTable.Target.ASSEMBLY_LINE) {
                    checkAssemblyLine(out, spec);
                } else {
                    checkMapRecipe(out, spec);
                }
                checked++;
            }
            out.check(checked >= 25, "the table should hold at least 25 rows, saw " + checked);
            return out.build();
        }

        private static void checkMapRecipe(ScenarioResult.Builder out, ComponentTable.Spec spec) {
            ProductionTable.Row row = spec.row();
            RecipeMap<?> map = row.target() == ProductionTable.Target.CHEMICAL_BATH ? RecipeMaps.chemicalBathRecipes
                : RecipeMaps.assemblerRecipes;
            List<ItemStack> items = new ArrayList<>();
            for (ProductionTable.Input input : row.inputs()) {
                ItemStack stack = ProductionRecipes.resolve(input);
                out.check(stack != null, row.id() + ": input should resolve");
                items.add(stack);
            }
            if (row.circuit() > 0) {
                items.add(GTUtility.getIntegratedCircuit(row.circuit()));
            }
            FluidStack[] fluids = fluids(spec);
            if (items.contains(null) || fluids == null) {
                out.check(fluids != null, row.id() + ": GT should have the fluid");
                return;
            }
            long voltage = row.tier().voltage();
            GTRecipe found = map.findRecipeQuery()
                .items(items.toArray(new ItemStack[0]))
                .fluids(fluids)
                .voltage(voltage)
                .find();
            out.check(found != null, row.id() + ": the sheet inputs should match a recipe at " + row.tier());
            if (found == null) {
                return;
            }
            out.checkEquals(row.id() + " EU/t", (int) ProductionRecipes.eutFor(row.tier()), found.mEUt);
            out.checkEquals(row.id() + " duration (ticks)", row.seconds() * GTRecipeBuilder.SECONDS, found.mDuration);
            out.checkEquals(row.id() + " output count", row.outputCount(), found.mOutputs[0].stackSize);
            out.check(
                Tier.lowestForEut(found.mEUt) == row.tier(),
                row.id() + ": the recipe's EU/t should need exactly " + row.tier());
            if (row.tier() != Tier.STEAM) {
                Tier below = Tier.values()[row.tier().ordinal() - 1];
                GTRecipe tooLow = map.findRecipeQuery()
                    .items(items.toArray(new ItemStack[0]))
                    .fluids(fluids)
                    .voltage(below.voltage())
                    .find();
                out.check(tooLow == null, row.id() + ": a " + below + " machine should not be offered the recipe");
            }
        }

        private static void checkAssemblyLine(ScenarioResult.Builder out, ComponentTable.Spec spec) {
            ProductionTable.Row row = spec.row();
            ItemStack made = ProductionRecipes.modStack(row.output(), 1);
            GTRecipe.RecipeAssemblyLine line = null;
            for (GTRecipe.RecipeAssemblyLine candidate : GTRecipe.RecipeAssemblyLine.sAssemblylineRecipes) {
                if (made != null && candidate.mOutput != null && candidate.mOutput.getItem() == made.getItem()) {
                    line = candidate;
                }
            }
            out.check(line != null, row.id() + ": GT's Assembly Line list should hold the recipe");
            if (line == null) {
                return;
            }
            out.checkEquals(row.id() + " EU/t", (int) ProductionRecipes.eutFor(row.tier()), line.mEUt);
            out.checkEquals(row.id() + " duration (ticks)", row.seconds() * GTRecipeBuilder.SECONDS, line.mDuration);
            out.checkEquals(row.id() + " output count", row.outputCount(), line.mOutput.stackSize);
            ItemStack research = ProductionRecipes.resolve(spec.research().item());
            out.check(
                research != null && line.mResearchItem != null && line.mResearchItem.getItem() == research.getItem(),
                row.id() + ": the research item should be " + spec.research().item().special());
            out.checkEquals(
                row.id() + " scan time (ticks)",
                spec.research().scanSeconds() * GTRecipeBuilder.SECONDS,
                line.mResearchTime);
            out.checkEquals(
                row.id() + " scan voltage",
                (int) ProductionRecipes.eutFor(spec.research().scanTier()),
                line.mResearchVoltage);
            out.check(
                GTValues.V[row.tier().voltageIndex()] >= line.mEUt,
                row.id() + ": the line's EU/t should fit its tier's voltage");
        }

        private static FluidStack[] fluids(ComponentTable.Spec spec) {
            List<FluidStack> out = new ArrayList<>();
            for (ComponentTable.FluidInput fluid : spec.fluids()) {
                Materials material = Materials.get(fluid.material());
                FluidStack stack = fluid.kind() == ComponentTable.FluidKind.MOLTEN ? material.getMolten(fluid.amount())
                    : material.getFluid(fluid.amount());
                if (stack == null) {
                    return null;
                }
                out.add(stack);
            }
            return out.toArray(new FluidStack[0]);
        }
    }
}
