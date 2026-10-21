package com.gregguns.recipe;

import com.gregguns.item.ammo.def.GoldenRoundAmmo;

/**
 * Registers the Golden Round's Assembler recipe (RC-66) on GT's before-postload hook, as the
 * {@code recipes} hook of {@code GoldenRoundAmmo}. Common side.
 */
// Spec: docs/gregguns-spec/06-items/recipes.md (RC-66)
public final class GoldenRoundRecipes {

    private GoldenRoundRecipes() {}

    /** Adds the row to GT's Assembler map. */
    public static void register() {
        ProductionRecipes.addMachineRecipe(GoldenRoundAmmo.ROW);
    }
}
