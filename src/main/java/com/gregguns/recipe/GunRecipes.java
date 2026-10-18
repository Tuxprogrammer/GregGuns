package com.gregguns.recipe;

import net.minecraft.item.ItemStack;

import com.gregguns.GregGuns;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.objects.ItemData;
import gregtech.api.objects.MaterialStack;
import gregtech.api.util.GTOreDictUnificator;

/**
 * Registers the guns' final-assembly recipes on GT's Assembler (the 9mm Pistol for M1) and the GT recycling data of
 * the guns, from {@link GunTable}. Parts come through the unificator, never as raw ore-dictionary names.
 *
 * <p>
 * Timing: {@link #register()} queues the work on {@code GregTechAPI.sBeforeGTPostload}, exactly as
 * {@link ProductionRecipes} does, so the barrel and mechanical-parts recipes and item data it needs already exist
 * and GT's postload generators see the gun's {@link ItemData}. A row with an unresolved input is skipped with an
 * error in the log, and the dev self-check fails the boot on it. Common side.
 */
// Spec: docs/gregguns-spec/02-guns/recipes.md (LV table, 9mm Pistol), docs/gregguns-spec/06-items/recipes.md GGS-06-34
public final class GunRecipes {

    private GunRecipes() {}

    /** Queues the registration on GT's before-postload hook. Call from the guns module's init. */
    public static void register() {
        // Source:
        // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/GTMod.java#L440-L447
        GregTechAPI.sBeforeGTPostload.add(GunRecipes::addAll);
    }

    private static void addAll() {
        for (ProductionTable.Row row : GunTable.ROWS) {
            ProductionRecipes.addMachineRecipe(row);
        }
        addRecyclingData();
    }

    /**
     * What recycling returns for the pistol: the steel and rubber of its direct inputs plus the steel inside its
     * barrel and mechanical parts. Amounts come from GT's own per-prefix figures.
     */
    // Source:
    // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTOreDictUnificator.java#L350-L359
    private static void addRecyclingData() {
        long plate = OrePrefixes.plate.getMaterialAmount();
        long barrel = OrePrefixes.pipeSmall.getMaterialAmount() + 2 * OrePrefixes.ring.getMaterialAmount() + plate;
        long mechanical2 = AmmoComposition.mechanical2Steel(
            OrePrefixes.gearGtSmall.getMaterialAmount(),
            OrePrefixes.springSmall.getMaterialAmount(),
            OrePrefixes.screw.getMaterialAmount());
        long steel = barrel + mechanical2
            + GunComposition.pistolDirectSteel(
                plate,
                OrePrefixes.screw.getMaterialAmount(),
                OrePrefixes.springSmall.getMaterialAmount());
        ItemStack pistol = ProductionRecipes.modStack(GunTable.PISTOL.output(), 1);
        if (pistol == null) {
            GregGuns.LOG.error("Cannot attach ItemData to {}: not registered", GunTable.PISTOL.output());
            return;
        }
        GTOreDictUnificator
            .addItemData(pistol, new ItemData(Materials.Steel, steel, new MaterialStack(Materials.Rubber, 2 * plate)));
    }
}
