package com.gregguns.armor.slots;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/**
 * The server-side container of the Ammo Pouch GUI: the six pouch slots (ammunition only) above the player's inventory.
 * The pouch itself cannot be moved while it is open, and the container closes if the pouch is no longer where it was
 * opened from. Server and client.
 */
// Spec: docs/gregguns-spec/05-armor/baubles-slots.md (GGS-05-03: 6-slot GUI)
public final class ContainerAmmoPouch extends Container {

    private final PouchInventory pouch;
    private final int guiId;
    private final int index;

    /**
     * @param player the viewer
     * @param pouch  the pouch being edited
     * @param guiId  {@link AmmoPouch#GUI_HELD} or {@link AmmoPouch#GUI_WORN}
     * @param index  the hotbar slot or Baubles index the pouch was opened from
     */
    public ContainerAmmoPouch(EntityPlayer player, PouchInventory pouch, int guiId, int index) {
        this.pouch = pouch;
        this.guiId = guiId;
        this.index = index;
        for (int i = 0; i < PouchInventory.SLOTS; i++) {
            addSlotToContainer(new Slot(pouch, i, 8 + i * 18, 18) {

                @Override
                public boolean isItemValid(ItemStack stack) {
                    return PouchInventory.accepts(stack);
                }
            });
        }
        InventoryPlayer inv = player.inventory;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlotToContainer(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 50 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlotToContainer(new Slot(inv, col, 8 + col * 18, 108));
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        PouchInventory now = guiId == AmmoPouch.GUI_HELD ? AmmoPouch.heldAt(player, index)
            : AmmoPouch.wornAt(player, index);
        return now != null && now.pouch() == pouch.pouch();
    }

    @Override
    public ItemStack slotClick(int slotId, int button, int mode, EntityPlayer player) {
        // The pouch must stay where it is while its GUI is open.
        if (slotId >= PouchInventory.SLOTS && guiId == AmmoPouch.GUI_HELD) {
            Slot slot = (Slot) inventorySlots.get(slotId);
            if (slot.getStack() == pouch.pouch()) {
                return null;
            }
            if (mode == 2 && button >= 0 && button < 9 && player.inventory.getStackInSlot(button) == pouch.pouch()) {
                return null;
            }
        }
        return super.slotClick(slotId, button, mode, player);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int slotIndex) {
        ItemStack copy = null;
        Slot slot = (Slot) inventorySlots.get(slotIndex);
        if (slot != null && slot.getHasStack()) {
            ItemStack stack = slot.getStack();
            if (stack == pouch.pouch()) {
                return null;
            }
            copy = stack.copy();
            if (slotIndex < PouchInventory.SLOTS) {
                if (!mergeItemStack(stack, PouchInventory.SLOTS, inventorySlots.size(), true)) {
                    return null;
                }
            } else if (!PouchInventory.accepts(stack) || !mergeItemStack(stack, 0, PouchInventory.SLOTS, false)) {
                return null;
            }
            if (stack.stackSize == 0) {
                slot.putStack(null);
            } else {
                slot.onSlotChanged();
            }
        }
        return copy;
    }
}
