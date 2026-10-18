package com.gregguns.recipe;

import net.minecraft.item.ItemStack;

import com.gregguns.GregGuns;
import com.gregguns.item.part.PartItems;

import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.util.GTModHandler;
import gregtech.api.util.GTOreDictUnificator;

/**
 * The ULV parts made at the crafting table with GT tools: the Stone Barrel (RC-01), the Wooden Stock (RC-02) and
 * Mechanical Parts (RC-03). They are the Revolver's and Hand Cannon's hand-made inputs; every machine-made part is a
 * {@link ProductionTable} row instead.
 *
 * <p>
 * The tool letters are GT's own ({@code s} saw, {@code h} hard hammer, {@code f} file): {@code GTModHandler} turns
 * them into tool ore-dictionary entries and damages the tool on craft. Mechanical Parts are registered once per
 * material that exists (Bronze, then Wrought Iron). Common side; each method is the {@code recipes} hook of its
 * part's {@code PartDefinition}.
 */
// Spec: docs/gregguns-spec/06-items/recipes.md (RC-01, RC-02, RC-03)
// Source:
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTModHandler.java#L626
// (addCraftingRecipe: lowercase letters are tools, strings are ore names, ItemStacks are exact items).
public final class PartCraftingRecipes {

    private PartCraftingRecipes() {}

    /** RC-02: the Wooden Stock, two cuts of log with a saw. */
    public static void registerWoodenStock() {
        ItemStack stock = ProductionRecipes.modStack(PartItems.STOCK_WOODEN, 1);
        if (stock == null) {
            GregGuns.LOG.error("Skipping RC-02: {} is not registered", PartItems.STOCK_WOODEN);
        } else {
            GTModHandler.addCraftingRecipe(stock, new Object[] { "WW", "sW", 'W', "logWood" });
        }
    }

    /** RC-03: Mechanical Parts, once per material that exists (Bronze, then Wrought Iron). */
    public static void registerMechanicalParts() {
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

    /**
     * RC-01: six stone around a Bronze tiny pipe (the bore liner and the gate), with a file and a hard hammer on the
     * middle row: {@code SSS / fPh / SSS}.
     */
    public static void registerStoneBarrel() {
        ItemStack barrel = ProductionRecipes.modStack(PartItems.BARREL_STONE, 1);
        ItemStack pipe = GTOreDictUnificator.get(OrePrefixes.pipeTiny, Materials.Bronze, 1);
        if (barrel == null || pipe == null) {
            GregGuns.LOG.error("Skipping RC-01: {} or the Bronze tiny pipe did not resolve", PartItems.BARREL_STONE);
            return;
        }
        GTModHandler.addCraftingRecipe(barrel, new Object[] { "SSS", "fPh", "SSS", 'S', "stone", 'P', pipe });
    }
}
