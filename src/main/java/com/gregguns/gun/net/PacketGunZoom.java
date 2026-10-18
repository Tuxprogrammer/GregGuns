package com.gregguns.gun.net;

import net.minecraft.entity.player.EntityPlayerMP;

import com.gregguns.gun.server.ServerGunHandler;

/**
 * Client to server: the player right-clicked a scoped gun and wants to toggle zoom. It carries no zoom flag: the
 * server flips its own state and answers with {@link PacketGunEvent} {@code ZOOM}. Fixes GUN-12 and GUN-47.
 *
 * <p>
 * Spec: docs/techguns-spec/02-guns/gun-lifecycle.md section 4 (right mouse button); docs/design/0009 row 7 (right
 * click zoom stays). Side: client sends, server handles.
 */
public final class PacketGunZoom extends GunIntentPacket {

    /** Required by the channel. */
    public PacketGunZoom() {}

    /**
     * @param dimension the sender's dimension
     * @param slot      the selected hotbar slot
     */
    public PacketGunZoom(int dimension, int slot) {
        super(dimension, slot);
    }

    @Override
    protected void handle(EntityPlayerMP player) {
        ServerGunHandler.onZoomToggle(player);
    }
}
