package com.gregguns.recipe;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;

import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.gun.logic.GunCatalog;
import com.gregguns.gun.sound.GunSoundEvents;
import com.gregguns.item.part.PartItems;

import gregtech.api.enums.GTValues;
import gregtech.api.enums.TierEU;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTRecipeBuilder;

/**
 * {@code gun.handcannon.recipe}: the Hand Cannon has a crafting-table recipe that yields an empty gun, its Stone
 * Barrel and stone bullets have hand recipes, and the Steam Ammo Press map holds the stone bullet row at ULV with at
 * most 4 inputs and 1 output. Dev environment only, server side.
 */
// Spec: docs/gregguns-spec/02-guns/recipes.md (Steam / ULV), docs/gregguns-spec/06-items/recipes.md (RC-01, RC-27,
// RC-27b)
final class HandCannonRecipeScenario implements Scenario {

    @Override
    public String name() {
        return "gun.handcannon.recipe";
    }

    @Override
    public String description() {
        return "The Hand Cannon is a crafting-table recipe giving an empty gun; the Stone Barrel and stone bullets have hand recipes; the stone bullet press row is in the ammo map at ULV with 24 bullets from at most 4 inputs.";
    }

    @Override
    public ScenarioResult run(ScenarioContext ctx) {
        ScenarioResult.Builder out = ScenarioResult.builder(name());
        ItemStack gun = ProductionRecipes.modStack(GunSoundEvents.HANDCANNON, 1);
        ItemStack barrel = ProductionRecipes.modStack(PartItems.BARREL_STONE, 1);
        ItemStack bullet = ProductionRecipes.modStack(ProductionTable.STONE_BULLET, 1);
        out.check(gun != null && barrel != null && bullet != null, "the gun, barrel and bullet items must exist");
        if (gun == null || barrel == null || bullet == null) {
            return out.build();
        }
        ItemStack gunRecipe = craftingResult(gun);
        out.check(gunRecipe != null, "a crafting recipe should make the Hand Cannon");
        if (gunRecipe != null) {
            out.checkEquals(
                "crafted gun item damage (rounds missing: empty)",
                GunCatalog.HANDCANNON.magazineSize(),
                gunRecipe.getItemDamage());
        }
        out.check(craftingResult(barrel) != null, "a crafting recipe should make the Stone Barrel");
        ItemStack bulletRecipe = craftingResult(bullet);
        out.check(bulletRecipe != null, "a crafting recipe should make stone bullets");
        if (bulletRecipe != null) {
            out.checkEquals("stone bullets per hand recipe", 16, bulletRecipe.stackSize);
        }

        RecipeMap<?> map = GregGunsRecipeMaps.AMMO_PRESS;
        int found = 0;
        for (GTRecipe recipe : map.getAllRecipes()) {
            if (recipe.mOutputs != null && recipe.mOutputs.length > 0
                && recipe.mOutputs[0] != null
                && recipe.mOutputs[0].getItem() == bullet.getItem()) {
                found++;
                out.checkEquals("press EU/t (ULV)", (int) TierEU.RECIPE_ULV, recipe.mEUt);
                out.checkEquals("press duration", 5 * GTRecipeBuilder.SECONDS * ProductionTable.AMMO_DURATION_MULTIPLIER, recipe.mDuration);
                out.checkEquals("press outputs", 1, recipe.mOutputs.length);
                out.checkEquals(
                    "stone bullets per press run",
                    ProductionTable.STONE_BULLETS_PER_RUN,
                    recipe.mOutputs[0].stackSize);
                out.check(
                    recipe.mInputs.length <= 4,
                    "the Steam press has 4 input slots but the recipe has " + recipe.mInputs.length);
                out.check(
                    GTValues.V[0] >= recipe.mEUt,
                    "a ULV machine's voltage must cover the recipe's EU/t " + recipe.mEUt);
            }
        }
        out.checkEquals("stone bullet recipes in the ammo map", 1, found);
        return out.build();
    }

    private static ItemStack craftingResult(ItemStack wanted) {
        for (Object recipe : CraftingManager.getInstance()
            .getRecipeList()) {
            ItemStack result = ((IRecipe) recipe).getRecipeOutput();
            if (result != null && result.getItem() == wanted.getItem()) {
                return result;
            }
        }
        return null;
    }
}
