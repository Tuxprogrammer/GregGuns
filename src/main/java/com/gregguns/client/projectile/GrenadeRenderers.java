package com.gregguns.client.projectile;

import net.minecraft.client.renderer.entity.RenderSnowball;

import com.gregguns.item.grenade.GrenadeItems;
import com.gregguns.projectile.GrenadeProjectile;

import cpw.mods.fml.client.registry.RenderingRegistry;

/**
 * Binds the grenade entity to a sprite renderer: the thrown Stielgranate is drawn as its item icon, facing the camera,
 * like a thrown snowball. Called from {@code GunsClientModule.init}. Client only.
 */
// Spec: docs/techguns-spec/04-projectiles-fx/renderers.md; a model renderer can replace this sprite later.
public final class GrenadeRenderers {

    private GrenadeRenderers() {}

    /** Registers the renderer. */
    public static void register() {
        RenderingRegistry
            .registerEntityRenderingHandler(GrenadeProjectile.class, new RenderSnowball(GrenadeItems.stielgranate()));
    }
}
