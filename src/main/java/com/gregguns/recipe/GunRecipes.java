package com.gregguns.recipe;

import com.gregguns.gun.def.GunDefinition;
import com.gregguns.gun.def.GunDefinitions;

import gregtech.api.GregTechAPI;

/**
 * Registers the guns' final-assembly recipes on GT's Assembler, their crafting-table recipes and their GT recycling
 * data, all from the {@link GunDefinition}s in {@link GunDefinitions#ALL}: each gun's rows go to the Assembler, then
 * its {@code recipes} and {@code recycling} hooks run. Parts come through the unificator, never as raw ore-dictionary
 * names.
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
        for (GunDefinition gun : GunDefinitions.ALL) {
            for (ProductionTable.Row row : gun.rows()) {
                ProductionRecipes.addMachineRecipe(row);
            }
            if (gun.recipes() != null) {
                gun.recipes()
                    .run();
            }
            if (gun.recycling() != null) {
                gun.recycling()
                    .run();
            }
        }
    }
}
