package com.gregguns.utility.rampart;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

import com.gregguns.GregGuns;
import com.gregguns.api.Tier;
import com.gregguns.block.GregGunsCasings;
import com.gregguns.machine.GregGunsMTEIDs;
import com.gregguns.utility.ItemTurretArmor;
import com.gregguns.utility.UtilityParts;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.util.GTModHandler;
import gregtech.api.util.GTOreDictUnificator;

/**
 * Creates the Rampart and the four Gun Mount Hatches and adds their recipes. Registration is a plain call from
 * {@code RampartRegistration.init}, inside GT's MTE window (after GT's preload, before its postload); the recipes are
 * queued on GT's before-postload hook so {@code ItemList} is filled. The casing block must exist, so the guns module
 * (which creates it) must be on. Common side.
 *
 * <p>
 * Usage: {@code RampartMachines.hatchStack(Tier.HV, 1)} is a HV Gun Mount Hatch item.
 */
// Spec: docs/gregguns-spec/09-machines-utility/recipes.md (Rampart and Gun Mount Hatch, GGS-09-35 to GGS-09-38)
public final class RampartMachines {

    private static final String HATCH_TOOLTIP = "gregguns.machine.gun_mount_hatch.tooltip";
    private static final String HATCH_TIER_TOOLTIP = "gregguns.machine.gun_mount_hatch.tier";
    private static final String[] HATCH_NAMES = { "Gun Mount Hatch (MV)", "Gun Mount Hatch (HV)",
        "Gun Mount Hatch (EV)", "Gun Mount Hatch (IV)" };

    private RampartMachines() {}

    /**
     * Constructs the Rampart and every hatch. Call once, from {@code RampartRegistration.init}.
     *
     * @return false when the casing block is missing (the guns module did not create it) and nothing was registered
     */
    public static boolean register() {
        if (GregGunsCasings.INSTANCE == null) {
            GregGuns.LOG.warn("The Rampart needs the casing block the guns module creates; it is not registered");
            return false;
        }
        // Source:
        // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/metatileentity/CommonMetaTileEntity.java#L85-L100
        // (construction is legal between GT's preload and postload)
        RampartRecipeSpec.Row[] rows = RampartRecipeSpec.Row.values();
        for (int i = 0; i < rows.length; i++) {
            new MTEGunMountHatch(
                rows[i].mteId(),
                "gregguns.hatch.gunmount.tier.0" + (i + 1),
                HATCH_NAMES[i],
                rows[i].tier()
                    .voltageIndex(),
                new String[] { HATCH_TOOLTIP, HATCH_TIER_TOOLTIP });
        }
        new MTERampart(GregGunsMTEIDs.RAMPART, "gregguns.multimachine.rampart", "Rampart");
        return true;
    }

    /** Queues the recipes; call from {@code RampartRegistration.init} after a successful {@link #register()}. */
    public static void registerRecipes() {
        GregTechAPI.sBeforeGTPostload.add(RampartMachines::addRecipes);
    }

    /**
     * @param tier   MV to IV
     * @param amount stack size
     * @return the Gun Mount Hatch of the tier as an item stack
     * @throws IllegalStateException if the hatch was not registered
     */
    public static ItemStack hatchStack(Tier tier, int amount) {
        return stackOf(
            RampartRecipeSpec.rowFor(tier)
                .mteId(),
            amount);
    }

    /**
     * @param amount stack size
     * @return the Rampart controller as an item stack
     * @throws IllegalStateException if the Rampart was not registered
     */
    public static ItemStack controllerStack(int amount) {
        return stackOf(GregGunsMTEIDs.RAMPART, amount);
    }

    private static ItemStack stackOf(int id, int amount) {
        if (GregTechAPI.METATILEENTITIES[id] == null) {
            throw new IllegalStateException("MTE " + id + " is not registered");
        }
        return GregTechAPI.METATILEENTITIES[id].getStackForm(amount);
    }

    private static ItemStack bastion(BastionPlating plating, int amount) {
        return new ItemStack(GregGunsCasings.INSTANCE, amount, plating.meta());
    }

    /**
     * Concrete as the ballast. Deviation: GregGuns Concrete is not built yet (area 07), so this is GT's own concrete
     * block, or stone when GT has none, exactly as the Turret does.
     */
    private static ItemStack ballast(int amount) {
        return GregTechAPI.sBlockConcretes != null ? new ItemStack(GregTechAPI.sBlockConcretes, amount, 8)
            : new ItemStack(Blocks.stone, amount);
    }

    private static void addRecipes() {
        // Source (a casing: a plate and ballast in the Assembler, programmed circuit, 10 s):
        // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/postload/recipes/AssemblerRecipes.java#L2671-L2700
        for (BastionPlating plating : BastionPlating.values()) {
            ItemTurretArmor armor = UtilityParts.plate(plating.plate());
            if (armor == null) {
                GregGuns.LOG.warn("No turret armor plate for {}; its Bastion Plating recipe is skipped", plating);
                continue;
            }
            GTValues.RA.stdBuilder()
                .itemInputs(new ItemStack(armor), ballast(BastionPlating.CONCRETE))
                .circuit(plating.circuit())
                .itemOutputs(bastion(plating, BastionPlating.YIELD))
                .duration(BastionPlating.DURATION_TICKS)
                .eut(plating.recipeEut())
                .addTo(gregtech.api.recipe.RecipeMaps.assemblerRecipes);
        }
        // Source (a machine around its hull, GT machine style):
        // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/load/MTERecipeLoader.java#L3767-L3771
        GTModHandler.addCraftingRecipe(
            controllerStack(1),
            GTModHandler.RecipeBits.BITS | GTModHandler.RecipeBits.DO_NOT_CHECK_FOR_COLLISIONS,
            new Object[] { "RSR", "CMC", "BKB", 'R', ItemList.Robot_Arm_HV, 'S', ItemList.Sensor_HV, 'C',
                OrePrefixes.circuit.get(Materials.HV), 'M', ItemList.Hull_HV, 'B', bastion(BastionPlating.STEEL, 1),
                'K', ballast(1) });
        // Source (hatches: the tier's hull and parts in the Assembler):
        // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/postload/recipes/AssemblerRecipes.java#L3471-L3480
        for (RampartRecipeSpec.Row row : RampartRecipeSpec.Row.values()) {
            String tier = row.tier()
                .gtName();
            GTValues.RA.stdBuilder()
                .itemInputs(
                    ItemList.valueOf("Hull_" + tier)
                        .get(1),
                    ItemList.valueOf("Robot_Arm_" + tier)
                        .get(1),
                    ItemList.valueOf("Sensor_" + tier)
                        .get(1),
                    GTOreDictUnificator
                        .get(OrePrefixes.plate, Materials.get(row.plateMaterial()), RampartRecipeSpec.HATCH_PLATES))
                .circuit(RampartRecipeSpec.HATCH_CIRCUIT)
                .itemOutputs(stackOf(row.mteId(), 1))
                .duration(RampartRecipeSpec.HATCH_TICKS)
                .eut(row.recipeEut())
                .addTo(gregtech.api.recipe.RecipeMaps.assemblerRecipes);
        }
        GregGuns.LOG.debug("Queued the Rampart and Gun Mount Hatch recipes");
    }
}
