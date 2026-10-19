package com.gregguns.gun.server;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

/**
 * Puts a stack straight into a player's inventory on the server and syncs it to the client. Used by the in-game
 * {@code mp.give} scenario instead of the vanilla {@code /give}: in 1.7.10 that command only drops an item entity at
 * the player, and the later collision pickup races with the arena item cleanup and with server lag. Server side.
 */
public final class InventoryGrant {

    private InventoryGrant() {}

    /**
     * Inserts a copy of {@code stack} with {@code addItemStackToInventory}; if that leaves anything over, falls back
     * to the selected hotbar slot, then to any empty main slot. Then syncs the container to the client (skipped when
     * the player has no network handler, as with a fake player) and verifies the stack is carried.
     *
     * @param player the receiving player
     * @param stack  the stack to grant; not modified
     * @return null on success, otherwise a message saying why the stack is not in the inventory
     */
    public static String grant(EntityPlayerMP player, ItemStack stack) {
        ItemStack copy = stack.copy();
        if (!player.inventory.addItemStackToInventory(copy) || copy.stackSize > 0) {
            ItemStack rest = copy.stackSize > 0 ? copy : stack.copy();
            if (!placeInSlot(player, rest)) {
                return "inventory full, could not place " + stack.getDisplayName();
            }
        }
        if (player.playerNetServerHandler != null) {
            player.inventoryContainer.detectAndSendChanges();
        }
        if (!carries(player, stack)) {
            return stack.getDisplayName() + " is not in the inventory after insertion";
        }
        return null;
    }

    private static boolean placeInSlot(EntityPlayerMP player, ItemStack stack) {
        ItemStack[] main = player.inventory.mainInventory;
        int selected = player.inventory.currentItem;
        if (selected >= 0 && selected < main.length && main[selected] == null) {
            main[selected] = stack;
            return true;
        }
        for (int i = 0; i < main.length; i++) {
            if (main[i] == null) {
                main[i] = stack;
                return true;
            }
        }
        return false;
    }

    /**
     * @param player the player to inspect
     * @param stack  the wanted item and damage value
     * @return true if the main inventory holds at least one item of the same kind and damage value
     */
    public static boolean carries(EntityPlayerMP player, ItemStack stack) {
        for (ItemStack held : player.inventory.mainInventory) {
            if (held != null && held.getItem() == stack.getItem() && held.getItemDamage() == stack.getItemDamage()) {
                return true;
            }
        }
        return false;
    }
}
