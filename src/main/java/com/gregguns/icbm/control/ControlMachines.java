package com.gregguns.icbm.control;

import net.minecraft.item.ItemStack;

import com.gregguns.GregGuns;
import com.gregguns.machine.GregGunsMTEIDs;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.objects.ItemData;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;

/**
 * Creates the Launch Control machine and its Assembler recipe. {@link #register()} runs inside GT's MTE window (after
 * GT's preload, before its postload), whatever the module flag says (content always registers); the recipe is queued
 * for GT's before-postload hook and added only for an enabled module. Common side.
 */
// Spec: docs/gregguns-icbm-spec/04-launch-control/launch-control-block.md (Recipe)
public final class ControlMachines {

    private static final String[] TOOLTIP = { "gregguns.icbm.control.tooltip", "gregguns.icbm.control.tooltip.reach",
        "gregguns.icbm.control.tooltip.range", "gregguns.icbm.control.tooltip.eu",
        "gregguns.icbm.control.tooltip.triggers" };

    private ControlMachines() {}

    /** Constructs the machine. Call once, from the ICBM module's init. */
    public static void register() {
        // Source:
        // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/metatileentity/CommonMetaTileEntity.java#L85-L100
        // (construction is legal between GT's preload and postload)
        new MTELaunchControl(GregGunsMTEIDs.LAUNCH_CONTROL, "gregguns.icbm.launchcontrol", "Launch Control", TOOLTIP);
    }

    /** Queues the recipe and the recycling data; call from the ICBM module's init (enabled module only). */
    public static void registerRecipes() {
        GregTechAPI.sBeforeGTPostload.add(ControlMachines::addRecipe);
    }

    /**
     * @param amount stack size
     * @return the Launch Control as an item stack
     * @throws IllegalStateException if it was not registered
     */
    public static ItemStack stack(int amount) {
        if (GregTechAPI.METATILEENTITIES[GregGunsMTEIDs.LAUNCH_CONTROL] == null) {
            throw new IllegalStateException("MTE " + GregGunsMTEIDs.LAUNCH_CONTROL + " is not registered");
        }
        return GregTechAPI.METATILEENTITIES[GregGunsMTEIDs.LAUNCH_CONTROL].getStackForm(amount);
    }

    private static void addRecipe() {
        try {
            ItemStack hull = ItemList.Hull_HV.get(1);
            ItemStack sensor = ItemList.Sensor_HV.get(1);
            ItemStack emitter = ItemList.Emitter_HV.get(1);
            ItemStack stick = ItemList.Tool_DataStick.get(1);
            ItemStack circuits = com.gregguns.icbm.recipe.IcbmIngredients
                .stack(com.gregguns.icbm.recipe.Ing.gt("circuit", "Advanced", 2, com.gregguns.api.Tier.HV, 3.0));
            if (hull == null || sensor == null || emitter == null || stick == null || circuits == null) {
                GregGuns.LOG.error("Skipping the Launch Control recipe: a GT input did not resolve");
                return;
            }
            GTValues.RA.stdBuilder()
                .circuit(24)
                .itemInputs(hull, sensor, emitter, stick, circuits)
                .itemOutputs(stack(1))
                .duration(600)
                .eut(TierEU.RECIPE_HV)
                .addTo(RecipeMaps.assemblerRecipes);
            // The hull's casing comes back; electronics and the stick do not (D44).
            GTOreDictUnificator.addItemData(
                stack(1),
                new ItemData(Materials.StainlessSteel, OrePrefixes.plate.getMaterialAmount() * 8));
        } catch (RuntimeException e) {
            GregGuns.LOG.error("Launch Control recipe failed", e);
        }
    }
}
