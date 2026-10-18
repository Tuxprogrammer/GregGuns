package com.gregguns.machine;

import net.minecraft.item.ItemStack;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.VoltageIndex;
import gregtech.api.metatileentity.implementations.MTEBasicMachineWithRecipe;

/**
 * Creates and looks up the Ammo Press machines.
 *
 * <p>
 * M1 builds the LV press only. Registration is a plain call from {@code GunsModule.init}, which is legal because GT's
 * rule is "after GT's preload started, before its postload started":
 * {@code CommonMetaTileEntity}'s constructor throws {@code IllegalAccessError} outside that window.
 *
 * <p>
 * Usage: {@code ItemStack press = AmmoPressMachines.lvStack(1);}. Common side.
 */
// Spec: docs/gregguns-spec/08-machines-production/ammo-press.md, metatileentity-framework.md (registration window)
// Deviation: the sheet (GGS-08-02) suggests GregTechAPI.sAfterGTPreload. GregGuns requires gregtech first, so GT's
// preload has already finished by our preInit and a Runnable queued on that list would never run; constructing from
// init is inside the legal window and is what CropsNH does.
public final class AmmoPressMachines {

    private static final String LV_NAME = "gregguns.ammopress.tier.01";
    private static final String LV_REGIONAL = "Basic Ammo Press";
    private static final String TOOLTIP_KEY = "gregguns.machine.ammo_press.tooltip";
    private static final String TYPE_KEY = "gregguns.machine.ammo_press.type";

    private AmmoPressMachines() {}

    /**
     * Constructs every Ammo Press built so far. Call once, from {@code GunsModule.init}.
     */
    public static void register() {
        // Source:
        // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/metatileentity/CommonMetaTileEntity.java#L85-L100
        // Source:
        // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTModHandler.java#L668-L720
        // (the X placeholders are swapped for the tier's hull, circuit, wire and parts)
        new MTEAmmoPress(
            GregGunsMTEIDs.AMMO_PRESS_LV,
            LV_NAME,
            LV_REGIONAL,
            VoltageIndex.LV,
            new String[] { TOOLTIP_KEY, TYPE_KEY },
            new Object[] { "VPV", "CMC", "WEW", 'V', MTEBasicMachineWithRecipe.X.CONVEYOR, 'P',
                MTEBasicMachineWithRecipe.X.PISTON, 'C', MTEBasicMachineWithRecipe.X.CIRCUIT, 'M',
                MTEBasicMachineWithRecipe.X.HULL, 'W', MTEBasicMachineWithRecipe.X.WIRE, 'E',
                MTEBasicMachineWithRecipe.X.MOTOR });
    }

    /**
     * @param amount stack size
     * @return the LV Ammo Press as an item stack
     * @throws IllegalStateException if the machine was not registered (the guns module is off, or too early)
     */
    public static ItemStack lvStack(int amount) {
        if (GregTechAPI.METATILEENTITIES[GregGunsMTEIDs.AMMO_PRESS_LV] == null) {
            throw new IllegalStateException("The LV Ammo Press is not registered");
        }
        return GregTechAPI.METATILEENTITIES[GregGunsMTEIDs.AMMO_PRESS_LV].getStackForm(amount);
    }
}
