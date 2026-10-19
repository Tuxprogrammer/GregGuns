package com.gregguns.utility;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.item.ItemStack;

import com.gregguns.GregGuns;
import com.gregguns.core.registry.ModuleRegistrar;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTModHandler;
import gregtech.api.util.GTOreDictUnificator;

/**
 * Registers the four turret armor plates and their recipes. The Turret reads a plate for its armor value; the Rampart's
 * Bastion Plating consumes one per four blocks. Common side.
 *
 * <p>
 * Usage: {@code UtilityParts.plate(TurretPlate.STEEL)} is the Steel Turret Armor item; null while the utility module is
 * off.
 */
// Spec: docs/gregguns-spec/09-machines-utility/recipes.md (Turret armor plates, GGS-09-26, GGS-09-29, GGS-09-30)
public final class UtilityParts {

    private static final Map<TurretPlate, ItemTurretArmor> PLATES = new EnumMap<>(TurretPlate.class);

    private UtilityParts() {}

    /**
     * Registers the four plates. Call from {@code UtilityModule.preInit}.
     *
     * @param registrar the utility module's registrar
     */
    public static void register(ModuleRegistrar registrar) {
        for (TurretPlate plate : TurretPlate.values()) {
            PLATES.put(plate, registrar.registerItem(plate.registryName(), new ItemTurretArmor(plate)));
        }
    }

    /**
     * @param plate which plate
     * @return its item; null before registration or with the utility module off
     */
    public static ItemTurretArmor plate(TurretPlate plate) {
        return PLATES.get(plate);
    }

    /** Queues the plate recipes on GT's before-postload hook, when its materials and item lists are filled. */
    public static void registerRecipes() {
        GregTechAPI.sBeforeGTPostload.add(UtilityParts::addPlateRecipes);
    }

    private static void addPlateRecipes() {
        for (TurretPlate plate : TurretPlate.values()) {
            ItemStack main = GTOreDictUnificator
                .get(OrePrefixes.plate, Materials.get(plate.mainMaterial()), plate.mainCount());
            ItemStack extra = plate.extraMaterial() == null ? null
                : GTOreDictUnificator.get(OrePrefixes.plate, Materials.get(plate.extraMaterial()), plate.extraCount());
            if (main == null || plate.extraMaterial() != null && extra == null) {
                // GGS-09-26 / GGS-06-11: a missing GT part skips only this recipe.
                GregGuns.LOG.warn("Skipping the {} recipe: a GT plate input is missing", plate.registryName());
                continue;
            }
            ItemStack out = new ItemStack(PLATES.get(plate));
            if (plate.handMade()) {
                // Source (GT's own hand-made armor is the precedent for a shaped plate recipe):
                // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/postload/CraftingRecipeLoader.java#L350-L368
                GTModHandler.addCraftingRecipe(
                    out,
                    GTModHandler.RecipeBits.BITS | GTModHandler.RecipeBits.DO_NOT_CHECK_FOR_COLLISIONS,
                    new Object[] { "PPP", " P ", " P ", 'P',
                        OrePrefixes.plate.get(Materials.get(plate.mainMaterial())) });
            } else {
                ItemStack[] inputs = extra == null ? new ItemStack[] { main } : new ItemStack[] { main, extra };
                GTValues.RA.stdBuilder()
                    .itemInputs(inputs)
                    .itemOutputs(out)
                    .duration(plate.durationTicks())
                    .eut(plate.recipeEut())
                    .addTo(RecipeMaps.assemblerRecipes);
            }
        }
    }
}
