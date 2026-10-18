package com.gregguns.machine;

import net.minecraft.item.ItemStack;

import com.gregguns.block.GregGunsCasings;
import com.gregguns.core.registry.ModuleRegistrar;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OreDictNames;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.enums.VoltageIndex;
import gregtech.api.metatileentity.implementations.MTEBasicMachineWithRecipe;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTModHandler;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipeBuilder;

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
     * Creates the Ammunition Works casing block. Call once, from {@code GunsModule.preInit}, so the block has a
     * registry id before any world loads.
     *
     * @param registrar the guns module's registrar (for the creative tab)
     */
    public static void registerBlocks(ModuleRegistrar registrar) {
        GregGunsCasings.create(registrar.tab());
    }

    /**
     * Constructs every Ammo Press and the Ammunition Works. Call once, from {@code GunsModule.init}.
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
        new MTEAmmoWorks(GregGunsMTEIDs.AMMO_WORKS, "gregguns.multimachine.ammoworks", "Ammunition Works");
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
        addWorksRecipes();
    }

    /**
     * Adds the Ammunition Works' own recipes: the Works Casing and Ram Housing in the Assembler and the controller's
     * hand recipe.
     */
    // Spec: docs/gregguns-spec/08-machines-production/recipes.md (Ammunition Works, 3 recipes; GGS-08-86, GGS-08-89)
    // Source (casing: 6 plates and a frame, circuit 1, RECIPE_LV / 2):
    // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/postload/recipes/AssemblerRecipes.java#L2671-L2700
    // Source (controller: shaped around a hull, GT machine style):
    // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/load/MTERecipeLoader.java#L3767-L3771
    // Gate material: Stainless Steel (HV, docs/design/0001-tier-map.md); AmmoWorksRecipeSpec pins it to GateRule.
    private static void addWorksRecipes() {
        ItemStack stainlessPlates = GTOreDictUnificator
            .get(OrePrefixes.plate, Materials.StainlessSteel, AmmoWorksRecipeSpec.CASING_PLATES);
        ItemStack stainlessFrame = GTOreDictUnificator.get(OrePrefixes.frameGt, Materials.StainlessSteel, 1);
        ItemStack stainlessGear = GTOreDictUnificator.get(OrePrefixes.gearGt, Materials.StainlessSteel, 1);
        ItemStack casing = new ItemStack(GregGunsCasings.INSTANCE, 1, GregGunsCasings.WORKS_CASING);
        ItemStack casingOut = new ItemStack(
            GregGunsCasings.INSTANCE,
            AmmoWorksRecipeSpec.CASINGS_PER_CRAFT,
            GregGunsCasings.WORKS_CASING);
        ItemStack housing = new ItemStack(GregGunsCasings.INSTANCE, 1, GregGunsCasings.RAM_HOUSING);

        GTValues.RA.stdBuilder()
            .itemInputs(stainlessPlates, stainlessFrame)
            .circuit(1)
            .itemOutputs(casingOut)
            .duration(2 * GTRecipeBuilder.SECONDS + 10 * GTRecipeBuilder.TICKS)
            .eut(TierEU.RECIPE_LV / 2)
            .addTo(RecipeMaps.assemblerRecipes);
        GTValues.RA.stdBuilder()
            .itemInputs(casing, ItemList.Electric_Piston_HV.get(1), stainlessGear)
            .circuit(2)
            .itemOutputs(housing)
            .duration(10 * GTRecipeBuilder.SECONDS)
            .eut(TierEU.RECIPE_HV)
            .addTo(RecipeMaps.assemblerRecipes);
        GTModHandler.addCraftingRecipe(
            worksStack(1),
            GTModHandler.RecipeBits.BITS | GTModHandler.RecipeBits.DO_NOT_CHECK_FOR_COLLISIONS,
            new Object[] { "WPW", "EME", "WVW", 'W', casing, 'P', ItemList.Electric_Piston_HV, 'E',
                OrePrefixes.circuit.get(Materials.HV), 'M', ItemList.Hull_HV, 'V', ItemList.Conveyor_Module_HV });
    }

    /**
     * @param amount stack size
     * @return the Ammunition Works controller as an item stack
     * @throws IllegalStateException if the machine was not registered (the guns module is off, or too early)
     */
    public static ItemStack worksStack(int amount) {
        if (GregTechAPI.METATILEENTITIES[GregGunsMTEIDs.AMMO_WORKS] == null) {
            throw new IllegalStateException("The Ammunition Works is not registered");
        }
        return GregTechAPI.METATILEENTITIES[GregGunsMTEIDs.AMMO_WORKS].getStackForm(amount);
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
