package com.gregguns.utility.storage;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;

import com.gregguns.worldgen.loot.LazyChests;
import com.gregguns.worldgen.loot.LootPending;

/**
 * The tile entity of the Reinforced Chest and the Weapon Chest: 36 slots (3 rows of 12), open to every side. The
 * Weapon Chest refuses everything {@link WeaponChestFilter} refuses, for hoppers and players alike. The viewer count is
 * kept by the chest itself, so a lid stays open while anyone looks (fixes MUTL-34). Both sides; the lid fields are
 * client animation.
 */
// Spec: docs/techguns-spec/09-machines-utility/misc-tile-entities.md section 1; fixes MUTL-34
// Deviation: single chests only (no double chests, no auto-food or ammo slots); M6 plan decision 13.
public class TileGregChest extends TileEntity implements IInventory {

    /** Slots: 3 rows of 12. */
    public static final int SLOTS = 36;
    /** Slots per GUI row. */
    public static final int COLUMNS = 12;
    private static final int RECOUNT_TICKS = 200;
    private static final String NAME_REINFORCED = "container.gregguns.reinforced_chest";
    private static final String NAME_WEAPON = "container.gregguns.weapon_chest";

    private final ItemStack[] items = new ItemStack[SLOTS];
    private String customName;
    /** Unfilled loot order from a structure, or null once filled (or never set). GGS-11-20. */
    private LootPending pending;
    private int viewers;
    /** Lid angle in 0..1, client only. */
    public float lidAngle;
    /** Last tick's lid angle, client only. */
    public float prevLidAngle;

    private boolean weapon() {
        return worldObj != null && getBlockType() instanceof BlockGregChest
            && ((BlockGregChest) getBlockType()).isWeaponChest();
    }

    /**
     * Marks the chest unfilled: its contents are drawn from the order on first access. Server side.
     *
     * @param order the loot order, or null to clear it
     */
    public void setPending(LootPending order) {
        pending = order;
        markDirty();
    }

    /** @return the unfilled loot order, or null when the chest is filled */
    public LootPending pending() {
        return pending;
    }

    /**
     * Fills the chest from its loot order, exactly once. Called by every inventory accessor, on open and on break, so
     * a hopper or GT extraction sees the same items a player would. The order is cleared before the draw because the
     * draw writes through the accessors. A client copy never fills (it has no order anyway). Contents depend only on
     * the order's seed, never on who asked first (WGEN-25).
     */
    public void ensureFilled() {
        LootPending order = pending;
        if (order == null || worldObj != null && worldObj.isRemote) {
            return;
        }
        pending = null;
        LazyChests.fillEagerly(this, order);
        markDirty();
    }

    /** @return the number of players looking in the chest */
    public int viewers() {
        return viewers;
    }

    /**
     * Puts a stack in the first slots that take it, like a hopper would. Server side.
     *
     * @param stack the stack to insert; reduced by what was taken
     * @return true when at least one item went in
     */
    public boolean insert(ItemStack stack) {
        ensureFilled();
        if (stack == null || !isItemValidForSlot(0, stack)) {
            return false;
        }
        int before = stack.stackSize;
        for (int i = 0; i < SLOTS && stack.stackSize > 0; i++) {
            ItemStack in = items[i];
            if (in != null && in.isItemEqual(stack) && ItemStack.areItemStackTagsEqual(in, stack)) {
                int room = Math.min(in.getMaxStackSize(), getInventoryStackLimit()) - in.stackSize;
                int moved = Math.min(room, stack.stackSize);
                if (moved > 0) {
                    in.stackSize += moved;
                    stack.stackSize -= moved;
                }
            }
        }
        for (int i = 0; i < SLOTS && stack.stackSize > 0; i++) {
            if (items[i] == null) {
                int moved = Math.min(stack.getMaxStackSize(), stack.stackSize);
                items[i] = stack.splitStack(moved);
            }
        }
        markDirty();
        return stack.stackSize < before;
    }

    @Override
    public int getSizeInventory() {
        return SLOTS;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        ensureFilled();
        return items[slot];
    }

    @Override
    public ItemStack decrStackSize(int slot, int amount) {
        ensureFilled();
        ItemStack in = items[slot];
        if (in == null) {
            return null;
        }
        ItemStack taken;
        if (in.stackSize <= amount) {
            taken = in;
            items[slot] = null;
        } else {
            taken = in.splitStack(amount);
            if (in.stackSize == 0) {
                items[slot] = null;
            }
        }
        markDirty();
        return taken;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int slot) {
        ensureFilled();
        ItemStack in = items[slot];
        items[slot] = null;
        return in;
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        ensureFilled();
        items[slot] = stack;
        if (stack != null && stack.stackSize > getInventoryStackLimit()) {
            stack.stackSize = getInventoryStackLimit();
        }
        markDirty();
    }

