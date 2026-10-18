package com.gregguns.client.gun;

import net.minecraftforge.client.event.FOVUpdateEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The local player's zoom as a field-of-view factor. The server decides zoom and tells the client
 * ({@code PacketGunEvent} ZOOM); this applies the answer to the camera. Client only.
 */
// Spec: docs/techguns-spec/02-guns/gun-lifecycle.md section 4 (the FOV event multiplies the new FOV by the zoom).
// Deviation: Techguns keeps the zoom flag on the client and sends it to the server (GUN-12).
@SideOnly(Side.CLIENT)
public final class ZoomView {

    private volatile float multiplier = 1.0f;

    /** @param fovMultiplier the factor to apply to the field of view; 1 means no zoom */
    public void set(double fovMultiplier) {
        this.multiplier = (float) Math.max(0.01, Math.min(1.0, fovMultiplier));
    }

    /** @return the current factor, 1 when not zoomed */
    public float multiplier() {
        return multiplier;
    }

    /** Applies the factor to the camera. */
    @SubscribeEvent
    public void onFov(FOVUpdateEvent event) {
        event.newfov *= multiplier;
    }
}
