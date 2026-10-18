package com.gregguns.gun.net;

import java.io.IOException;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;

import com.gregguns.gun.server.ServerGunHandler;

/**
 * Client to server: the fire button went down or up. The server's fire control decides whether a shot results, from
 * its own ammo and cooldown. Sent on change only, so a held button costs no traffic.
 *
 * <p>
 * Spec: docs/techguns-spec/02-guns/gun-lifecycle.md section 4 (input) and 5 (firing). Deviation: Techguns sends one
 * {@code PacketShootGun} per predicted shot with a zoom flag the server trusts (GUN-12); GregGuns sends edges only and
 * the server owns every shot. Side: client sends, server handles.
 */
public final class PacketGunTrigger extends GunIntentPacket {

    private boolean down;

    /** Required by the channel. */
    public PacketGunTrigger() {}

    /**
     * @param dimension the sender's dimension
     * @param slot      the selected hotbar slot
     * @param down      true when the button was pressed, false when released
     */
    public PacketGunTrigger(int dimension, int slot, boolean down) {
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
        ServerGunHandler.onTrigger(player, down);
    }
}
