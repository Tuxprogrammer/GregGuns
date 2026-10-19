package com.gregguns.utility.storage;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/**
 * The container of a chest GUI: the 36 chest slots in 3 rows of 12, then the player's inventory. Shift-click moves a
 * chest slot into the player's inventory and anything else into the chest, with no special slots in the way (fixes
 * MUTL-36); a Weapon Chest still refuses what its filter refuses. Both sides.
 */
// Spec: docs/techguns-spec/09-machines-utility/misc-tile-entities.md section 1 (Container); fixes MUTL-36
public final class ContainerGregChest extends Container {

    /** GUI width in pixels: 12 slots plus the margins. */
    public static final int WIDTH = 8 + TileGregChest.COLUMNS * 18 + 8;
    /** GUI height in pixels for 3 chest rows. */
    public static final int HEIGHT = 168;

    private final TileGregChest chest;

    /**
     * @param player the viewer
     * @param chest  the chest
     */
    public ContainerGregChest(EntityPlayer player, TileGregChest chest) {
        this.chest = chest;
        chest.openInventory();
        for (int i = 0; i < TileGregChest.SLOTS; i++) {
            addSlotToContainer(
                new Slot(chest, i, 8 + 18 * (i % TileGregChest.COLUMNS), 18 + 18 * (i / TileGregChest.COLUMNS)) {

                    @Override
                    public boolean isItemValid(ItemStack stack) {
                        return chest.isItemValidForSlot(getSlotIndex(), stack);
                    }
                });
        }
        InventoryPlayer inv = player.inventory;
        int left = 8 + (TileGregChest.COLUMNS - 9) * 9;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlotToContainer(new Slot(inv, col + row * 9 + 9, left + col * 18, 85 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlotToContainer(new Slot(inv, col, left + col * 18, 143));
        }
    }

    /** @return the chest this container shows */
    public TileGregChest chest() {
        return chest;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return chest.isUseableByPlayer(player);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        Slot slot = (Slot) inventorySlots.get(index);
        if (slot == null || !slot.getHasStack()) {
            return null;
        }
        ItemStack stack = slot.getStack();
        ItemStack copy = stack.copy();
        if (index < TileGregChest.SLOTS) {
            if (!mergeItemStack(stack, TileGregChest.SLOTS, inventorySlots.size(), true)) {
                return null;
            }
        } else if (!chest.isItemValidForSlot(0, stack) || !mergeItemStack(stack, 0, TileGregChest.SLOTS, false)) {
            return null;
        }
        if (stack.stackSize == 0) {
            slot.putStack(null);
        } else {
            slot.onSlotChanged();
        }
        return copy;
    }

    @Override
    public void onContainerClosed(EntityPlayer player) {
        super.onContainerClosed(player);
        chest.closeInventory();
    }
}
