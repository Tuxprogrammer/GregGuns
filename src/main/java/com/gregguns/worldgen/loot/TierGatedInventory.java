package com.gregguns.worldgen.loot;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

/**
 * An inventory view that refuses any stack above a ceiling or {@code loot.maxTier}, judged by {@link ObtainTier}: the
 * last guard of a world loot source that writes stacks itself (furniture, a furnace's output) (D364). A refused stack
 * is simply not written. Everything else passes through.
 */
// Spec: docs/gregguns-spec/11-worldgen/structure-loot.md (Loot tier cap); D364
public final class TierGatedInventory implements IInventory {

    private final IInventory inv;
    private final int ceiling;

    /**
     * @param inv     the real inventory
     * @param ceiling the source's ceiling (voltage index)
     */
    public TierGatedInventory(IInventory inv, int ceiling) {
        this.inv = inv;
        this.ceiling = ceiling;
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        if (stack != null && !ObtainTier.allowed(stack, ceiling)) {
            return;
        }
        inv.setInventorySlotContents(slot, stack);
    }

    @Override
    public int getSizeInventory() {
        return inv.getSizeInventory();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return inv.getStackInSlot(slot);
    }

    @Override
    public ItemStack decrStackSize(int slot, int count) {
        return inv.decrStackSize(slot, count);
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int slot) {
        return inv.getStackInSlotOnClosing(slot);
    }

    @Override
    public String getInventoryName() {
        return inv.getInventoryName();
    }

    @Override
    public boolean hasCustomInventoryName() {
        return inv.hasCustomInventoryName();
    }

    @Override
    public int getInventoryStackLimit() {
        return inv.getInventoryStackLimit();
    }

    @Override
    public void markDirty() {
        inv.markDirty();
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        return inv.isUseableByPlayer(player);
    }

    @Override
    public void openInventory() {
        inv.openInventory();
    }

    @Override
    public void closeInventory() {
        inv.closeInventory();
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return inv.isItemValidForSlot(slot, stack);
    }
}