    @Override
    public String getInventoryName() {
        return hasCustomInventoryName() ? customName : weapon() ? NAME_WEAPON : NAME_REINFORCED;
    }

    @Override
    public boolean hasCustomInventoryName() {
        return customName != null && !customName.isEmpty();
    }

    /** @param name the anvil name carried by the item, or null */
    public void setCustomName(String name) {
        customName = name;
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        return worldObj != null && worldObj.getTileEntity(xCoord, yCoord, zCoord) == this
            && player.getDistanceSq(xCoord + 0.5, yCoord + 0.5, zCoord + 0.5) <= 64.0;
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (!weapon()) {
            return true;
        }
        Item item = stack.getItem();
        String name = item == null ? null : Item.itemRegistry.getNameForObject(item);
        return WeaponChestFilter.accepts(name, item instanceof ItemBlock);
    }

    @Override
    public void openInventory() {
        ensureFilled();
        if (worldObj != null && !worldObj.isRemote) {
            setViewers(viewers + 1);
        }
    }

    @Override
    public void closeInventory() {
        if (worldObj != null && !worldObj.isRemote) {
            setViewers(Math.max(0, viewers - 1));
        }
    }

    private void setViewers(int count) {
        if (count == viewers) {
            return;
        }
        viewers = count;
        worldObj.addBlockEvent(xCoord, yCoord, zCoord, getBlockType(), 1, viewers);
        worldObj.notifyBlocksOfNeighborChange(xCoord, yCoord, zCoord, getBlockType());
    }

    @Override
    public boolean receiveClientEvent(int id, int type) {
        if (id == 1) {
            viewers = type;
            return true;
        }
        return super.receiveClientEvent(id, type);
    }

    @Override
    public void updateEntity() {
        if (worldObj == null) {
            return;
        }
        if (!worldObj.isRemote) {
            if ((worldObj.getTotalWorldTime() + xCoord + yCoord + zCoord) % RECOUNT_TICKS == 0) {
                // Our own recount: players whose open container is this chest's container (MUTL-34).
                int counted = 0;
                for (Object o : worldObj.playerEntities) {
                    EntityPlayer p = (EntityPlayer) o;
                    if (p.openContainer instanceof ContainerGregChest
                        && ((ContainerGregChest) p.openContainer).chest() == this) {
                        counted++;
                    }
                }
                setViewers(counted);
            }
            return;
        }
        prevLidAngle = lidAngle;
        if (viewers > 0 && lidAngle == 0.0F) {
            worldObj.playSound(xCoord + 0.5, yCoord + 0.5, zCoord + 0.5, "random.chestopen", 0.5F, 0.9F, false);
        }
        if (viewers == 0 && lidAngle > 0.0F || viewers > 0 && lidAngle < 1.0F) {
            float was = lidAngle;
            lidAngle = viewers > 0 ? Math.min(1.0F, lidAngle + 0.1F) : Math.max(0.0F, lidAngle - 0.1F);
            if (lidAngle < 0.5F && was >= 0.5F) {
                worldObj.playSound(xCoord + 0.5, yCoord + 0.5, zCoord + 0.5, "random.chestclosed", 0.5F, 0.9F, false);
            }
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        NBTTagList list = new NBTTagList();
        for (int i = 0; i < SLOTS; i++) {
            if (items[i] != null) {
                NBTTagCompound entry = new NBTTagCompound();
                entry.setByte("Slot", (byte) i);
                items[i].writeToNBT(entry);
                list.appendTag(entry);
            }
        }
        tag.setTag("Items", list);
        if (pending != null) {
            pending.writeTo(tag);
        }
        if (hasCustomInventoryName()) {
            tag.setString("CustomName", customName);
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        java.util.Arrays.fill(items, null);
        NBTTagList list = tag.getTagList("Items", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound entry = list.getCompoundTagAt(i);
            int slot = entry.getByte("Slot") & 255;
            if (slot < SLOTS) {
                items[slot] = ItemStack.loadItemStackFromNBT(entry);
            }
        }
        customName = tag.hasKey("CustomName") ? tag.getString("CustomName") : null;
        pending = LootPending.readFrom(tag);
    }

    /** @return the items, for the block to spill; slots may be null */
    ItemStack[] rawItems() {
        ensureFilled();
        return items;
    }
}
