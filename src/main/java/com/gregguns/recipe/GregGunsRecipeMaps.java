package com.gregguns.recipe;

import gregtech.api.modularui2.GTGuiTextures;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMapBackend;
import gregtech.api.recipe.RecipeMapBuilder;

/**
 * GregGuns' own GT recipe maps.
 *
 * <p>
 * Load this class from {@code GunsModule.preInit} (through {@link #init()}), not later. GT records the active mod
 * container as the owner of a recipe category when the map is built, and NEI uses it for the tab's badge, so building
 * the map inside a GT callback would label it a GregTech tab
 * (docs/gregguns-spec/08-machines-production/nei-handlers.md, GGS-08-32).
 *
 * <p>
 * NEI needs no code from us: GT creates a handler for every map whose NEI registration is on, titled with the lang key
 * equal to the map name ({@code gregguns.ammoPress} in {@code en_US.lang}), and attaches every machine that reports the
 * map from {@code getRecipeMap()} as a catalyst. Common side.
 */
// Spec: docs/gregguns-spec/08-machines-production/ammo-press.md (The recipe map), docs/design/0011-machine-policy.md
public final class GregGunsRecipeMaps {

    /** The unlocalised name of the ammo map. It is also the NEI handler id and the lang key of the tab title. */
    public static final String AMMO_PRESS_NAME = "gregguns.ammoPress";

    /**
     * Every ammo and magazine recipe, run by the Ammo Press single blocks (and later the Ammunition Works).
     * Slots: up to 6 item inputs, 3 item outputs, 1 fluid input, no fluid output.
     */
    // Source:
    // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/recipe/RecipeMapBuilder.java#L83-L85
    // (of(name)) and #L186 (maxIO(itemIn, itemOut, fluidIn, fluidOut)); the name must be unique across all maps.
    // Deviation: the sheet's own stroke-shaped progress bar is M4 work; the map keeps GT's default arrow until then.
    // Steam GUI: GT's bronze and steel steam machines read the map's steam GUI textures when their screen is built
    // (MTEBasicMachineBaseGui's constructor calls progressBarTextureSteamMUI2.get(variant), and every slot overlay
    // getter's result is read the same way). BasicUIPropertiesBuilder leaves progressBarTextureSteamMUI2 null and the
    // steam slot overlay getter returning null, so a map that sets neither throws a NullPointerException when the Steam
    // Ammo Press is opened, and no GUI appears. GT's own maps set both (RecipeMaps.java#L204-L207 for the compressor).
    // Source:
    // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/gui/modularui/singleblock/base/MTEBasicMachineBaseGui.java#L77-L82
    // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/recipe/BasicUIPropertiesBuilder.java#L79-L83
    public static final RecipeMap<RecipeMapBackend> AMMO_PRESS = RecipeMapBuilder.of(AMMO_PRESS_NAME)
        .maxIO(6, 3, 1, 0)
        .progressBarTextureSteamMUI2(GTGuiTextures.PROGRESSBAR_ARROW_STEAM)
        .slotOverlaysSteamMUI2((index, isFluid, isOutput, isSpecial) -> null)
        .build();

    private GregGunsRecipeMaps() {}

    /** Forces this class to load, so the maps are built during GregGuns' own preInit. Call once. */
    public static void init() {
        // Loading the class runs the static initializer above; there is nothing else to do.
    }
}
