package com.gregguns.icbm.emp;

import net.minecraft.item.ItemStack;

import com.gregguns.GregGuns;
import com.gregguns.icbm.recipe.IcbmIngredients;
import com.gregguns.icbm.recipe.Ing;
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
 * Creates the EMP Tower and its Assembler recipe. {@link #register()} runs inside GT's MTE window (after GT's preload,
 * before its postload), whatever the module flag says (content always registers); the recipe is queued for GT's
 * before-postload hook and added only for an enabled module. Common side.
 */
// Spec: docs/gregguns-icbm-spec/05-detection-defense/emp-tower.md (Recipe)
public final class EmpMachines {

    private static final String[] TOOLTIP = { "gregguns.icbm.emp.tooltip", "gregguns.icbm.emp.tooltip.pulse",
        "gregguns.icbm.emp.tooltip.cost", "gregguns.icbm.emp.tooltip.missiles", "gregguns.icbm.emp.tooltip.triggers" };

    private EmpMachines() {}

    /** Constructs the machine. Call once, from the ICBM module's init. */
    public static void register() {
        // Source:
        // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/metatileentity/CommonMetaTileEntity.java#L85-L100
        // (construction is legal between GT's preload and postload)
        new MTEEmpTower(GregGunsMTEIDs.EMP_TOWER, "gregguns.icbm.emptower", "EMP Tower", TOOLTIP);
    }

    /** Queues the recipe and the recycling data; call from the ICBM module's init (enabled module only). */
    public static void registerRecipes() {
        GregTechAPI.sBeforeGTPostload.add(EmpMachines::addRecipe);
    }

    /**
     * @param amount stack size
     * @return the EMP Tower as an item stack
     * @throws IllegalStateException if it was not registered
     */
    public static ItemStack stack(int amount) {
        if (GregTechAPI.METATILEENTITIES[GregGunsMTEIDs.EMP_TOWER] == null) {
            throw new IllegalStateException("MTE " + GregGunsMTEIDs.EMP_TOWER + " is not registered");
        }
        return GregTechAPI.METATILEENTITIES[GregGunsMTEIDs.EMP_TOWER].getStackForm(amount);
    }

    private static void addRecipe() {
        try {
            ItemStack hull = ItemList.Hull_EV.get(1);
            ItemStack generator = ItemList.Field_Generator_EV.get(1);
            ItemStack emitter = ItemList.Emitter_EV.get(2);
            ItemStack capacitors = ItemList.Battery_RE_HV_Lithium.get(4);
            ItemStack circuits = IcbmIngredients.stack(Ing.gt("circuit", "Data", 2, com.gregguns.api.Tier.EV, 5.0));
            if (hull == null || generator == null || emitter == null || capacitors == null || circuits == null) {
                GregGuns.LOG.error("Skipping the EMP Tower recipe: a GT input did not resolve");
                return;
            }
            GTValues.RA.stdBuilder()
                .circuit(24)
                .itemInputs(hull, generator, emitter, capacitors, circuits)
                .itemOutputs(stack(1))
                .duration(1200)
                .eut(TierEU.RECIPE_EV)
                .addTo(RecipeMaps.assemblerRecipes);
            // The hull's casing comes back; electronics and batteries do not (D44).
            GTOreDictUnificator
                .addItemData(stack(1), new ItemData(Materials.Titanium, OrePrefixes.plate.getMaterialAmount() * 8));
        } catch (RuntimeException e) {
            GregGuns.LOG.error("EMP Tower recipe failed", e);
        }
    }
}
