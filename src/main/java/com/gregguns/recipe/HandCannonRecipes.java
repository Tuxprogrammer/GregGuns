package com.gregguns.recipe;

import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import com.gregguns.GregGuns;
import com.gregguns.gun.def.HandCannonDef;
import com.gregguns.item.ammo.AmmoTypes;
import com.gregguns.item.part.PartItems;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.util.GTModHandler;
import gregtech.api.util.GTOreDictUnificator;

/**
 * The Hand Cannon's crafting-table recipes: the gun itself (no machine exists at ULV) and the stone bullet's hand
 * recipe. The stone bullet's Steam Ammo Press row is a {@link ProductionTable} row (RC-27b); the Stone Barrel is in
 * {@link PartCraftingRecipes}. Common side; {@link #registerGun()} is the {@code recipes} hook of the gun's definition
 * and
 * {@link #registerStoneBulletByHand()} that of the stone bullet's.
 */
// Spec: docs/gregguns-spec/02-guns/recipes.md (Steam / ULV: Hand Cannon), docs/gregguns-spec/06-items/recipes.md
// (RC-27)
// Source:
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTModHandler.java#L626
// (addCraftingRecipe: lowercase letters are tools, strings are ore names, ItemStacks are exact items); its
// shapeless sibling addShapelessCraftingRecipe is in the same class.
public final class HandCannonRecipes {

    private HandCannonRecipes() {}

    /**
     * {@code B F S / . P h}: Stone Barrel, Flint and Steel (any damage), Wooden Stock, a Bronze plate (Wrought Iron is
     * the allowed alternative) and a hard hammer. The output has item damage equal to the clip, which
     * {@code ItemGun.onCreated} turns into an empty gun, as in Techguns.
     */
    public static void registerGun() {
        Item gun = GameRegistry.findItem(GregGuns.MODID, HandCannonDef.NAME);
        ItemStack barrel = ProductionRecipes.modStack(PartItems.BARREL_STONE, 1);
        ItemStack stock = ProductionRecipes.modStack(PartItems.STOCK_WOODEN, 1);
        if (gun == null || barrel == null || stock == null) {
            GregGuns.LOG.error("Skipping the Hand Cannon recipe: the gun, Stone Barrel or Wooden Stock is missing");
            return;
        }
        ItemStack output = new ItemStack(gun, 1, HandCannonDef.STATS.magazineSize());
        ItemStack flintAndSteel = new ItemStack(Items.flint_and_steel, 1, OreDictionary.WILDCARD_VALUE);
        boolean any = false;
        for (Materials material : new Materials[] { Materials.Bronze, Materials.WroughtIron }) {
            ItemStack plate = GTOreDictUnificator.get(OrePrefixes.plate, material, 1);
            if (plate == null) {
                continue;
            }
            GTModHandler.addCraftingRecipe(
                output.copy(),
                new Object[] { "BFS", " Ph", 'B', barrel, 'F', flintAndSteel, 'S', stock, 'P', plate });
            any = true;
        }
        if (!any) {
            GregGuns.LOG.error("Skipping the Hand Cannon recipe: no Bronze or Wrought Iron plate resolved");
        }
    }

    /** RC-27: three cobblestone, a pinch of gunpowder and a Bronze nugget make 16 stone bullets, shapeless. */
    public static void registerStoneBulletByHand() {
        ItemStack bullets = ProductionRecipes.modStack(AmmoTypes.STONE.roundItemName(), 16);
        ItemStack nugget = GTOreDictUnificator.get(OrePrefixes.nugget, Materials.Bronze, 1);
        ItemStack gunpowder = GTOreDictUnificator.get(OrePrefixes.dust, Materials.Gunpowder, 1);
        if (bullets == null || nugget == null || gunpowder == null) {
            GregGuns.LOG.error("Skipping RC-27: the stone bullet, Bronze nugget or gunpowder did not resolve");
            return;
        }
        GTModHandler.addShapelessCraftingRecipe(
            bullets,
            new Object[] { "cobblestone", "cobblestone", "cobblestone", gunpowder, nugget });
    }
}
