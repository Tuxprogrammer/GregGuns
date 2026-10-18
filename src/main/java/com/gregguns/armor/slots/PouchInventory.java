package com.gregguns.armor.slots;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import org.jetbrains.annotations.Nullable;

import com.gregguns.item.ammo.ItemAmmoRound;
import com.gregguns.item.ammo.ItemMagazine;
import com.gregguns.item.ammo.ItemMagazineEmpty;
import com.gregguns.item.grenade.ItemStielgranate;

/**
 * The six slots of an Ammo Pouch, as a view over the pouch stack's NBT. Every change is written back to the stack at
 * once and reported through {@code onChange}, which for a worn pouch writes the stack into the Baubles inventory so
 * the change is saved and synced to the client. Server and client (the client reads it for the ammo HUD).
 *
 * <p>
 * Only ammunition fits: loose rounds, loaded and empty magazines, and grenades ({@link #accepts}).
 */
// Spec: docs/gregguns-spec/05-armor/baubles-slots.md (GGS-05-03: stack-size-1 pouch with a 6-slot GUI)
public final class PouchInventory implements IInventory {

    /** Slots in an Ammo Pouch. */
    public static final int SLOTS = 6;

    private static final String ITEMS = "Items";
    private static final String SLOT = "Slot";

    private final ItemStack pouch;
    private final Runnable onChange;
    private final ItemStack[] slots = new ItemStack[SLOTS];

    /**
     * @param pouch    the Ammo Pouch stack; its NBT is the storage
     * @param onChange called after every write to the stack, may do nothing
     */
    public PouchInventory(ItemStack pouch, Runnable onChange) {
        this.pouch = pouch;
        this.onChange = onChange;
        NBTTagCompound tag = pouch.getTagCompound();
        if (tag != null) {
            NBTTagList list = tag.getTagList(ITEMS, 10);
            for (int i = 0; i < list.tagCount(); i++) {
                NBTTagCompound entry = list.getCompoundTagAt(i);
                int slot = entry.getByte(SLOT) & 255;
                if (slot < slots.length) {
                    slots[slot] = ItemStack.loadItemStackFromNBT(entry);
                }
            }
        }
    }

    /**
     * @param stack any stack
     * @return whether it may go into a pouch: ammunition only
     */
    public static boolean accepts(@Nullable ItemStack stack) {
        if (stack == null) {
            return false;
        }
        Item item = stack.getItem();
        return item instanceof ItemAmmoRound || item instanceof ItemMagazine
            || item instanceof ItemMagazineEmpty
            || item instanceof ItemStielgranate;
    }

    /** @return the pouch stack this view reads and writes */
    public ItemStack pouch() {
        return pouch;
    }

    private void save() {
        NBTTagCompound tag = pouch.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            pouch.setTagCompound(tag);
        }
        NBTTagList list = new NBTTagList();
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] != null) {
                NBTTagCompound entry = new NBTTagCompound();
                slots[i].writeToNBT(entry);
                entry.setByte(SLOT, (byte) i);
                list.appendTag(entry);
            }
        }
        tag.setTag(ITEMS, list);
        onChange.run();
    }

    // --- IInventory ----------------------------------------------------------------------------------------------

    @Override
    public int getSizeInventory() {
        return slots.length;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return slots[slot];
    }

    @Override
    public ItemStack decrStackSize(int slot, int amount) {
        ItemStack stack = slots[slot];
        if (stack == null) {
            return null;
        }
        ItemStack out;
        if (stack.stackSize <= amount) {
            out = stack;
            slots[slot] = null;
        } else {
            out = stack.splitStack(amount);
            if (stack.stackSize == 0) {
                slots[slot] = null;
            }
        }
        save();
        return out;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int slot) {
        ItemStack stack = slots[slot];
        slots[slot] = null;
        return stack;
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        slots[slot] = stack;
        if (stack != null && stack.stackSize > getInventoryStackLimit()) {
            stack.stackSize = getInventoryStackLimit();
        }
        save();
    }

    @Override
    public String getInventoryName() {
        return "container.gregguns.ammo_pouch";
    }

    @Override
    public boolean hasCustomInventoryName() {
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public void markDirty() {
        save();
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        return true;
    }

    @Override
    public void openInventory() {}

    @Override
    public void closeInventory() {}

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return accepts(stack);
    }
}
