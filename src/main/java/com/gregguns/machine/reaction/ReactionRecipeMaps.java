package com.gregguns.machine.reaction;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.util.StatCollector;

import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMapBackend;
import gregtech.api.recipe.RecipeMapBuilder;
import gregtech.api.recipe.RecipeMetadataKey;
import gregtech.api.recipe.metadata.SimpleRecipeMetadataKey;
import gregtech.api.util.GTRecipe;

/**
 * The Reaction Chamber's GT recipe map and the four metadata keys every recipe in it carries.
 *
 * <p>
 * Load this class from {@code MachinesModule.preInit} through {@link #init()}, so GT records GregGuns as the map's
 * owner mod (NEI badge). NEI needs no code from us: GT makes a tab titled by the lang key equal to the map name and
 * attaches every machine that reports the map as a catalyst. Common side.
 */
// Spec: docs/gregguns-spec/08-machines-production/reaction-chamber-multiblock.md (Recipe map, GGS-08-18),
// docs/gregguns-spec/08-machines-production/nei-handlers.md (GGS-08-32, GGS-08-33)
public final class ReactionRecipeMaps {

    /** The map's name; also its NEI handler id and the lang key of the tab title. */
    public static final String NAME = "gregguns.recipe.reactionchamber";

    // Source:
    // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/recipe/metadata/SimpleRecipeMetadataKey.java
    // (a key that draws nothing itself; the map's special-info formatter below shows the values in NEI).
    /** The intensity setting a recipe needs at its start (0 to 10). */
    public static final RecipeMetadataKey<Integer> PREFERRED_INTENSITY = SimpleRecipeMetadataKey
        .create(Integer.class, "gregguns_rc_intensity");
    /** How far the preferred intensity may drift either way during the run. */
    public static final RecipeMetadataKey<Integer> INTENSITY_MARGIN = SimpleRecipeMetadataKey
        .create(Integer.class, "gregguns_rc_margin");
    /** Good reaction ticks needed. */
    public static final RecipeMetadataKey<Integer> REQUIRED_COMPLETION = SimpleRecipeMetadataKey
        .create(Integer.class, "gregguns_rc_required");
    /** The {@link ReactionRisk} name applied on failure. */
    public static final RecipeMetadataKey<String> RISK = SimpleRecipeMetadataKey
        .create(String.class, "gregguns_rc_risk");

    /**
     * Two item inputs (the input and the non-consumed focus), four item outputs, one fluid input.
     */
    // Source:
    // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/recipe/RecipeMapBuilder.java#L186
    // (maxIO) and #L100 (minInputs); neiSpecialInfoFormatter at #L466.
    public static final RecipeMap<RecipeMapBackend> REACTION_CHAMBER = RecipeMapBuilder.of(NAME)
        .maxIO(2, 4, 1, 0)
        .minInputs(1, 0)
        .neiSpecialInfoFormatter(info -> describe(info.recipe))
        .build();

    private ReactionRecipeMaps() {}

    /** Forces this class to load, so the map is built during GregGuns' own preInit. Call once. */
    public static void init() {
        // Loading the class runs the static initializer above.
    }

    private static List<String> describe(GTRecipe recipe) {
        List<String> lines = new ArrayList<>();
        lines.add(
            StatCollector.translateToLocalFormatted(
                "gregguns.reaction.nei.intensity",
                recipe.getMetadataOrDefault(PREFERRED_INTENSITY, 0),
                recipe.getMetadataOrDefault(INTENSITY_MARGIN, 0)));
        lines.add(
            StatCollector.translateToLocalFormatted(
                "gregguns.reaction.nei.completion",
                recipe.getMetadataOrDefault(REQUIRED_COMPLETION, 0),
                recipe.mDuration / ReactionState.REACTION_TICK));
        lines.add(
            StatCollector.translateToLocal(
                "gregguns.reaction.risk." + recipe.getMetadataOrDefault(RISK, ReactionRisk.NONE.name())
                    .toLowerCase()));
        return lines;
    }
}
