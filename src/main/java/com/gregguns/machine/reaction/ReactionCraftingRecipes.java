package com.gregguns.machine.reaction;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

import com.gregguns.GregGuns;
import com.gregguns.machine.GregGunsMTEIDs;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTModHandler;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipeBuilder;

/**
 * Build recipes for the Reaction Casing, the Heat Ray Focus and the Retort controller, queued on GT's before-postload
 * hook. The single-block chambers carry their own grid in their constructor ({@link ReactionMachines}). Common side.
 */
// Spec: docs/gregguns-spec/08-machines-production/recipes.md (Reaction Chamber Casing, Retort controller, Heat Ray
// Focus)
public final class ReactionCraftingRecipes {

    private ReactionCraftingRecipes() {}

    /** Queues the registration; call from {@code MachinesModule.init}. */
    public static void register() {
        GregTechAPI.sBeforeGTPostload.add(ReactionCraftingRecipes::addAll);
    }

    private static void addAll() {
        ItemStack casing = new ItemStack(ReactionCasingBlock.get(), 1, 0);
        ItemStack focus = ReactionParts.stack(ReactionParts.HEAT_RAY_FOCUS, 1);
        ItemStack plateTitanium = GTOreDictUnificator.get(OrePrefixes.plate, Materials.Titanium, 4);
        ItemStack plateLead = GTOreDictUnificator.get(OrePrefixes.plate, Materials.Lead, 2);
        ItemStack frameTitanium = GTOreDictUnificator.get(OrePrefixes.frameGt, Materials.Titanium, 1);
        ItemStack circuitEv = GTOreDictUnificator.get(OrePrefixes.circuit, Materials.EV, 2);
        ItemStack wire = GTOreDictUnificator.get(OrePrefixes.wireFine, Materials.Gold, 1);
        if (focus == null || plateTitanium == null
            || plateLead == null
            || frameTitanium == null
            || circuitEv == null
            || wire == null) {
            GregGuns.LOG.error("Skipping Reaction Chamber crafting recipes: an item did not resolve");
            return;
        }
        // Source:
        // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/postload/recipes/AssemblerRecipes.java#L2671-L2700
        // (every GT casing: plates, a frame, circuit 1, RECIPE_LV / 2, 2 s 10 ticks).
        GTValues.RA.stdBuilder()
            .itemInputs(plateTitanium, plateLead, frameTitanium)
            .circuit(1)
            .itemOutputs(casing)
            .duration(2 * GTRecipeBuilder.SECONDS + 10)
            .eut(TierEU.RECIPE_LV / 2)
            .addTo(RecipeMaps.assemblerRecipes);
        GTValues.RA.stdBuilder()
            .itemInputs(
                circuitEv,
                ItemList.Emitter_EV.get(1),
                wire,
                plateTitanium.copy(),
                new ItemStack(Blocks.redstone_lamp, 2))
            .circuit(1)
            .itemOutputs(focus)
            .duration(20 * GTRecipeBuilder.SECONDS)
            .eut(TierEU.RECIPE_EV)
            .addTo(RecipeMaps.assemblerRecipes);
        // Source:
        // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/load/MTERecipeLoader.java#L3407-L3413
        // (the Vacuum Freezer controller: casing in the middle, pump, circuit and gold cable around it).
        GTModHandler.addCraftingRecipe(
            ReactionMachines.stack(GregGunsMTEIDs.RETORT, 1),
            GTModHandler.RecipeBits.BITS | GTModHandler.RecipeBits.DO_NOT_CHECK_FOR_COLLISIONS,
            new Object[] { "EPE", "CMC", "WSW", 'E', ItemList.Emitter_EV, 'P', ItemList.Electric_Pump_EV, 'C',
                OrePrefixes.circuit.get(Materials.EV), 'M', casing, 'W', OrePrefixes.cableGt01.get(Materials.Gold), 'S',
                ItemList.Sensor_EV });
    }
}
