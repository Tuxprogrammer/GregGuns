package com.gregguns.recipe;

import net.minecraft.item.Item;
import net.minecraftforge.oredict.RecipeSorter;

import com.gregguns.GregGuns;
import com.gregguns.armor.ArmorParts;
import com.gregguns.armor.slots.EquipmentItems;
import com.gregguns.armor.slots.EquipmentRefillRecipe;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.GregTechAPI;

/**
 * Registers the special-slot equipment recipes from {@link EquipmentTable}: the Assembler rows (queued on GT's
 * before-postload hook like the other armor rows) and the two shapeless refills, Gas Mask plus Filter and Oxygen Tanks
 * plus Compressed Air Tank. Common side.
 */
// Spec: docs/gregguns-spec/05-armor/recipes.md (Gas Mask refilter, GGS-05-37)
public final class EquipmentRecipes {

    private EquipmentRecipes() {}

    /** Adds the crafting-table refills now and queues the Assembler rows. Call from the armor module's init. */
    public static void register() {
        addRefill(EquipmentItems.gasMask(), ArmorParts.gasMaskFilter());
        addRefill(EquipmentItems.oxygenTanks(), ArmorParts.tankAir());
        // Source:
        // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/GTMod.java#L440-L447
        GregTechAPI.sBeforeGTPostload.add(EquipmentRecipes::addMachineRecipes);
    }

    private static void addRefill(Item equipment, Item refill) {
        if (equipment == null || refill == null) {
            GregGuns.LOG.error("Skipping a refill recipe: an item is not registered");
            return;
        }
        // Source: Forge 1.7.10 RecipeSorter.register; without it FML logs "Unknown recipe class" at load.
        RecipeSorter
            .register("gregguns:equipment_refill", EquipmentRefillRecipe.class, RecipeSorter.Category.SHAPELESS, "");
        // Source: Forge 1.7.10 GameRegistry.addRecipe(IRecipe)
        GameRegistry.addRecipe(new EquipmentRefillRecipe(equipment, refill));
    }

    private static void addMachineRecipes() {
        for (ProductionTable.Row row : EquipmentTable.ROWS) {
            ProductionRecipes.addMachineRecipe(row);
        }
    }
}
