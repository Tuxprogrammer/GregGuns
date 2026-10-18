package com.gregguns.machine.reaction;

import net.minecraft.item.ItemStack;

import com.gregguns.GregGuns;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.util.GTOreDictUnificator;

/**
 * The two kept Reaction Chamber recipes (Laser Focus, Anti Gravity Core), queued on GT's before-postload hook like the
 * other GregGuns recipes. Common side.
 */
// Spec: docs/gregguns-spec/08-machines-production/recipes.md (Processing recipes), reaction-chamber-multiblock.md
public final class ReactionRecipes {

    /** Eight ingots of molten metal in mB; the sheet's fluid unit. */
    static final int EIGHT_INGOTS = 1152;
    /** Ten reaction ticks of 60 game ticks. */
    static final int DURATION = 600;

    private ReactionRecipes() {}

    /** Queues the registration; call from {@code MachinesModule.init}. */
    public static void register() {
        // Source:
        // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/GTMod.java#L440-L447
        GregTechAPI.sBeforeGTPostload.add(ReactionRecipes::addAll);
    }

    private static void addAll() {
        ItemStack focus = ReactionParts.stack(ReactionParts.HEAT_RAY_FOCUS, 0);
        ItemStack laser = ReactionParts.stack(ReactionParts.LASER_FOCUS, 1);
        ItemStack core = ReactionParts.stack(ReactionParts.ANTI_GRAVITY_CORE, 1);
        ItemStack diamond = GTOreDictUnificator.get(OrePrefixes.gem, Materials.Diamond, 1);
        ItemStack star = GTOreDictUnificator.get(OrePrefixes.gem, Materials.NetherStar, 1);
        if (focus == null || laser == null || core == null || diamond == null || star == null) {
            GregGuns.LOG.error("Skipping Reaction Chamber recipes: an item did not resolve");
            return;
        }
        // Source:
        // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTRecipeBuilder.java#L485-L506
        // (builder usage); a stack size of 0 is GT's non-consumed input (RecipeMap.java#L90-L97). Fluid precedent:
        // AssemblerRecipes.java#L1498-L1503 (molten redstone).
        GTValues.RA.stdBuilder()
            .itemInputs(diamond, focus)
            .itemOutputs(laser)
            .fluidInputs(Materials.Redstone.getMolten(EIGHT_INGOTS))
            .duration(DURATION)
            .eut(TierEU.RECIPE_EV)
            .metadata(ReactionRecipeMaps.PREFERRED_INTENSITY, 3)
            .metadata(ReactionRecipeMaps.INTENSITY_MARGIN, 2)
            .metadata(ReactionRecipeMaps.REQUIRED_COMPLETION, 5)
            .metadata(ReactionRecipeMaps.RISK, ReactionRisk.BREAK_ITEM.name())
            .addTo(ReactionRecipeMaps.REACTION_CHAMBER);
        // Deviation: molten Enderium, because Materials.Ender has no fluid in the pinned GT (recipes.md).
        GTValues.RA.stdBuilder()
            .itemInputs(star, focus)
            .itemOutputs(core)
            .fluidInputs(Materials.Enderium.getMolten(EIGHT_INGOTS))
            .duration(DURATION)
            .eut(TierEU.RECIPE_IV)
            .metadata(ReactionRecipeMaps.PREFERRED_INTENSITY, 8)
            .metadata(ReactionRecipeMaps.INTENSITY_MARGIN, 1)
            .metadata(ReactionRecipeMaps.REQUIRED_COMPLETION, 7)
            .metadata(ReactionRecipeMaps.RISK, ReactionRisk.EXPLOSION_LOW.name())
            .addTo(ReactionRecipeMaps.REACTION_CHAMBER);
    }
}
