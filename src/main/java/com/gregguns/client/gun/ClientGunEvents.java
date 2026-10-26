package com.gregguns.client.gun;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import com.gregguns.client.GunsClientModule;
import com.gregguns.client.render.GunAnimations;
import com.gregguns.client.render.GunRenderers;
import com.gregguns.gun.GunClientEvents;
import com.gregguns.gun.logic.AdsSupport;
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

    private final GunAnimations animations;
    private boolean reloading;
    private long lastShotTick = com.gregguns.gun.logic.LoopStart.NEVER;

    /**
     * @param animations the rendering framework's animation state: recoil, muzzle flash, reload pose and the zoom
     *                   (which
     *                   owns the FOV change and the scope overlay, so nothing else touches the FOV)
     */
    public ClientGunEvents(GunAnimations animations) {
        this.animations = animations;
    }

    @Override
    public void onShotFired(EntityLivingBase entity, GunStats stats, int attackType) {
        // Each shot reaches this interface once per entity (the local mirror or the server's event), so every entity
        // animates here, the local player included.
        animations.onShotFired(entity, attackType);
        ItemGun gun = heldGun(entity);
        if (isLocal(entity) && gun != null) {
            long now = entity.worldObj.getTotalWorldTime();
            GunsClientModule.sounds()
                .playFire((EntityPlayer) entity, gun.shotEvent(now, lastShotTick));
            lastShotTick = now;
            if (gun.rechamberEvent() != null) {
                GunsClientModule.sounds()
                    .playHandling((EntityPlayer) entity, gun.rechamberEvent());
            }
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
        animations.onReloadStarted(entity, ticks);
        ItemGun gun = heldGun(entity);
        if (isLocal(entity) && gun != null) {
            reloading = true;
            // The sound ends by itself when the reload is canceled or the gun is no longer held.
            GunsClientModule.sounds()
                .startReload((EntityPlayer) entity, gun.reloadEvent(), () -> reloading && heldGun(entity) == gun);
        }
    }

    @Override
    public void onReloadCanceled(EntityLivingBase entity) {
        animations.onReloadCanceled(entity);
        if (isLocal(entity)) {
            reloading = false;
            GunsClientModule.sounds()
                .stopReload();
        }
    }

    @Override
    public void setZoom(EntityLivingBase entity, boolean zoomed, double fovMultiplier) {
        if (!isLocal(entity)) {
            return;
        }
        // Aim down sights: the model move for every gun on the ADS list, whether or not it also has a scope FOV.
        ItemGun aimGun = heldGun(entity);
        boolean sights = aimGun != null && AdsSupport.supports(aimGun.contentName());
        animations.ads()
            .setAiming(
                sights ? aimGun : null,
                zoomed && sights,
                sights && !aimGun.stats()
                    .hasScope());
        if (zoomed && fovMultiplier < 1.0) {
            // The FOV factor is the gun's GunStats zoom; the overlay is drawn only if its render profile has a
            // ScopeSpec (texture and kick), so both are per-gun data.
            ItemGun gun = heldGun(entity);
            animations.setZoom((float) fovMultiplier, gun != null && GunRenderers.hasScopeOverlay(gun));
        } else {
            animations.clearZoom();
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
