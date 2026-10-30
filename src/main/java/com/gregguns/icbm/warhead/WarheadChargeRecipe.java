package com.gregguns.icbm.warhead;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;

import com.gregguns.icbm.explosive.Explosives;
import com.gregguns.icbm.recipe.IcbmPart;
import com.gregguns.icbm.recipe.IcbmParts;

/**
 * Shapeless crafting, the D23 knob (D282): one inert warhead, or one live warhead that is not full, plus 1 to 8
 * Plastic Explosive gives the live warhead of that casing with that many more charges. The payload of an inert warhead
 * becomes the explosive; a bare T1 casing becomes condensed. A bare T2 to T5 casing, a load past the cap or anything
 * else in the grid makes no recipe ({@link WarheadCharging}), so nothing is ever lost. Common side.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/warheads-and-fill.md (The warhead chain)
// Source: Forge 1.7.10 net.minecraft.item.crafting.IRecipe (matches, getCraftingResult, getRecipeSize, getRecipeOutput)
public final class WarheadChargeRecipe implements IRecipe {

    @Override
    public boolean matches(InventoryCrafting grid, World world) {
        return result(grid) != null;
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting grid) {
        return result(grid);
    }

    /**
     * @param grid the crafting grid
     * @return the loaded warhead, or null when the grid is not a valid load
     */
    static ItemStack result(InventoryCrafting grid) {
        Item charge = IcbmParts.get(IcbmPart.PLASTIC_EXPLOSIVE);
        ItemStack warhead = null;
        int charges = 0;
        for (int i = 0; i < grid.getSizeInventory(); i++) {
            ItemStack s = grid.getStackInSlot(i);
            if (s == null) {
                continue;
            }
            if (charge != null && s.getItem() == charge) {
                charges++;
            } else if (warhead == null && (InertWarheads.is(s) || s.getItem() instanceof ItemBlockWarhead)) {
                warhead = s;
            } else {
                return null;
            }
        }
        return warhead == null ? null : load(warhead, charges);
    }

    /**
     * @param warhead an inert or live warhead stack
     * @param charges the Plastic Explosive added
     * @return the live warhead after the load, or null when the load is refused
     */
    public static ItemStack load(ItemStack warhead, int charges) {
        WarheadCasing casing;
        String explosive;
        int fill;
        if (InertWarheads.is(warhead)) {
            casing = InertWarheads.casing(warhead);
            explosive = WarheadCharging.explosiveFor(casing, InertWarheads.payload(warhead));
            fill = 0;
        } else if (warhead != null && warhead.getItem() instanceof ItemBlockWarhead) {
            casing = ((ItemBlockWarhead) warhead.getItem()).casing();
            WarheadData data = WarheadStacks.read(casing, warhead);
            explosive = data.explosive();
            fill = data.fill();
        } else {
            return null;
        }
        if (explosive == null) {
            return null;
        }
        int cap = WarheadStacks.cap(casing, Explosives.get(explosive));
        int loaded = WarheadCharging.loadedFill(fill, charges, cap);
        if (loaded < 0) {
            return null;
        }
        return WarheadStacks.make(Item.getItemFromBlock(WarheadBlocks.of(casing)), casing, explosive, loaded);
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
