package com.gregguns.armor.slots;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import com.gregguns.GregGuns;
import com.gregguns.armor.slots.logic.BaubleSlot;
import com.gregguns.item.gun.AmmoSources;

/**
 * Finding and opening the player's Ammo Pouch. A pouch is either <em>held</em> (hotbar slot, opened by right-click) or
 * <em>worn</em> in a Baubles {@code quiver} slot. Only the worn one feeds the gun reload path
 * ({@link #SOURCE}, registered with {@link AmmoSources}), so ammo is "in the quiver" (design 0009 decision 30).
 * Server and client.
 */
// Spec: docs/gregguns-spec/05-armor/baubles-slots.md (GGS-05-03); docs/design/0009 decision 30
public final class AmmoPouch {

    /** GUI id of a pouch in the hotbar; the GUI x argument is the hotbar slot. */
    public static final int GUI_HELD = 1;
    /** GUI id of a pouch in the Baubles inventory; the GUI x argument is the Baubles inventory index. */
    public static final int GUI_WORN = 2;

    /** The hook the gun reload path consults: the worn pouch's slots, searched before the main inventory. */
    public static final AmmoSources.Provider SOURCE = AmmoPouch::collectWorn;

    private AmmoPouch() {}

    private static void collectWorn(EntityPlayer player, List<IInventory> out) {
        for (int index : BaublesCompat.indexes(player, BaubleSlot.QUIVER)) {
            PouchInventory pouch = wornAt(player, index);
            if (pouch != null) {
                out.add(pouch);
            }
        }
    }

    /**
     * @param player a player
     * @param index  a Baubles inventory index
     * @return the pouch worn there, or null when the slot is empty or holds something else
     */
    public static @Nullable PouchInventory wornAt(EntityPlayer player, int index) {
        ItemStack stack = BaublesCompat.stackAt(player, index);
        if (stack == null || !(stack.getItem() instanceof ItemAmmoPouch)) {
            return null;
        }
        return new PouchInventory(stack, () -> BaublesCompat.write(player, BaubleSlot.QUIVER, index, stack));
    }

    /**
     * @param player a player
     * @param hotbar a hotbar slot, 0 to 8
     * @return the pouch in that slot, or null
     */
    public static @Nullable PouchInventory heldAt(EntityPlayer player, int hotbar) {
        if (hotbar < 0 || hotbar > 8) {
            return null;
        }
        ItemStack stack = player.inventory.getStackInSlot(hotbar);
        if (stack == null || !(stack.getItem() instanceof ItemAmmoPouch)) {
            return null;
        }
        return new PouchInventory(stack, () -> {});
    }

    /**
     * @param player a player
     * @return the first worn pouch's Baubles index, or -1
     */
    public static int wornIndex(EntityPlayer player) {
        for (int index : BaublesCompat.indexes(player, BaubleSlot.QUIVER)) {
            if (wornAt(player, index) != null) {
                return index;
            }
        }
        return -1;
    }

    /**
     * Opens the GUI of the player's worn pouch. Server side.
     *
     * @param player a server player
     * @return true when a worn pouch was found
     */
    public static boolean openWorn(EntityPlayer player) {
        int index = wornIndex(player);
        if (index < 0) {
            return false;
        }
        player.openGui(GregGuns.instance, GUI_WORN, player.worldObj, index, 0, 0);
        return true;
    }
}
