package com.gregguns.worldgen.loot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.junit.jupiter.api.Test;

/** A chest fill never overwrites a stack, so a gun's ammo cannot be lost to a later pick (GGS-11-26). */
class NoOverwriteTest {

    @Test
    void aStackAimedAtAnOccupiedSlotGoesToTheNextEmptyOne() {
        InventoryBasic chest = new InventoryBasic("t", false, 3);
        LootCategories.NoOverwrite view = new LootCategories.NoOverwrite(chest);
        Item item = new Item();
        ItemStack a = new ItemStack(item, 1, 0);
        ItemStack b = new ItemStack(item, 1, 1);
        ItemStack c = new ItemStack(item, 1, 2);
        ItemStack d = new ItemStack(item, 1, 3);
        view.setInventorySlotContents(2, a);
        view.setInventorySlotContents(2, b);
        view.setInventorySlotContents(2, c);
        assertSame(a, chest.getStackInSlot(2));
        assertSame(b, chest.getStackInSlot(0), "wraps around to slot 0");
        assertSame(c, chest.getStackInSlot(1));
        // A full chest drops the extra stack and keeps what it holds.
        view.setInventorySlotContents(0, d);
        assertSame(b, chest.getStackInSlot(0));
        assertSame(c, chest.getStackInSlot(1));
        assertSame(a, chest.getStackInSlot(2));
        assertEquals(3, view.getSizeInventory());
    }

    @Test
    void anEmptyChestTakesTheStackWhereItWasAimed() {
        InventoryBasic chest = new InventoryBasic("t", false, 5);
        LootCategories.NoOverwrite view = new LootCategories.NoOverwrite(chest);
        ItemStack a = new ItemStack(new Item(), 1, 0);
        view.setInventorySlotContents(3, a);
        assertSame(a, chest.getStackInSlot(3));
        assertNull(chest.getStackInSlot(0));
    }
}
