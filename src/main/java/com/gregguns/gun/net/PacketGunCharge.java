package com.gregguns.gun.net;

import java.io.IOException;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;

import com.gregguns.gun.server.ServerGunHandler;

/**
 * Client to server: the charge (right-click) button of a charge gun went down or up. Like {@link PacketGunTrigger}
 * it sends the edge only; the server counts the hold ticks itself, so a client cannot claim a longer charge than it
 * held. Validated by {@link GunIntentPacket}: same dimension, hotbar slot in range, a gun in that slot.
 *
 * <p>
 * Spec: docs/techguns-spec/02-guns/per-gun-behavior.md (GenericGunCharge); docs/gregguns-spec/02-guns/index.md.
 * Deviation: Techguns relies on the vanilla item-use timer and sends no release message at all, so other players
 * never see a charged shot (GUN-31); GregGuns sends both edges and the server broadcasts the shot. Side: client
 * sends, server handles.
 */
public final class PacketGunCharge extends GunIntentPacket {

    private boolean down;

    /** Required by the channel. */
    public PacketGunCharge() {}

    /**
     * @param dimension the sender's dimension
     * @param slot      the selected hotbar slot
     * @param down      true when the charge button was pressed, false when released
     */
    public PacketGunCharge(int dimension, int slot, boolean down) {
        super(dimension, slot);
        this.down = down;
    }

    @Override
    protected void encodeBody(PacketBuffer buf) throws IOException {
        buf.writeBoolean(down);
    }

    @Override
    protected void decodeBody(PacketBuffer buf) throws IOException {
        down = buf.readBoolean();
    }

    @Override
    protected void handle(EntityPlayerMP player) {
        ServerGunHandler.onCharge(player, down);
    }
}
