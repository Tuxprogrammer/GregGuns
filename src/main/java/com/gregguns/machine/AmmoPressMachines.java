package com.gregguns.machine;

import net.minecraft.item.ItemStack;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OreDictNames;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.VoltageIndex;
import gregtech.api.metatileentity.implementations.MTEBasicMachineWithRecipe;
import gregtech.api.util.GTModHandler;

/**
 * Creates and looks up the five Ammo Press machines (Steam, LV, MV, HV, EV).
 *
 * <p>
 * Registration is a plain call from {@code GunsModule.init}, which is legal because GT's rule is "after GT's preload
 * started, before GT's postload started": {@code CommonMetaTileEntity}'s constructor throws {@code IllegalAccessError}
 * outside that window. The steam press's hand recipe is added by {@link #addCraftingRecipes()}, which
 * {@code ProductionRecipes} calls on GT's before-postload hook.
 *
 * <p>
 * Usage: {@code ItemStack press = AmmoPressMachines.stack(AmmoPressTier.MV, 1);}. Common side.
 */
// Spec: docs/gregguns-spec/08-machines-production/ammo-press.md, recipes.md (Ammo Press, Steam; Ammo Press, LV to EV),
// metatileentity-framework.md (registration window)
// Deviation: the sheet (GGS-08-02) suggests GregTechAPI.sAfterGTPreload. GregGuns requires gregtech first, so GT's
// preload has already finished by our preInit and a Runnable queued on that list would never run; constructing from
// init is inside the legal window and is what CropsNH does.
public final class AmmoPressMachines {

    private static final String TOOLTIP_KEY = "gregguns.machine.ammo_press.tooltip";
    private static final String STEAM_TOOLTIP_KEY = "gregguns.machine.ammo_press_steam.tooltip";
    private static final String TYPE_KEY = "gregguns.machine.ammo_press.type";

    private AmmoPressMachines() {}

    /**
     * Constructs every Ammo Press. Call once, from {@code GunsModule.init}.
     */
    public static void register() {
        // Source:
        // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/metatileentity/CommonMetaTileEntity.java#L85-L100
        new MTEAmmoPressSteam(
            GregGunsMTEIDs.AMMO_PRESS_STEAM,
            "gregguns.bronzemachine.ammopress",
            "Steam Ammo Press",
            STEAM_TOOLTIP_KEY);
        // Source:
        // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTModHandler.java#L668-L720
        // (the X placeholders are swapped for the tier's hull, circuit, wire and parts)
        electric(GregGunsMTEIDs.AMMO_PRESS_LV, "gregguns.ammopress.tier.01", "Basic Ammo Press", VoltageIndex.LV);
        electric(GregGunsMTEIDs.AMMO_PRESS_MV, "gregguns.ammopress.tier.02", "Advanced Ammo Press", VoltageIndex.MV);
        electric(GregGunsMTEIDs.AMMO_PRESS_HV, "gregguns.ammopress.tier.03", "Advanced Ammo Press II", VoltageIndex.HV);
        electric(
            GregGunsMTEIDs.AMMO_PRESS_EV,
            "gregguns.ammopress.tier.04",
            "Advanced Ammo Press III",
            VoltageIndex.EV);
    }

    private static void electric(int id, String name, String regional, int voltageIndex) {
        new MTEAmmoPress(
            id,
            name,
            regional,
            voltageIndex,
            new String[] { TOOLTIP_KEY, TYPE_KEY },
            new Object[] { "VPV", "CMC", "WEW", 'V', MTEBasicMachineWithRecipe.X.CONVEYOR, 'P',
                MTEBasicMachineWithRecipe.X.PISTON, 'C', MTEBasicMachineWithRecipe.X.CIRCUIT, 'M',
                MTEBasicMachineWithRecipe.X.HULL, 'W', MTEBasicMachineWithRecipe.X.WIRE, 'E',
                MTEBasicMachineWithRecipe.X.MOTOR });
    }

    /**
     * Adds the steam press's hand recipe. The electric tiers' recipes are made by the machine constructor.
     * Call once, after GT's loaders have filled {@code ItemList} (GT's before-postload hook).
     */
    // Source:
    // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/load/MTERecipeLoader.java#L2849-L2853
    // (the Bronze Compressor: small bronze pipes, a vanilla-ore-dict piston, Hull_Bronze). Gate material: Bronze.
    public static void addCraftingRecipes() {
        ItemStack steam = stack(AmmoPressTier.STEAM, 1);
        GTModHandler.addCraftingRecipe(
            steam,
            GTModHandler.RecipeBits.BITS | GTModHandler.RecipeBits.DO_NOT_CHECK_FOR_COLLISIONS,
            new Object[] { "XXX", "PMP", "BBB", 'X', OrePrefixes.pipeSmall.get(Materials.Bronze), 'P',
                OreDictNames.craftingPiston, 'M', ItemList.Hull_Bronze, 'B', OrePrefixes.plate.get(Materials.Bronze) });
    }

    /**
     * @param tier   which press
     * @param amount stack size
     * @return the press as an item stack
     * @throws IllegalStateException if the machine was not registered (the guns module is off, or too early)
     */
    public static ItemStack stack(AmmoPressTier tier, int amount) {
        if (GregTechAPI.METATILEENTITIES[tier.mteId()] == null) {
            throw new IllegalStateException("The " + tier + " Ammo Press is not registered");
        }
        return GregTechAPI.METATILEENTITIES[tier.mteId()].getStackForm(amount);
    }

    /**
     * @param amount stack size
     * @return the LV Ammo Press as an item stack
     * @throws IllegalStateException if the machine was not registered (the guns module is off, or too early)
     */
    public static ItemStack lvStack(int amount) {
        return stack(AmmoPressTier.LV, amount);
    }
}
