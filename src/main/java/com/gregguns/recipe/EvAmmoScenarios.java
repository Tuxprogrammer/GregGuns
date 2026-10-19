package com.gregguns.recipe;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.gregguns.GregGuns;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;

/**
 * Scenarios for the EV ammo items (Advanced Bullets, Advanced Magazine, Minigun Ammo Drum). Dev environment only,
 * registered from {@link ProductionScenarios#registerAll}. Server side.
 */
// Spec: docs/gregguns-spec/06-items/recipes.md (RC-32, RC-42, RC-43, RC-49, RC-50)
public final class EvAmmoScenarios {

    private EvAmmoScenarios() {}

    /**
     * Registers the EV ammo scenarios.
     *
     * @param registry where to add them
     */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new EvAmmoRecipesScenario());
    }

    private static ItemStack mod(String name, int count) {
        Item item = GameRegistry.findItem(GregGuns.MODID, name);
        return item == null ? null : new ItemStack(item, count);
    }

    private static ItemStack gt(String prefix, String material, int count) {
        return GTOreDictUnificator.get(OrePrefixes.getPrefix(prefix), Materials.get(material), count);
    }

    private static void check(ScenarioResult.Builder out, String id, String output, int count, ItemStack... inputs) {
        for (ItemStack stack : inputs) {
            out.check(stack != null, id + ": an input stack failed to resolve");
            if (stack == null) {
                return;
            }
        }
        GTRecipe ev = GregGunsRecipeMaps.AMMO_PRESS.findRecipeQuery()
            .items(inputs)
            .voltage(GTValues.V[4])
            .find();
        out.check(ev != null, id + " should be found at EV voltage");
        if (ev != null) {
            out.checkEquals(id + " EU/t", (int) TierEU.RECIPE_EV, ev.mEUt);
            out.checkEquals(id + " output count", count, ev.mOutputs[0].stackSize);
            out.check(
                ev.mOutputs[0].getItem() == GameRegistry.findItem(GregGuns.MODID, output),
                id + " should make " + output);
        }
        GTRecipe hv = GregGunsRecipeMaps.AMMO_PRESS.findRecipeQuery()
            .items(inputs)
            .voltage(GTValues.V[3])
            .find();
        out.check(hv == null, id + " must not be found at HV voltage");
    }

    /** The ammo map holds each EV ammo row at RECIPE_EV, and an HV press cannot run it. */
    private static final class EvAmmoRecipesScenario implements Scenario {

        @Override
        public String name() {
            return "production.ev_ammo";
        }

        @Override
        public String description() {
            return "RC-32, RC-42, RC-43, RC-49 and RC-50 are on the ammo map at RECIPE_EV with the sheet counts, and not found at HV.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            check(
                out,
                "RC-32",
                "ammo_advanced_bullet",
                16,
                gt("foil", "Titanium", 16),
                gt("round", "Titanium", 16),
                new ItemStack(net.minecraft.init.Blocks.tnt, 1));
            check(
                out,
                "RC-42",
                "mag_advanced_empty",
                8,
                gt("plate", "Titanium", 3),
                gt("springSmall", "Titanium", 8),
                mod("part_mechanical_3", 1));
            check(
                out,
                "RC-43",
                "mag_minigun_drum_empty",
                4,
                gt("plate", "Titanium", 4),
                gt("plate", "Plastic", 2),
                mod("part_mechanical_3", 1));
            check(out, "RC-49", "mag_advanced", 4, mod("mag_advanced_empty", 4), mod("ammo_advanced_bullet", 12));
            check(out, "RC-50", "mag_minigun_drum", 4, mod("mag_minigun_drum_empty", 4), mod("ammo_rifle_bullet", 64));
            return out.build();
        }
    }
}
