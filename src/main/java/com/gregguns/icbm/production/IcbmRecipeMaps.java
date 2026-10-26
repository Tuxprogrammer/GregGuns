package com.gregguns.icbm.production;

import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMapBackend;
import gregtech.api.recipe.RecipeMapBuilder;

/**
 * The ICBM module's own GT recipe map: a display-only list of what the NASA Workbench's missile pages make, so the
 * hand build shows in NEI beside the Assembly Line (every non-GT process is visible, D44). Its rows are fake recipes;
 * no machine runs them. Its NEI tab is the client's {@code MissileWorkbenchHandler} (the schematic in the shape of the
 * missile), not GT's grid. Load it from the module's preInit, so NEI badges the tab as GregGuns' (see
 * {@code GregGunsRecipeMaps}). Common side.
 */
// Spec: docs/gregguns-icbm-spec/03-missiles/build-and-recipes.md (NEI)
public final class IcbmRecipeMaps {

    /** The map's unlocalized name: also the NEI handler id and the lang key of the tab title. */
    public static final String MISSILE_WORKBENCH_NAME = "gregguns.missileWorkbench";

    /** Four item inputs (plates, fins, engine, guidance computer) and the body. */
    public static final RecipeMap<RecipeMapBackend> MISSILE_WORKBENCH = RecipeMapBuilder.of(MISSILE_WORKBENCH_NAME)
        .maxIO(4, 1, 0, 0)
        .disableRegisterNEI()
        .build();

    private IcbmRecipeMaps() {}

    /** Forces this class to load, so the map is built during GregGuns' own preInit. Call once. */
    public static void init() {
        // Loading the class runs the static initializer above.
    }
}
