package com.gregguns.recipe;

import net.minecraft.item.ItemStack;

import com.gregguns.GregGuns;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.objects.ItemData;
import gregtech.api.objects.MaterialStack;
import gregtech.api.util.GTOreDictUnificator;

/**
 * Registers the melee and grenade recipes from {@link MeleeTable} on GT's Assembler and gives the knife and crowbar
 * their GT recycling data. Same timing as {@link GunRecipes}: queued on {@code GregTechAPI.sBeforeGTPostload}. Common
 * side.
 */
// Spec: docs/gregguns-spec/02-guns/recipes.md (LV table)
public final class MeleeRecipes {

    private MeleeRecipes() {}

    /** Queues the registration on GT's before-postload hook. Call from the guns module's init. */
    public static void register() {
        GregTechAPI.sBeforeGTPostload.add(MeleeRecipes::addAll);
    }

    private static void addAll() {
        for (ProductionTable.Row row : MeleeTable.ROWS) {
            ProductionRecipes.addMachineRecipe(row);
        }
        long plate = OrePrefixes.plate.getMaterialAmount();
        long screw = OrePrefixes.screw.getMaterialAmount();
        long longRod = OrePrefixes.stickLong.getMaterialAmount();
        attach(
            MeleeTable.COMBAT_KNIFE,
            new ItemData(Materials.Steel, 2 * plate + 2 * screw, new MaterialStack(Materials.Rubber, plate)));
        attach(MeleeTable.CROWBAR, new ItemData(Materials.Steel, 2 * longRod + plate + screw));
    }

    private static void attach(ProductionTable.Row row, ItemData data) {
        ItemStack stack = ProductionRecipes.modStack(row.output(), 1);
        if (stack == null) {
            GregGuns.LOG.error("Cannot attach ItemData to {}: not registered", row.output());
            return;
        }
        GTOreDictUnificator.addItemData(stack, data);
    }
}
