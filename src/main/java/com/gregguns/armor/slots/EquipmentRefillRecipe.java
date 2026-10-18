package com.gregguns.armor.slots;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;

import com.gregguns.armor.slots.logic.RefilterRule;

/**
 * Shapeless crafting recipe: one piece of equipment of any damage plus one refill item gives the equipment at damage 0
 * (a Gas Mask with a Gas Mask Filter, Oxygen Tanks with a Compressed Air Tank). The output carries none of the input's
 * NBT (GGS-05-34, GGS-05-37). Common side.
 */
// Spec: docs/gregguns-spec/05-armor/recipes.md (Gas Mask refilter); docs/techguns-spec/05-armor/recipes.md section 2
// Source: Forge 1.7.10 net.minecraft.item.crafting.IRecipe (matches, getCraftingResult, getRecipeSize, getRecipeOutput)
public final class EquipmentRefillRecipe implements IRecipe {

    private final Item mask;
    private final Item filter;

    /**
     * @param mask   the equipment item
     * @param filter the refill item
     */
    public EquipmentRefillRecipe(Item mask, Item filter) {
        this.mask = mask;
        this.filter = filter;
    }

    @Override
    public boolean matches(InventoryCrafting grid, World world) {
        int masks = 0;
        int filters = 0;
        int others = 0;
        for (int i = 0; i < grid.getSizeInventory(); i++) {
            ItemStack stack = grid.getStackInSlot(i);
            if (stack == null) {
                continue;
            }
            if (stack.getItem() == mask) {
                masks++;
            } else if (stack.getItem() == filter) {
                filters++;
            } else {
                others++;
            }
        }
        return RefilterRule.matches(masks, filters, others);
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting grid) {
        return new ItemStack(mask);
    }

    @Override
    public int getRecipeSize() {
        return 2;
    }

    @Override
    public ItemStack getRecipeOutput() {
        return new ItemStack(mask);
    }
}
