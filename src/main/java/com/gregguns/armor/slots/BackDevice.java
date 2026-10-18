package com.gregguns.armor.slots;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * The hook for a device worn in the Baubles {@code wings} slot. The slot holds one item, so only one back device is
 * active at a time and flight gear cannot stack. {@link SlotEquipment} calls the hooks; a later branch adds the glider,
 * jump pack and anti-gravity device by implementing this interface on a bauble item (extend {@link ItemBackDevice},
 * which does the Baubles plumbing), and calls {@link SlotEquipment#backKey} from its own keybind for held-key actions.
 *
 * <p>
 * All hooks run on the server thread. The client learns a device's effect through ordinary vanilla sync (motion, air,
 * potion effects) or its own packet; nothing here mutates a stack on the client (fixes ARM-34).
 *
 * <p>
 * Usage:
 *
 * <pre>
 * public final class ItemGlider extends ItemBackDevice {
 *
 *     public void onBackTick(EntityPlayer player, ItemStack stack) {
 *         // glide while sneaking and falling
 *     }
 * }
 * </pre>
 */
// Spec: docs/gregguns-spec/05-armor/baubles-slots.md (Back (1) row; GGS-05-02, GGS-05-05)
public interface BackDevice {

    /**
     * Called every server tick while the device is worn in the {@code wings} slot.
     *
     * @param player the wearer
     * @param stack  the worn stack
     */
    void onBackTick(EntityPlayer player, ItemStack stack);

    /**
     * Called once when the device stops being worn: unequipped, replaced by another device, or the player left the
     * world. Undo anything the device applied.
     *
     * @param player the former wearer
     * @param item   the item that was worn
     */
    default void onBackRemoved(EntityPlayer player, Item item) {}

    /**
     * Called when the player presses or releases the back-device action key.
     *
     * @param player the wearer
     * @param stack  the worn stack
     * @param down   true on press, false on release
     */
    default void onBackKey(EntityPlayer player, ItemStack stack, boolean down) {}
}
