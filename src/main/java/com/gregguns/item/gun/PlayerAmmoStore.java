package com.gregguns.item.gun;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.gregguns.core.registry.Naming;

import cpw.mods.fml.common.registry.GameRegistry;

/**
 * An {@link AmmoStore} over a player's ammo: the extra inventories registered with {@link AmmoSources} (the Ammo Pouch)
 * first, then the main inventory. Works on either side (the client uses it read-only for the HUD count); only the
 * server adds, removes and drops. Items are added to the main inventory only.
 */
// Spec: gun-lifecycle.md section 7 (pouch first, then main inventory); baubles-slots.md GGS-05-03.
public final class PlayerAmmoStore implements AmmoStore {

    private final EntityPlayer player;

    /** @param player the shooter */
    public PlayerAmmoStore(EntityPlayer player) {
        this.player = player;
    }

    private static Item item(String itemId) {
        return GameRegistry.findItem(Naming.NAMESPACE, itemId);
    }

    @Override
    public int count(String itemId) {
        Item item = item(itemId);
        if (item == null) {
            return 0;
        }
        int total = 0;
        for (IInventory extra : AmmoSources.extra(player)) {
            for (int i = 0; i < extra.getSizeInventory(); i++) {
                ItemStack stack = extra.getStackInSlot(i);
                if (stack != null && stack.getItem() == item) {
                    total += stack.stackSize;
                }
            }
        }
        for (ItemStack stack : player.inventory.mainInventory) {
            if (stack != null && stack.getItem() == item) {
                total += stack.stackSize;
            }
        }
        return total;
    }

    @Override
    public int remove(String itemId, int max) {
        Item item = item(itemId);
        if (item == null) {
            return 0;
        }
        int removed = 0;
        for (IInventory extra : AmmoSources.extra(player)) {
            removed += removeFrom(extra, item, max - removed);
        }
        ItemStack[] slots = player.inventory.mainInventory;
        for (int i = 0; i < slots.length && removed < max; i++) {
            ItemStack stack = slots[i];
            if (stack == null || stack.getItem() != item) {
                continue;
            }
            int take = Math.min(stack.stackSize, max - removed);
            stack.stackSize -= take;
            removed += take;
            if (stack.stackSize <= 0) {
                slots[i] = null;
            }
        }
        return removed;
    }

    private static int removeFrom(IInventory inventory, Item item, int max) {
        int removed = 0;
        for (int i = 0; i < inventory.getSizeInventory() && removed < max; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack == null || stack.getItem() != item) {
                continue;
            }
            int take = Math.min(stack.stackSize, max - removed);
            stack.stackSize -= take;
            removed += take;
            inventory.setInventorySlotContents(i, stack.stackSize <= 0 ? null : stack);
        }
        if (removed > 0) {
            inventory.markDirty();
        }
        return removed;
    }

    @Override
    public int add(String itemId, int amount) {
        Item item = item(itemId);
        if (item == null) {
            return amount;
        }
        int left = amount;
        while (left > 0) {
            ItemStack stack = new ItemStack(item, Math.min(left, item.getItemStackLimit()));
            int chunk = stack.stackSize;
            player.inventory.addItemStackToInventory(stack);
            left -= chunk - stack.stackSize;
            if (stack.stackSize > 0) {
                break; // the inventory is full
            }
        }
        return left;
    }

    @Override
    public void drop(String itemId, int amount) {
        Item item = item(itemId);
        if (item == null) {
            return;
        }
        int left = amount;
        while (left > 0) {
            int chunk = Math.min(left, item.getItemStackLimit());
            player.dropPlayerItemWithRandomChoice(new ItemStack(item, chunk), false);
            left -= chunk;
        }
    }
}
