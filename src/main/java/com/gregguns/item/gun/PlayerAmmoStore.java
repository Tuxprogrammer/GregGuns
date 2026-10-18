package com.gregguns.item.gun;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.gregguns.core.registry.Naming;

import cpw.mods.fml.common.registry.GameRegistry;

/**
 * An {@link AmmoStore} over a player's main inventory. Ammo-pouch slots join in with the armor milestone. Works on
 * either side (the client uses it read-only for the HUD count); only the server adds, removes and drops.
 */
// Spec: gun-lifecycle.md section 7 (pouch first, then main inventory; pouch slots do not exist yet).
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
