package com.gregguns.gun;

import net.minecraft.entity.EntityLivingBase;

/**
 * The server's answers to the <em>local</em> player, delivered by {@code PacketGunEvent} and consumed by the client
 * mirror ({@code com.gregguns.client.gun.ClientGunMirror}). The mirror predicts shots and reloads for animation and
 * sound; these calls correct it when the server disagrees or decides something the client did not predict.
 *
 * <p>
 * Client thread only. Common code reaches the mirror through {@link GunClientHooks#sync()}, so no client class is
 * loaded on a dedicated server.
 */
public interface GunClientSync {

    /** The server started a reload for the local player (an automatic one, or one the client predicted). */
    void serverReloadStarted(EntityLivingBase player, int ticks);

    /** The server canceled, refused or finished a reload. The mirror stops its own reload state. */
    void serverReloadEnded(EntityLivingBase player);

    /** The server set the local player's zoom. */
    void serverZoom(EntityLivingBase player, boolean zoomed);

    /** A synced no-op, used until the client installs the mirror and on a dedicated server. */
    GunClientSync NONE = new GunClientSync() {

        @Override
        public void serverReloadStarted(EntityLivingBase player, int ticks) {}

        @Override
        public void serverReloadEnded(EntityLivingBase player) {}

        @Override
        public void serverZoom(EntityLivingBase player, boolean zoomed) {}
    };
}
