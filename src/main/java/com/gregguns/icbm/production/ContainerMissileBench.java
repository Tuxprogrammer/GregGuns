package com.gregguns.icbm.production;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import com.gregguns.icbm.missile.MissileBodies;
import com.gregguns.icbm.missile.MissileTier;

/**
 * The container of the NASA Workbench's missile page (D8): result slot 0, then the plate slots, the four fin slots, the
 * engine slot and the guidance slot, then the player's inventory. The slots only take what their place needs
 * ({@link MissileBenchRules}); the result is the body with the guidance the computer gives. Taking the result uses one
 * item from every slot. Closing the page returns the rest to the player. Both sides.
 */
// Spec: docs/gregguns-icbm-spec/03-missiles/build-and-recipes.md (NASA Workbench page)
public class ContainerMissileBench extends Container {

    /** Slot index of the result. */
    public static final int RESULT = 0;

    private final MissileBuildSpec spec;
    private final IInventory matrix;
    private final IInventory craftResult = new InventoryBasic("result", false, 1);
    private final int plateFirst;
    private final int finFirst;
    private final int engineSlot;
    private final int guidanceSlot;
    private final int playerFirst;
    private final EntityPlayer player;

    /**
     * @param inventory the player's inventory
     * @param spec      which body the page makes
     */
    public ContainerMissileBench(InventoryPlayer inventory, MissileBuildSpec spec) {
        this.spec = spec;
        this.player = inventory.player;
        int plates = MissileBenchRules.plateSlots(spec);
        this.matrix = new InventoryBasic("matrix", false, plates + MissileBenchRules.FIN_SLOTS + 2) {

            @Override
            public void markDirty() {
                super.markDirty();
                onCraftMatrixChanged(this);
            }
        };
        addSlotToContainer(new ResultSlot(craftResult, 0, 142, 96));
        plateFirst = inventorySlots.size();
        for (int i = 0; i < plates; i++) {
            addSlotToContainer(
                new KeySlot(matrix, i, 30 + (i % 3) * 18, 20 + (i / 3) * 18, MissileBenchRules.keyOf(spec.plates())));
        }
        finFirst = inventorySlots.size();
        for (int i = 0; i < MissileBenchRules.FIN_SLOTS; i++) {
            addSlotToContainer(new KeySlot(matrix, plates + i, 30 + i * 18, 62, MissileBenchRules.keyOf(spec.fins())));
        }
        engineSlot = inventorySlots.size();
        addSlotToContainer(
            new KeySlot(
                matrix,
                plates + MissileBenchRules.FIN_SLOTS,
                100,
                20,
                spec.engine()
                    .registryName()));
        guidanceSlot = inventorySlots.size();
        addSlotToContainer(new KeySlot(matrix, plates + MissileBenchRules.FIN_SLOTS + 1, 100, 44, null));
        playerFirst = inventorySlots.size();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlotToContainer(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 118 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlotToContainer(new Slot(inventory, col, 8 + col * 18, 176));
        }
        onCraftMatrixChanged(matrix);
    }

    /** @return the body this page makes */
    public MissileBuildSpec spec() {
        return spec;
    }

    /** @return the number of slots before the player's inventory, including the result */
    public int machineSlots() {
        return playerFirst;
    }

    /** @return the guidance steps the current slots give, or -1 when they make no body */
    public int currentGuidance() {
        List<String> plates = new ArrayList<>();
        for (int i = 0; i < MissileBenchRules.plateSlots(spec); i++) {
            plates.add(MissileBenchStacks.keyOf(matrix.getStackInSlot(i)));
        }
        int p = MissileBenchRules.plateSlots(spec);
        List<String> fins = new ArrayList<>();
        for (int i = 0; i < MissileBenchRules.FIN_SLOTS; i++) {
            fins.add(MissileBenchStacks.keyOf(matrix.getStackInSlot(p + i)));
        }
        String engine = MissileBenchStacks.keyOf(matrix.getStackInSlot(p + MissileBenchRules.FIN_SLOTS));
        String guidance = MissileBenchStacks.keyOf(matrix.getStackInSlot(p + MissileBenchRules.FIN_SLOTS + 1));
        return MissileBenchRules.match(spec, plates, fins, engine, guidance);
    }

    @Override
    public void onCraftMatrixChanged(IInventory changed) {
        int g = currentGuidance();
        MissileTier tier = MissileTier.byNumber(spec.number());
        ItemStack out = null;
        if (g >= 0 && tier != null && MissileBodies.item(tier) != null) {
            out = MissileBodies.body(tier, g);
        }
        craftResult.setInventorySlotContents(0, out);
    }

    @Override
    public boolean canInteractWith(EntityPlayer p) {
        return true;
    }

    @Override
    public void onContainerClosed(EntityPlayer p) {
        super.onContainerClosed(p);
        if (!p.worldObj.isRemote) {
            for (int i = 0; i < matrix.getSizeInventory(); i++) {
                ItemStack s = matrix.getStackInSlotOnClosing(i);
                if (s != null) {
                    p.dropPlayerItemWithRandomChoice(s, false);
                }
            }
        }
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer p, int index) {
        Slot slot = (Slot) inventorySlots.get(index);
        if (slot == null || !slot.getHasStack()) {
            return null;
        }
        ItemStack stack = slot.getStack();
        ItemStack copy = stack.copy();
        if (index == RESULT) {
            if (!mergeItemStack(stack, playerFirst, playerFirst + 36, true)) {
                return null;
            }
            slot.onSlotChange(stack, copy);
        } else if (index < playerFirst) {
            if (!mergeItemStack(stack, playerFirst, playerFirst + 36, false)) {
                return null;
            }
        } else {
            // From the player's inventory into the first slot that accepts it.
            boolean moved = false;
            for (int i = 1; i < playerFirst && !moved; i++) {
                Slot target = (Slot) inventorySlots.get(i);
                if (!target.getHasStack() && target.isItemValid(stack)) {
                    ItemStack one = stack.copy();
                    one.stackSize = 1;
                    target.putStack(one);
                    stack.stackSize--;
                    moved = true;
                }
            }
            if (!moved) {
                return null;
            }
        }
        if (stack.stackSize == 0) {
            slot.putStack(null);
        } else {
            slot.onSlotChanged();
        }
        if (stack.stackSize == copy.stackSize) {
            return null;
        }
        slot.onPickupFromSlot(p, stack);
        return copy;
    }

    /** One slot of the matrix: takes one item, and only the key its place needs; a null key is the guidance slot. */
    private final class KeySlot extends Slot {

        private final String key;

        KeySlot(IInventory inv, int index, int x, int y, String key) {
            super(inv, index, x, y);
            this.key = key;
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            String k = MissileBenchStacks.keyOf(stack);
            if (k == null) {
                return false;
            }
            return key == null ? MissileBenchRules.accepts(spec, k) && k.startsWith("guidance_computer")
                : k.equals(key);
        }

        @Override
        public int getSlotStackLimit() {
            return 1;
        }
    }

    /** The result slot: nothing goes in, and taking the body uses one item from every matrix slot. */
    private final class ResultSlot extends Slot {

        ResultSlot(IInventory inv, int index, int x, int y) {
            super(inv, index, x, y);
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            return false;
        }

        @Override
        public void onPickupFromSlot(EntityPlayer p, ItemStack stack) {
            for (int i = 0; i < matrix.getSizeInventory(); i++) {
                ItemStack s = matrix.getStackInSlot(i);
                if (s != null) {
                    matrix.decrStackSize(i, 1);
                }
            }
            onCraftMatrixChanged(matrix);
        }
    }
}
