package com.gregguns.armor.slots;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.gregguns.GregGuns;
import com.gregguns.armor.slots.logic.BaubleSlot;
import com.gregguns.armor.slots.logic.SlotPayload;

/**
 * Describes what a player has in a Baubles slot as a {@link SlotPayload} string. The two-client slot scenarios run it
 * on the server for the truth and on each client for what that client received, then compare the strings. It reads only
 * what must survive sync and saving: the item, the stack size, the goggles' on/off switch and the pouch's inner slots
 * (stack NBT). It leaves out the electric charge on purpose, because the server drains it every tick without
 * re-sending the stack. Server and client, dev scenarios only.
 */
// Spec: docs/gregguns-spec/05-armor/baubles-slots.md (GGS-05-03, GGS-05-04)
public final class SlotSyncProbe {

    private SlotSyncProbe() {}

    /**
     * @param player a player (the local one or another player on a client; any on the server)
     * @param slot   a slot type
     * @return the payload of the first stack in a slot of that type, or {@link SlotPayload#EMPTY}
     */
    public static String describe(EntityPlayer player, BaubleSlot slot) {
        ItemStack stack = BaublesCompat.first(player, slot);
        if (stack == null) {
            return SlotPayload.EMPTY;
        }
        String name = shortName(stack.getItem());
        if (stack.getItem() instanceof ItemNightVisionGoggles) {
            return SlotPayload.build(name, stack.stackSize, "nv=" + (ElectricGear.nightVisionOn(stack) ? "on" : "off"));
        }
        if (stack.getItem() instanceof ItemAmmoPouch) {
            PouchInventory view = new PouchInventory(stack, () -> {});
            String[] names = new String[view.getSizeInventory()];
            int[] counts = new int[names.length];
            for (int i = 0; i < names.length; i++) {
                ItemStack inner = view.getStackInSlot(i);
                if (inner != null) {
                    names[i] = shortName(inner.getItem());
                    counts[i] = inner.stackSize;
                }
            }
            return SlotPayload.build(name, stack.stackSize, SlotPayload.contents(names, counts));
        }
        return SlotPayload.build(name, stack.stackSize);
    }

    private static String shortName(Item item) {
        String name = Item.itemRegistry.getNameForObject(item);
        if (name == null) {
            GregGuns.LOG.warn("[gregguns] SlotSyncProbe: unregistered item {}", item);
            return "unregistered";
        }
        return name.startsWith("gregguns:") ? name.substring("gregguns:".length()) : name.replace(':', '_');
    }
}
