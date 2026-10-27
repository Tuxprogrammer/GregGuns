package com.gregguns.icbm.antidote;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;

import com.gregguns.GregGuns;
import com.gregguns.api.Tier;
import com.gregguns.core.registry.ModuleRegistrar;
import com.gregguns.icbm.recipe.IcbmIngredients;
import com.gregguns.icbm.recipe.Ing;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.GTValues;
import gregtech.api.recipe.RecipeMaps;

/**
 * Registers the antidote item and its guard (always; content always registers) and its one Assembler recipe (D69, D151:
 * HV, two syringes from rubber, steel and charcoal). Common side.
 */
// Spec: docs/gregguns-icbm-spec/06-blocks-items/antidote.md
public final class AntidoteRegistration {

    /** The programmed circuit of the recipe. */
    public static final int CIRCUIT = 6;
    /** The Assembler's tier. */
    public static final Tier MACHINE_TIER = Tier.HV;
    /** The recipe's inputs (data, so a unit test can check the HV floor). */
    public static final List<Ing> INPUTS = Collections.unmodifiableList(
        Arrays.asList(
            Ing.gt("plate", "Rubber", 1, Tier.LV, 1.0),
            Ing.gt("plate", "StainlessSteel", 1, Tier.HV, 1.0),
            Ing.gt("dust", "Charcoal", 2, Tier.LV, 0.2)));

    private static ItemAntidote item;

    private AntidoteRegistration() {}

    /** Registers the item and the guard. */
    public static void preInit(ModuleRegistrar registrar) {
        item = registrar.registerItem(ItemAntidote.NAME, new ItemAntidote());
        item.setTextureName("gregguns:thirdparty/ntm/antidote");
        MinecraftForge.EVENT_BUS.register(new AntidoteGuard());
    }

    /** Queues the recipe. Enabled module only. */
    public static void init() {
        GregTechAPI.sBeforeGTPostload.add(AntidoteRegistration::addRecipe);
    }

    /** Dev-only scenarios. */
    public static void registerScenarios(com.gregguns.core.testing.ScenarioRegistry registry) {
        AntidoteScenarios.registerAll(registry);
    }

    /** @return the antidote item; null before registration */
    public static ItemAntidote item() {
        return item;
    }

    private static void addRecipe() {
        List<ItemStack> items = new ArrayList<>();
        for (Ing in : INPUTS) {
            ItemStack stack = IcbmIngredients.stack(in);
            if (stack == null) {
                GregGuns.LOG.error("Skipping the antidote recipe: {} is missing", IcbmIngredients.describe(in));
                return;
            }
            items.add(stack);
        }
        GTValues.RA.stdBuilder()
            .circuit(CIRCUIT)
            .itemInputs(items.toArray(new ItemStack[0]))
            .itemOutputs(new ItemStack(item, AntidoteRules.PER_RUN))
            .duration(200)
            .eut(GTValues.VP[MACHINE_TIER.voltageIndex()])
            .addTo(RecipeMaps.assemblerRecipes);
    }
}
