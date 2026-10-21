package com.gregguns.recipe;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.gregguns.api.Tier;
import com.gregguns.core.module.ModuleState;
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
        registry.register(new LaserFocusScenario());
        registry.register(new AntiGravityCoreScenario());
    }

    static final class ComponentRecipesScenario implements Scenario {

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
                if (spec.requiresModule() != null && !ModuleState.isActive(spec.requiresModule())) {
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
            out.check(checked >= 20, "the table should hold at least 20 rows, saw " + checked);
            return out.build();
        }

        static void checkMapRecipe(ScenarioResult.Builder out, ComponentTable.Spec spec) {
            ProductionTable.Row row = spec.row();
            RecipeMap<?> map = mapOf(row);
            List<ItemStack> items = new ArrayList<>();
            for (ProductionTable.Input input : row.inputs()) {
                ItemStack stack = ProductionRecipes.resolve(input);
                out.check(stack != null, row.id() + ": input should resolve");
                if (stack != null && stack.stackSize == 0) {
                    stack.stackSize = 1; // a lens sits in the machine at size 1; the recipe asks for 0 (non-consumed)
                }
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
            long voltage = row.tier()
                .voltage();
            GTRecipe found = map.findRecipeQuery()
                .items(items.toArray(new ItemStack[0]))
                .fluids(fluids)
                .voltage(voltage)
                .find();
            out.check(found != null, row.id() + ": the sheet inputs should match a recipe at " + row.tier());
            if (found == null) {
                return;
            }
            if (found.mOutputs == null || found.mOutputs.length == 0 || found.mOutputs[0] == null) {
                // The finder matched a no-output recipe: other pack mods' recipes whose output item is missing on the
                // headless dev server keep only a bare programmed circuit as input and match any circuit-1 query. That
                // is an
                // artifact of the dev pack, so verify the registered recipe itself instead.
                GTRecipe own = makerOf(map, row);
                out.check(own != null, row.id() + ": the recipe should be registered in " + row.target());
                if (own == null) {
                    return;
                }
                out.note(
                    row.id()
                        + ": the finder matched a no-output dev-pack recipe; checked the registered recipe directly");
                found = own;
            }
            out.checkEquals(row.id() + " EU/t", (int) ProductionRecipes.eutFor(row.tier()), found.mEUt);
            out.checkEquals(row.id() + " duration (ticks)", row.ticks(), found.mDuration);
            out.checkEquals(row.id() + " output count", row.outputCount(), found.mOutputs[0].stackSize);
            out.check(
                Tier.lowestForEut(found.mEUt) == row.tier(),
                row.id() + ": the recipe's EU/t should need exactly " + row.tier());
            if (row.tier() != Tier.STEAM) {
                // GT's finder does not filter by voltage; what keeps a lower machine out is the recipe's EU/t, which
                // must exceed the voltage one tier down (so the lower machine could never power it).
                Tier below = Tier.values()[row.tier()
                    .ordinal() - 1];
                out.check(
                    found.mEUt > below.voltage(),
                    row.id() + ": EU/t " + found.mEUt + " should exceed the " + below + " voltage");
            }
        }

        /** @return the recipe in the map whose first item output is the row's output, or null */
        private static GTRecipe makerOf(RecipeMap<?> map, ProductionTable.Row row) {
            ItemStack made = ProductionRecipes.modStack(row.output(), 1);
            for (GTRecipe recipe : map.getAllRecipes()) {
                if (made != null && recipe.mOutputs != null
                    && recipe.mOutputs.length > 0
                    && recipe.mOutputs[0] != null
                    && recipe.mOutputs[0].getItem() == made.getItem()) {
                    return recipe;
                }
            }
            return null;
        }

        static RecipeMap<?> mapOf(ProductionTable.Row row) {
            switch (row.target()) {
                case CHEMICAL_BATH:
                    return RecipeMaps.chemicalBathRecipes;
                case LASER_ENGRAVER:
                    return RecipeMaps.laserEngraverRecipes;
                default:
                    return RecipeMaps.assemblerRecipes;
            }
        }

        static void checkAssemblyLine(ScenarioResult.Builder out, ComponentTable.Spec spec) {
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
            out.checkEquals(row.id() + " duration (ticks)", row.ticks(), line.mDuration);
            out.checkEquals(row.id() + " output count", row.outputCount(), line.mOutput.stackSize);
            ItemStack research = ProductionRecipes.resolve(
                spec.research()
                    .item());
            out.check(
                research != null && line.mResearchItem != null && line.mResearchItem.getItem() == research.getItem(),
                row.id() + ": the research item should be "
                    + spec.research()
                        .item()
                        .special());
            out.checkEquals(
                row.id() + " scan time (ticks)",
                spec.research()
                    .scanSeconds() * GTRecipeBuilder.SECONDS,
                line.mResearchTime);
            out.checkEquals(
                row.id() + " scan voltage",
                (int) ProductionRecipes.eutFor(
                    spec.research()
                        .scanTier()),
                line.mResearchVoltage);
            out.check(
                GTValues.V[row.tier()
                    .voltageIndex()] >= line.mEUt,
                row.id() + ": the line's EU/t should fit its tier's voltage");
        }

        static ComponentTable.Spec specFor(String id) {
            for (ComponentTable.Spec spec : ComponentTable.SPECS) {
                if (spec.row()
                    .id()
                    .equals(id)) {
                    return spec;
                }
            }
            throw new IllegalStateException("no component row " + id);
        }

        static FluidStack[] fluids(ComponentTable.Spec spec) {
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

    /**
     * The engraver boot test of design record 0012 (open risk 2): the Laser Focus recipe, a ruby plus an EV Emitter
     * and a non-consumed white lens, is accepted by GT's Laser Engraver map. It is found by the three inputs at EV, the
     * lens is stored at stack size 0, and the recipe is not found without the lens. Server side.
     */
    static final class LaserFocusScenario implements Scenario {

        @Override
        public String name() {
            return "components.laser_focus";
        }

        @Override
        public String description() {
            return "An EV Laser Engraver with a ruby, an EV Emitter and the white lens makes one Laser Focus; the lens is non-consumed and required.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            ComponentTable.Spec spec = ComponentRecipesScenario.specFor("RC-64");
            out.check(
                spec.row()
                    .target() == ProductionTable.Target.LASER_ENGRAVER,
                "RC-64 should target the Laser Engraver");
            ComponentRecipesScenario.checkMapRecipe(out, spec);
            List<ItemStack> withoutLens = new ArrayList<>();
            for (ProductionTable.Input input : spec.row()
                .inputs()) {
                if (!input.nonConsumed()) {
                    withoutLens.add(ProductionRecipes.resolve(input));
                }
            }
            GTRecipe missing = RecipeMaps.laserEngraverRecipes.findRecipeQuery()
                .items(withoutLens.toArray(new ItemStack[0]))
                .voltage(
                    spec.row()
                        .tier()
                        .voltage())
                .find();
            out.check(missing == null, "without the lens the engraver should not match the recipe");
            List<ItemStack> all = new ArrayList<>();
            for (ProductionTable.Input input : spec.row()
                .inputs()) {
                ItemStack stack = ProductionRecipes.resolve(input);
                if (stack != null && stack.stackSize == 0) {
                    stack.stackSize = 1;
                }
                all.add(stack);
            }
            GTRecipe found = RecipeMaps.laserEngraverRecipes.findRecipeQuery()
                .items(all.toArray(new ItemStack[0]))
                .voltage(
                    spec.row()
                        .tier()
                        .voltage())
                .find();
            if (found != null) {
                int zero = 0;
                for (ItemStack in : found.mInputs) {
                    if (in != null && in.stackSize == 0) {
                        zero++;
                    }
                }
                out.checkEquals("non-consumed (size 0) inputs", 1, zero);
                out.checkEquals("item inputs", 3, found.mInputs.length);
            }
            return out.build();
        }
    }

    /**
     * An IV Assembler makes one Anti Gravity Core from the Nether Star, IV Emitter, IV circuits and molten Enderium.
     */
    static final class AntiGravityCoreScenario implements Scenario {

        @Override
        public String name() {
            return "components.anti_gravity_core";
        }

        @Override
        public String description() {
            return "An IV Assembler with a Nether Star, an IV Emitter, two IV circuits and 1152 mB molten Enderium makes one Anti Gravity Core.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            ComponentRecipesScenario.checkMapRecipe(out, ComponentRecipesScenario.specFor("RC-65"));
            return out.build();
        }
    }
}
