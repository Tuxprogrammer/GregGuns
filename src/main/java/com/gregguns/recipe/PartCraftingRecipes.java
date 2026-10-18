package com.gregguns.recipe;

import net.minecraft.item.ItemStack;

import com.gregguns.GregGuns;
import com.gregguns.item.part.PartItems;

import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.util.GTModHandler;
import gregtech.api.util.GTOreDictUnificator;

/**
 * The ULV parts made at the crafting table with GT tools: the Wooden Stock (RC-02) and Mechanical Parts (RC-03). They
 * are the Revolver's hand-made inputs; every machine-made part is a {@link ProductionTable} row instead.
 *
 * <p>
 * The tool letters are GT's own ({@code s} saw, {@code h} hard hammer, {@code f} file): {@code GTModHandler} turns
 * them into tool ore-dictionary entries and damages the tool on craft. Mechanical Parts are registered once per
 * material that exists (Bronze, then Wrought Iron). Common side; called from {@link GunRecipes}.
 */
// Spec: docs/gregguns-spec/06-items/recipes.md (RC-02, RC-03)
// Source:
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTModHandler.java#L626
// (addCraftingRecipe: lowercase letters are tools, strings are ore names, ItemStacks are exact items).
final class PartCraftingRecipes {

    private PartCraftingRecipes() {}

    /** Registers RC-02 and RC-03. */
    static void register() {
        ItemStack stock = ProductionRecipes.modStack(PartItems.STOCK_WOODEN, 1);
        if (stock == null) {
            GregGuns.LOG.error("Skipping RC-02: {} is not registered", PartItems.STOCK_WOODEN);
        } else {
            GTModHandler.addCraftingRecipe(stock, new Object[] { "WW", "sW", 'W', "logWood" });
        }
        ItemStack mechanical = ProductionRecipes.modStack(PartItems.MECHANICAL_1, 1);
        if (mechanical == null) {
            GregGuns.LOG.error("Skipping RC-03: {} is not registered", PartItems.MECHANICAL_1);
            return;
        }
        boolean any = false;
        for (Materials material : new Materials[] { Materials.Bronze, Materials.WroughtIron }) {
            ItemStack gear = GTOreDictUnificator.get(OrePrefixes.gearGtSmall, material, 1);
            ItemStack spring = GTOreDictUnificator.get(OrePrefixes.springSmall, material, 1);
            if (gear == null || spring == null) {
                continue;
            }
            GTModHandler.addCraftingRecipe(mechanical, new Object[] { "hGf", "GSG", " G ", 'G', gear, 'S', spring });
            any = true;
        }
        if (!any) {
            GregGuns.LOG.error("Skipping RC-03: no Bronze or Wrought Iron gear and spring resolved");
        }
    }
}
