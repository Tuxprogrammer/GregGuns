package com.gregguns.recipe;

import java.util.Collections;

import net.minecraft.item.ItemStack;

import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;

import gregtech.api.util.GTRecipe;

/**
 * The Nuclear Death Ray's recipe scenarios ({@link GunLineTable}): GT's Assembly Line list holds it, and it carries the
 * ZPM gate. Dev environment only. Server side.
 */
// Spec: docs/gregguns-spec/02-guns/recipes.md (LuV and ZPM table, Nuclear Death Ray)
public final class NdrLineScenarios {

    private NdrLineScenarios() {}

    /**
     * Registers the recipe and tier-gate scenarios.
     *
     * @param registry where to add them
     */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new Recipe());
        registry.register(new Gate());
    }

    /**
     * GT's Assembly Line list holds GR-103 with its EU/t, time, research, fluids and item inputs; the output is ours.
     */
    private static final class Recipe implements Scenario {

        @Override
        public String name() {
            return "gun.nuclear_death_ray.recipe";
        }

        @Override
        public String description() {
            return "GT's Assembly Line list holds the Nuclear Death Ray (GR-103) with the sheet's EU/t, time, research item, scan time and voltage, both fluids and all 8 item inputs, and the found recipe's output is ours.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            ComponentTable.Spec spec = GunLineTable.byOutput(GunLineTable.NUCLEAR_DEATH_RAY);
            ComponentScenarios.ComponentRecipesScenario.checkAssemblyLine(out, spec);
            ItemStack made = ProductionRecipes.modStack(GunLineTable.NUCLEAR_DEATH_RAY, 1);
            out.check(made != null, "nuclear_death_ray should be registered");
            for (GTRecipe.RecipeAssemblyLine candidate : GTRecipe.RecipeAssemblyLine.sAssemblylineRecipes) {
                if (made != null && candidate.mOutput != null && candidate.mOutput.getItem() == made.getItem()) {
                    out.checkEquals(
                        "fluid inputs",
                        spec.fluids()
                            .size(),
                        candidate.mFluidInputs == null ? 0 : candidate.mFluidInputs.length);
                    out.checkEquals(
                        "item inputs",
                        spec.row()
                            .inputs()
                            .size(),
                        candidate.mInputs == null ? 0 : candidate.mInputs.length);
                }
            }
            return out.build();
        }
    }

    /** The row is ZPM, carries Naquadah alloy (the ZPM gate), and meets the D1 gate rule on its own. */
    private static final class Gate implements Scenario {

        @Override
        public String name() {
            return "gun.nuclear_death_ray.tiergate";
        }

        @Override
        public String description() {
            return "The Nuclear Death Ray row is a ZPM Assembly Line recipe that carries a Naquadah alloy plate, the ZPM gate material, and satisfies the gate rule.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            ProductionTable.Row row = GunLineTable.byOutput(GunLineTable.NUCLEAR_DEATH_RAY)
                .row();
            out.checkEquals("tier", com.gregguns.api.Tier.ZPM, row.tier());
            boolean carries = false;
            for (ProductionTable.Input input : row.inputs()) {
                if ("plate".equals(input.prefix()) && "NaquadahAlloy".equals(input.material())) {
                    carries = true;
                }
            }
            out.check(carries, "the row should carry a NaquadahAlloy plate");
            out.check(
                GateRule.satisfied(
                    row,
                    Collections.<String, ProductionTable.Row>emptyMap(),
                    Collections.<String>emptySet()),
                "the row should satisfy the gate rule");
            return out.build();
        }
    }
}
