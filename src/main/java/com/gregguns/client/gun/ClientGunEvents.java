package com.gregguns.client.gun;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import com.gregguns.client.GunsClientModule;
import com.gregguns.gun.GunClientEvents;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.item.gun.ItemGun;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The client implementation of {@link GunClientEvents} installed by {@code GunsClientModule}. For now it only plays the
 * local player's predicted sounds and applies zoom to the camera; the rendering framework wraps it to add the
 * animations (see the Javadoc on {@link GunClientEvents}).
 *
 * <p>
 * Only the local player's events make sound here. Other shooters are heard through the server's sound packets, which
 * exclude the shooter, so nothing plays twice. Client only.
 */
@SideOnly(Side.CLIENT)
public final class ClientGunEvents implements GunClientEvents {

    private final ZoomView zoom;
    private boolean reloading;

    /** @param zoom the camera zoom to drive */
    public ClientGunEvents(ZoomView zoom) {
        this.zoom = zoom;
    }

    @Override
    public void onShotFired(EntityLivingBase entity, GunStats stats, int attackType) {
        ItemGun gun = heldGun(entity);
        if (isLocal(entity) && gun != null) {
            GunsClientModule.sounds()
                .playFire((EntityPlayer) entity, gun.fireEvent());
        }
    }

    @Override
    public void onDryFire(EntityLivingBase entity, GunStats stats) {
        if (isLocal(entity)) {
            GunsClientModule.sounds()
                .playDryFire((EntityPlayer) entity);
        }
    }

    @Override
    public void onReloadStarted(EntityLivingBase entity, GunStats stats, int ticks) {
        ItemGun gun = heldGun(entity);
        if (isLocal(entity) && gun != null) {
            reloading = true;
            // The sound ends by itself when the reload is cancelled or the gun is no longer held.
            GunsClientModule.sounds()
                .startReload((EntityPlayer) entity, gun.reloadEvent(), () -> reloading && heldGun(entity) == gun);
        }
    }

    @Override
    public void onReloadCancelled(EntityLivingBase entity) {
        if (isLocal(entity)) {
            reloading = false;
            GunsClientModule.sounds()
                .stopReload();
        }
    }

    @Override
    public void setZoom(EntityLivingBase entity, boolean zoomed, double fovMultiplier) {
        if (isLocal(entity)) {
            zoom.set(zoomed ? fovMultiplier : 1.0);
        }
    }

    private static boolean isLocal(EntityLivingBase entity) {
        return entity == Minecraft.getMinecraft().thePlayer;
    }

    private static ItemGun heldGun(EntityLivingBase entity) {
        ItemStack held = entity.getHeldItem();
        return held != null && held.getItem() instanceof ItemGun ? (ItemGun) held.getItem() : null;
    }
}
