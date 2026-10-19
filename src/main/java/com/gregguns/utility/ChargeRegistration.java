package com.gregguns.utility;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

import com.gregguns.GregGuns;
import com.gregguns.core.registry.ModuleRegistrar;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.utility.charge.BlockExplosiveCharge;
import com.gregguns.utility.charge.ChargeRecipeSpec;
import com.gregguns.utility.charge.ChargeScenarios;
import com.gregguns.utility.charge.ItemBlockCharge;
import com.gregguns.utility.charge.TileExplosiveCharge;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;

/**
 * Registration of the explosive charges (chunk m6/charges): the block with its two metas, the tile entity, the
 * Assembler recipes and the dev scenarios. {@link UtilityModule} calls the three hooks. Common side.
 */
// Spec: docs/ROADMAP.md (M6), docs/gregguns-spec/09-machines-utility/explosive-charges.md,
// recipes.md (GGS-09-31, GGS-09-32)
public final class ChargeRegistration {

    private ChargeRegistration() {}

    /**
     * Config-dependent registration: the charge block and its tile entity.
     *
     * @param registrar the utility module's registrar
     */
    public static void preInit(ModuleRegistrar registrar) {
        BlockExplosiveCharge block = registrar
            .registerBlock(BlockExplosiveCharge.NAME, new BlockExplosiveCharge(), ItemBlockCharge.class);
        // Placeholder: no charge texture exists yet, so the particle/icon uses the closest block texture (asset gap).
        block.setBlockTextureName("gregguns:casings/gantry_casing");
        BlockExplosiveCharge.instance = block;
        registrar.registerTileEntity("explosive_charge", TileExplosiveCharge.class);
    }

    /** Recipes, queued for GT's before-postload hook when its materials and item lists are filled. */
    public static void init() {
        GregTechAPI.sBeforeGTPostload.add(ChargeRegistration::addRecipes);
    }

    private static void addRecipes() {
        for (ChargeRecipeSpec spec : ChargeRecipeSpec.values()) {
            ItemStack sheets = GTOreDictUnificator
                .get(OrePrefixes.plate, Materials.get(spec.sheetMaterial()), spec.sheetCount());
            ItemStack circuit = GTOreDictUnificator.get(OrePrefixes.circuit, Materials.get(spec.circuitMaterial()), 1);
            ItemStack gate = GTOreDictUnificator.get(OrePrefixes.plate, Materials.get(spec.gatePlateMaterial()), 1);
            if (sheets == null || circuit == null || gate == null) {
                GregGuns.LOG.warn(
                    "Skipping the {} recipe: a GT input is missing (sheets {}, circuit {}, plate {})",
                    spec.type(),
                    sheets != null,
                    circuit != null,
                    gate != null);
                continue;
            }
            ItemStack out = new ItemStack(
                BlockExplosiveCharge.instance,
                spec.outputCount(),
                spec.type()
                    .meta());
            // Source:
            // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/postload/recipes/AssemblerRecipes.java#L606-L614
            // (the Plascrete block is the precedent for a plate-and-circuit Assembler recipe with a fluid).
            gregtech.api.util.GTRecipeBuilder builder = GTValues.RA.stdBuilder()
                .itemInputs(new ItemStack(Blocks.tnt, ChargeRecipeSpec.TNT_COUNT), sheets, circuit, gate)
                .itemOutputs(out)
                .duration(ChargeRecipeSpec.DURATION_TICKS)
                .eut(spec.eut());
            if (spec.fluidMillibuckets() > 0) {
                builder = builder.fluidInputs(Materials.Glyceryl.getFluid(spec.fluidMillibuckets()));
            }
            builder.addTo(RecipeMaps.assemblerRecipes);
        }
    }

    /**
     * Dev-only scenarios.
     *
     * @param registry where to add them
     */
    public static void registerScenarios(ScenarioRegistry registry) {
        ChargeScenarios.registerAll(registry);
    }
}
