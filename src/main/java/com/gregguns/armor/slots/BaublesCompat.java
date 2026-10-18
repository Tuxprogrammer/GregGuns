package com.gregguns.armor.slots;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import com.gregguns.GregGuns;
import com.gregguns.armor.slots.logic.BaubleSlot;

import baubles.api.BaublesApi;
import baubles.api.expanded.BaubleExpandedSlots;
import baubles.api.expanded.BaubleItemHelper;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The one place that reads Baubles Expanded. Baubles Expanded is a soft dependency (GGS-05-07): every public method
 * checks {@link Loader#isModLoaded} first and answers "nothing worn" when it is absent, and the Baubles types are only
 * touched inside {@link Impl}, which the JVM loads on first use, so a server without the mod never links them.
 *
 * <p>
 * Equipment is always found through the typed slots (the pack's slot list decides which indexes are {@code head},
 * {@code wings} and {@code quiver}), never by a fixed index. Server and client.
 */
// Spec: docs/gregguns-spec/05-armor/baubles-slots.md (Integration points; GGS-05-07)
// Source: Baubles Expanded 2.2.25-GTNH, BaubleExpandedSlots#getIndexesOfAssignedSlotsOfType
// https://github.com/GTNewHorizons/Baubles-Expanded/blob/2.2.25-GTNH/src/main/java/baubles/api/expanded/BaubleExpandedSlots.java#L184
public final class BaublesCompat {

    /** The Baubles Expanded mod id. */
    public static final String MODID = "Baubles|Expanded";

    // Set on first use; a cache of "is the mod there", not gameplay state.
    private static Boolean loaded;
    private static boolean warned;

    private BaublesCompat() {}

    /** @return whether Baubles Expanded is loaded */
    public static boolean isLoaded() {
        if (loaded == null) {
            loaded = Loader.isModLoaded(MODID);
        }
        return loaded;
    }

    /** Logs the single "bauble items can only be held" warning when the mod is absent (GGS-05-07). */
    public static void warnIfAbsent() {
        if (!isLoaded() && !warned) {
            warned = true;
            GregGuns.LOG.warn(
                "Baubles Expanded is not loaded: the GregGuns goggles, back devices and ammo pouch can be held but not worn.");
        }
    }

    /**
     * @param player a player
     * @param slot   a slot type
     * @return the stacks currently in the player's slots of that type; empty when none, or the mod is absent
     */
    public static List<ItemStack> worn(EntityPlayer player, BaubleSlot slot) {
        if (!isLoaded()) {
            return Collections.emptyList();
        }
        return Impl.worn(player, slot);
    }

    /**
     * @param player a player
     * @param slot   a slot type
     * @return the first stack in a slot of that type, or null
     */
    public static ItemStack first(EntityPlayer player, BaubleSlot slot) {
        List<ItemStack> all = worn(player, slot);
        return all.isEmpty() ? null : all.get(0);
    }

    /**
     * Puts a stack into the first empty slot of a type.
     *
     * @param player a player
     * @param slot   a slot type
     * @param stack  the stack
     * @return true when it was placed; false when the mod is absent, the type has no slot or all are full
     */
    public static boolean equip(EntityPlayer player, BaubleSlot slot, ItemStack stack) {
        return isLoaded() && Impl.equip(player, slot, stack);
    }

    /**
     * Empties every slot of a type.
     *
     * @param player a player
     * @param slot   a slot type
     */
    public static void clear(EntityPlayer player, BaubleSlot slot) {
        if (isLoaded()) {
            Impl.clear(player, slot);
        }
    }

    /**
     * @param player a player
     * @param slot   a slot type
     * @return the inventory slot indexes the pack assigned to the type; empty when none or the mod is absent
     */
    public static int[] indexes(EntityPlayer player, BaubleSlot slot) {
        return isLoaded() ? Impl.indexes(slot) : new int[0];
    }

    /**
     * @param player a player
     * @param index  an inventory index from {@link #indexes}
     * @return the stack in that Baubles inventory index, or null
     */
    public static ItemStack stackAt(EntityPlayer player, int index) {
        return isLoaded() ? Impl.stackAt(player, index) : null;
    }

    /**
     * Writes a stack back into the Baubles inventory so the change is saved and synced to the client.
     *
     * @param player a player
     * @param slot   a slot type
     * @param index  an inventory index from {@link #indexes}
     * @param stack  the stack now in that index
     */
    public static void write(EntityPlayer player, BaubleSlot slot, int index, ItemStack stack) {
        if (isLoaded()) {
            Impl.write(player, index, stack);
        }
    }

    /**
     * Equips the held bauble into the first matching free slot, as Baubles Expanded's own items do. Does nothing when
     * the mod is absent (the item can only be held, GGS-05-07).
     *
     * @param stack  the held stack
     * @param world  the world
     * @param player the player
     * @return the stack to leave in the hand
     */
    public static ItemStack rightClickEquip(ItemStack stack, World world, EntityPlayer player) {
        return isLoaded() ? Impl.rightClick(stack, world, player) : stack;
    }

    /**
     * Adds the shift-for-slots tooltip lines. Client only.
     *
     * @param tooltip the tooltip list
     * @param slot    the slot type the item fits
     */
    @SideOnly(Side.CLIENT)
    public static void addSlotInformation(List<String> tooltip, BaubleSlot slot) {
        if (isLoaded()) {
            Impl.addSlotInformation(tooltip, slot);
        }
    }

    /** The code that links against Baubles. Never referenced unless the mod is loaded. */
    private static final class Impl {

        static int[] indexes(BaubleSlot slot) {
            int[] idx = BaubleExpandedSlots.getIndexesOfAssignedSlotsOfType(slot.typeName());
            return idx == null ? new int[0] : idx;
        }

        static List<ItemStack> worn(EntityPlayer player, BaubleSlot slot) {
            IInventory baubles = BaublesApi.getBaubles(player);
            if (baubles == null) {
                return Collections.emptyList();
            }
            List<ItemStack> out = new ArrayList<>(1);
            for (int i : indexes(slot)) {
                if (i >= 0 && i < baubles.getSizeInventory()) {
                    ItemStack stack = baubles.getStackInSlot(i);
                    if (stack != null) {
                        out.add(stack);
                    }
                }
            }
            return out;
        }

        static boolean equip(EntityPlayer player, BaubleSlot slot, ItemStack stack) {
            IInventory baubles = BaublesApi.getBaubles(player);
            if (baubles == null) {
                return false;
            }
            for (int i : indexes(slot)) {
                if (i >= 0 && i < baubles.getSizeInventory() && baubles.getStackInSlot(i) == null) {
                    baubles.setInventorySlotContents(i, stack);
                    return true;
                }
            }
            return false;
        }

        static void clear(EntityPlayer player, BaubleSlot slot) {
            IInventory baubles = BaublesApi.getBaubles(player);
            if (baubles == null) {
                return;
            }
            for (int i : indexes(slot)) {
                if (i >= 0 && i < baubles.getSizeInventory()) {
                    baubles.setInventorySlotContents(i, null);
                }
            }
        }

        static ItemStack stackAt(EntityPlayer player, int index) {
            IInventory baubles = BaublesApi.getBaubles(player);
            return baubles != null && index >= 0 && index < baubles.getSizeInventory() ? baubles.getStackInSlot(index)
                : null;
        }

        static ItemStack rightClick(ItemStack stack, World world, EntityPlayer player) {
            return BaubleItemHelper.onBaubleRightClick(stack, world, player);
        }

        @SideOnly(Side.CLIENT)
        static void addSlotInformation(List<String> tooltip, BaubleSlot slot) {
            BaubleItemHelper.addSlotInformation(tooltip, slot.types());
        }

        static void write(EntityPlayer player, int index, ItemStack stack) {
            IInventory baubles = BaublesApi.getBaubles(player);
            if (baubles != null && index >= 0 && index < baubles.getSizeInventory()) {
                baubles.setInventorySlotContents(index, stack);
            }
        }
    }
}
