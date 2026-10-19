package com.gregguns.recipe;

import net.minecraft.item.ItemStack;

import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.item.ammo.def.NukeRocketAmmo;
import com.gregguns.item.part.ComponentItems;

import gregtech.api.util.GTUtility;

/**
 * Scenarios for the Assembly Line ammo (Nether Charge, Tactical Nuke Rocket). Dev environment only, registered from
 * {@link ProductionScenarios#registerAll}. Server side.
 */
// Spec: docs/gregguns-spec/06-items/recipes.md (RC-35, RC-36, GGS-06-23, GGS-06-43)
public final class AmmoLineScenarios {

    private AmmoLineScenarios() {}

    /**
     * Registers the scenarios.
     *
     * @param registry where to add them
     */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new AmmoLineScenario());
    }

    /** RC-35 and RC-36 are on GT's Assembly Line list, and the nuclear ammo is radioactive. */
    private static final class AmmoLineScenario implements Scenario {

        @Override
        public String name() {
            return "production.ammo_line";
        }

        @Override
        public String description() {
            return "RC-35 (Tactical Nuke Rocket) and RC-36 (Nether Charge) are Assembly Line recipes with the sheet's EU/t, time, count and research; the nuke rocket and the depleted cell are radioactive.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            for (ComponentTable.Spec spec : AmmoLineTable.SPECS) {
                ComponentScenarios.ComponentRecipesScenario.checkAssemblyLine(out, spec);
            }
            for (String item : new String[] { NukeRocketAmmo.TYPE.roundItemName(),
                ComponentItems.CELL_NUCLEAR_DEPLETED }) {
                ItemStack stack = ProductionRecipes.modStack(item, 1);
                out.check(stack != null, item + " should be registered");
                if (stack != null) {
                    out.check(GTUtility.getRadioactivityLevel(stack) > 0, item + " should be radioactive through GT");
                }
            }
            return out.build();
        }
    }
}
