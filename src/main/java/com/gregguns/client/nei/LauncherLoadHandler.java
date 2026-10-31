package com.gregguns.client.nei;

import java.util.Arrays;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import com.gregguns.icbm.launcher.ItemLauncherWarhead;
import com.gregguns.icbm.launcher.LauncherLoads;
import com.gregguns.icbm.launcher.LauncherRules;
import com.gregguns.icbm.launcher.LauncherWarhead;
import com.gregguns.icbm.launcher.LauncherWarheadRecipe;
import com.gregguns.icbm.launcher.LauncherWarheadRegistration;
import com.gregguns.icbm.launcher.LauncherWarheads;
import com.gregguns.item.gun.GunItems;
import com.gregguns.item.gun.ItemGun;

import codechicken.nei.recipe.ShapelessRecipeHandler;

/**
 * The NEI page of the launcher-warhead load (D227, D338): one page per launcher and warhead of its family, the empty
 * launcher and the warhead in the crafting grid, the launcher with the warhead's charges loaded as the result.
 * {@link LauncherWarheadRecipe} is a custom recipe that NEI's own crafting pages cannot list, so this handler draws its
 * loads from {@link LauncherLoads}. It is a shapeless crafting page, so the "?" button fills a crafting grid as on any
 * other. Look up the launcher's recipes or the warhead's uses to reach it. Client only.
 */
// Source: NEI's shapeless crafting page, whose cached recipe this reuses:
// https://github.com/GTNewHorizons/NotEnoughItems/blob/2.8.155-GTNH/src/main/java/codechicken/nei/recipe/ShapelessRecipeHandler.java
// Spec: docs/gregguns-icbm-spec/02-explosives/launcher-warheads.md (NEI)
public class LauncherLoadHandler extends ShapelessRecipeHandler {

    /** NEI's output id for every page of this handler: its title's lang key. */
    static final String ID = "gregguns.nei.launcher_load";

    @Override
    public String getRecipeName() {
        return StatCollector.translateToLocal("gregguns.nei.launcher_load");
    }

    @Override
    public void loadCraftingRecipes(String outputId, Object... results) {
        // Every page for our own id and for the crafting table's "recipes" arrow; the item lookups go through super.
        if (ID.equals(outputId) || "crafting".equals(outputId)) {
            add(null, null);
        } else {
            super.loadCraftingRecipes(outputId, results);
        }
    }

    @Override
    public void loadCraftingRecipes(ItemStack result) {
        String gun = launcherName(result);
        if (gun != null) {
            // A loaded launcher shows how its warhead got there; an empty one, every warhead it takes.
            add(gun, LauncherWarheads.loaded(result));
        }
    }

    @Override
    public void loadUsageRecipes(ItemStack ingredient) {
        if (ingredient == null) {
            return;
        }
        if (ingredient.getItem() instanceof ItemLauncherWarhead) {
            add(null, ((ItemLauncherWarhead) ingredient.getItem()).warhead());
        } else {
            String gun = launcherName(ingredient);
            if (gun != null) {
                add(gun, null);
            }
        }
    }

    private void add(String gun, LauncherWarhead warhead) {
        for (LauncherLoads.Load l : LauncherLoads.matching(gun, warhead)) {
            ItemGun item = GunItems.get(l.gun());
            ItemStack load = LauncherWarheadRegistration.stack(l.warhead());
            if (item == null || load == null) {
                continue; // a launcher or warhead this run did not register
            }
            ItemStack out = new ItemStack(item);
            LauncherWarheads.set(out, l.warhead(), LauncherRules.CHARGES_PER_ITEM);
            CachedShapelessRecipe page = new CachedShapelessRecipe(Arrays.asList(new ItemStack(item), load), out);
            arecipes.add(page);
        }
    }

    /** @return the registry name (no mod id) of a launcher stack, or null for anything else */
    private static String launcherName(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof ItemGun)) {
            return null;
        }
        String name = Item.itemRegistry.getNameForObject(stack.getItem());
        if (name == null) {
            return null;
        }
        String bare = name.substring(name.indexOf(':') + 1);
        return LauncherLoads.matching(bare, null)
            .isEmpty() ? null : bare;
    }
}
