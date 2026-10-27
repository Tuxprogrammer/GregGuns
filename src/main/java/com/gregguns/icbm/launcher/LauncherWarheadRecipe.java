package com.gregguns.icbm.launcher;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;

import com.gregguns.item.gun.ItemGun;

/**
 * Shapeless crafting: one launcher plus one launcher warhead of its family gives the launcher with the warhead loaded
 * (D150). The launcher keeps every other tag (ammo, camo, charge). The same type adds charges up to
 * {@link LauncherRules#MAX_CHARGES}; a different type, or a load that would pass the cap, makes no recipe (nothing is
 * ever lost). Common side.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/launcher-warheads.md
// Source: Forge 1.7.10 net.minecraft.item.crafting.IRecipe (matches, getCraftingResult, getRecipeSize, getRecipeOutput)
public final class LauncherWarheadRecipe implements IRecipe {

    @Override
    public boolean matches(InventoryCrafting grid, World world) {
        return result(grid) != null;
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting grid) {
        return result(grid);
    }

    private static ItemStack result(InventoryCrafting grid) {
        ItemStack gun = null;
        LauncherWarhead add = null;
        for (int i = 0; i < grid.getSizeInventory(); i++) {
            ItemStack s = grid.getStackInSlot(i);
            if (s == null) {
                continue;
            }
            if (s.getItem() instanceof ItemLauncherWarhead && add == null) {
                add = ((ItemLauncherWarhead) s.getItem()).warhead();
            } else if (s.getItem() instanceof ItemGun && gun == null) {
                gun = s;
            } else {
                return null;
            }
        }
        if (gun == null || add == null
            || !add.kind()
                .fits(
                    ((ItemGun) gun.getItem()).stats()
                        .id())) {
            return null;
        }
        LauncherWarhead have = LauncherWarheads.loaded(gun);
        int charges = LauncherRules.combine(have == null ? null : have.key(), LauncherWarheads.charges(gun), add.key());
        if (charges < 0) {
            return null;
        }
        ItemStack out = gun.copy();
        out.stackSize = 1;
        LauncherWarheads.set(out, add, charges);
        return out;
    }

    @Override
    public int getRecipeSize() {
        return 2;
    }

    @Override
    public ItemStack getRecipeOutput() {
        return null;
    }
}
