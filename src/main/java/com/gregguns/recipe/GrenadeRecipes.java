package com.gregguns.recipe;

import gregtech.api.GregTechAPI;

/**
 * Registers the MV grenade recipes from {@link GrenadeTable} on GT's Assembler. Same timing as {@link MeleeRecipes}:
 * queued on {@code GregTechAPI.sBeforeGTPostload}. Common side.
 */
// Spec: docs/gregguns-spec/02-guns/recipes.md (MV table)
public final class GrenadeRecipes {

    private GrenadeRecipes() {}

    /** Queues the registration on GT's before-postload hook. Call from the guns module's init. */
    public static void register() {
        GregTechAPI.sBeforeGTPostload.add(GrenadeRecipes::addAll);
    }

    private static void addAll() {
        for (ProductionTable.Row row : GrenadeTable.ROWS) {
            ProductionRecipes.addMachineRecipe(row);
        }
    }
}
