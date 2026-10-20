package com.gregguns.recipe;

import static bartworks.API.recipe.BartWorksRecipeMaps.bacterialVatRecipes;
import static gregtech.api.util.GTRecipeBuilder.SECONDS;
import static gregtech.api.util.GTRecipeConstants.GLASS;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import com.gregguns.GregGuns;
import com.gregguns.compat.BartWorksCulture;

import cpw.mods.fml.common.Loader;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.Materials;
import gregtech.api.enums.TierEU;
import gregtech.api.util.GTModHandler;

/**
 * The one Bacterial Vat recipe GregGuns adds: Biomass becomes E. coli culture fluid, the Bio Tank's ammo. The E. coli
 * petri dish sits in the vat controller (a non-consumed special item). Queued on {@code GregTechAPI.sBeforeGTPostload}
 * like the other GregGuns recipes, which runs after BartWorks' init created the culture fluid. Skipped with a log line
 * when BartWorks, its map, the dish or the fluid cannot be resolved. Common side.
 */
// Spec: docs/gregguns-spec/06-items/recipes.md (RC-57), docs/research/pack-integration-audit.md (Option A)
public final class BioCultureRecipes {

    /** Biomass in, and culture fluid out, in mB. */
    public static final int AMOUNT = 1000;
    /** Distilled water in, in mB. */
    public static final int WATER = 100;
    /** The programmed circuit. */
    public static final int CIRCUIT = 2;
    /** The minimum borosilicate glass tier of the vat. */
    public static final int GLASS_TIER = 4;
    /** Recipe time in seconds. */
    public static final int SECONDS_TAKEN = 30;

    private BioCultureRecipes() {}

    /** Queues the recipe on GT's before-postload hook. Call from the guns module's init. */
    public static void register() {
        GregTechAPI.sBeforeGTPostload.add(BioCultureRecipes::addAll);
    }

    private static void addAll() {
        if (!Loader.isModLoaded("bartworks")) {
            GregGuns.LOG.info("Skipping the Bio Tank culture vat recipe: BartWorks is not loaded");
            return;
        }
        try {
            add();
        } catch (Throwable t) {
            GregGuns.LOG.error("Skipping the Bio Tank culture vat recipe: {}", t.toString());
        }
    }

    private static void add() {
        Fluid culture = BartWorksCulture.eColiFluid();
        ItemStack dish = BartWorksCulture.eColiPetriDish();
        FluidStack biomass = Materials.Biomass.getFluid(AMOUNT);
        FluidStack water = GTModHandler.getDistilledWater(WATER);
        if (culture == null || dish == null || biomass == null || water == null || bacterialVatRecipes == null) {
            GregGuns.LOG.error(
                "Skipping the Bio Tank culture vat recipe: culture {}, dish {}, biomass {}, water {}, map {}",
                culture,
                dish,
                biomass,
                water,
                bacterialVatRecipes);
            return;
        }
        // Source: the builder usage and the vat's own recipes (special dish, GLASS metadata, fluid in and out).
        // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/bartworks/common/loaders/BioRecipeLoader.java#L143-L165
        // GLASS is a RecipeMetadataKey<Integer>:
        // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTRecipeConstants.java#L263
        GTValues.RA.stdBuilder()
            .circuit(CIRCUIT)
            .special(dish)
            .fluidInputs(biomass, water)
            .fluidOutputs(new FluidStack(culture, AMOUNT))
            .metadata(GLASS, GLASS_TIER)
            .duration(SECONDS_TAKEN * SECONDS)
            .eut(TierEU.RECIPE_EV)
            .addTo(bacterialVatRecipes);
    }
}
