package com.gregguns.client.projectile;

import net.minecraft.client.renderer.entity.RenderSnowball;
import net.minecraftforge.common.MinecraftForge;

import com.gregguns.explosion.FlashHooks;
import com.gregguns.item.ammo.AmmoItems;
import com.gregguns.item.grenade.GrenadeItems;
import com.gregguns.projectile.FlashbangProjectile;
import com.gregguns.projectile.FragGrenadeProjectile;
import com.gregguns.projectile.GrenadeLauncherProjectile;
import com.gregguns.projectile.GrenadeProjectile;
import com.gregguns.projectile.RocketProjectile;

import cpw.mods.fml.client.registry.RenderingRegistry;

/**
 * Binds each grenade entity, and the two launcher rounds, to a sprite renderer: a thrown grenade is drawn as its item
 * icon, facing the camera, like
 * a thrown snowball. Also installs the flashbang white-out. Called from {@code GunsClientModule.init}. Client only.
 */
// Spec: docs/techguns-spec/04-projectiles-fx/renderers.md; a model renderer can replace this sprite later.
public final class GrenadeRenderers {

    private GrenadeRenderers() {}

    /** Registers the renderers and the flash overlay. */
    public static void register() {
        RenderingRegistry
            .registerEntityRenderingHandler(GrenadeProjectile.class, new RenderSnowball(GrenadeItems.stielgranate()));
        RenderingRegistry.registerEntityRenderingHandler(
            FragGrenadeProjectile.class,
            new RenderSnowball(GrenadeItems.fragGrenade()));
        RenderingRegistry
            .registerEntityRenderingHandler(FlashbangProjectile.class, new RenderSnowball(GrenadeItems.flashbang()));
        // The launchers' rounds are drawn as their ammo item's icon until a model replaces the sprite.
        RenderingRegistry.registerEntityRenderingHandler(
            RocketProjectile.class,
            new RenderSnowball(AmmoItems.round(com.gregguns.gun.logic.GunCatalog.AmmoIds.ROCKET)));
        RenderingRegistry.registerEntityRenderingHandler(
            GrenadeLauncherProjectile.class,
            new RenderSnowball(AmmoItems.round(com.gregguns.gun.logic.GunCatalog.AmmoIds.GRENADE_40MM)));
        FlashOverlay overlay = new FlashOverlay();
        FlashHooks.install(overlay);
        MinecraftForge.EVENT_BUS.register(overlay);
    }
}
