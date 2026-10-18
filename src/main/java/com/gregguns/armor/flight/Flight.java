package com.gregguns.armor.flight;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import com.gregguns.armor.slots.BaublesCompat;
import com.gregguns.armor.slots.logic.BaubleSlot;

/**
 * Entry points of the back-device flight system: the server tracker, the client mirror and a lookup of the worn
 * device. Both are runtime trackers, not saved state.
 *
 * <p>
 * Usage: {@code Flight.server().publish(player, status)} on the server; {@code Flight.mirror().status(player)} on the
 * client. Server and client; each side touches only its own half.
 */
// Spec: docs/gregguns-spec/05-armor/powered-flight.md
public final class Flight {

    // One tracker per logical side's JVM role; a registry-like singleton, as ArmorModule's event handler is.
    private static final FlightServer SERVER = new FlightServer();
    private static final FlightMirror MIRROR = new FlightMirror();

    private Flight() {}

    /** @return the server-side tracker */
    public static FlightServer server() {
        return SERVER;
    }

    /** @return the client-side mirror of the local player's status */
    public static FlightMirror mirror() {
        return MIRROR;
    }

    /**
     * @param player a player
     * @return the worn stack in the {@code wings} slot when it is one of the three flight devices, else null
     */
    public static @Nullable ItemStack wornDevice(EntityPlayer player) {
        ItemStack stack = BaublesCompat.first(player, BaubleSlot.WINGS);
        if (stack == null) {
            return null;
        }
        Item item = stack.getItem();
        return item instanceof ItemGlider || item instanceof ItemJumpPack || item instanceof ItemAntiGravityDevice
            ? stack
            : null;
    }

    /**
     * @param player a player
     * @return whether the player is on the ground, in water, on a ladder or riding: the cases where no device acts
     */
    public static boolean airborne(EntityPlayer player) {
        return !player.onGround && !player.isInWater() && !player.isOnLadder() && player.ridingEntity == null;
    }
}
