package com.gregguns.armor.flight;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraft.util.Vec3;

import com.gregguns.api.Tier;
import com.gregguns.armor.flight.logic.BackKind;
import com.gregguns.armor.flight.logic.FlightMode;
import com.gregguns.armor.flight.logic.FlightPhysics;
import com.gregguns.armor.flight.logic.FlightPolicy;
import com.gregguns.armor.flight.logic.FlightStatus;
import com.gregguns.armor.slots.ItemBackDevice;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The Glider: an unpowered back device in the {@code wings} slot. While airborne and sneaking, or while the
 * back-device key is held, fall speed becomes forward speed (about 0.18 blocks per tick of descent) and fall damage is
 * negated. No energy, no thrust. The server clears the fall distance and tells the client; the client applies the
 * motion ({@code com.gregguns.client.flight.FlightClient}), because 1.7.10 clients own their motion.
 *
 * <p>
 * Server and client.
 */
// Spec: docs/techguns-spec/05-armor/special-equipment.md 4.2; docs/gregguns-spec/05-armor/powered-flight.md
// Deviation: the key-held trigger is new (the task and the back-device framework provide a key); Techguns glides on
// sneak only. Sneak still works.
public final class ItemGlider extends ItemBackDevice {

    /** Registry name. */
    public static final String NAME = "glider";

    @Override
    public Tier getTier() {
        return Tier.HV;
    }

    @Override
    public void onBackTick(EntityPlayer player, ItemStack stack) {
        FlightServer server = Flight.server();
        boolean held = server.keyHeld(player);
        FlightMode mode = FlightPolicy.mode(
            BackKind.GLIDER,
            held,
            true,
            Flight.airborne(player),
            player.isSneaking(),
            player.capabilities.isFlying);
        if (mode == FlightMode.GLIDE) {
            // The same formula the client predicts with; on a real player the server copy of motion is overwritten by
            // the client's position packets, so only fallDistance below matters there.
            Vec3 look = player.getLookVec();
            FlightPhysics.Impulse glide = FlightPhysics.glide(player.motionY, look.xCoord, look.zCoord);
            player.motionX += glide.x;
            player.motionY += glide.y;
            player.motionZ += glide.z;
            player.fallDistance = 0.0F;
            server.markAssisted(player);
        }
        server.publish(player, new FlightStatus(BackKind.GLIDER, held, true));
    }

    @Override
    public void onBackKey(EntityPlayer player, ItemStack stack, boolean down) {
        Flight.server()
            .setKey(player, down);
    }

    @Override
    public void onBackRemoved(EntityPlayer player, Item item) {
        Flight.server()
            .forget(player);
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        tooltip.add(StatCollector.translateToLocal("gregguns.flight.tooltip.glider"));
        super.addInformation(stack, player, tooltip, advanced);
    }
}
