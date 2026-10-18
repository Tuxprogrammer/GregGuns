package com.gregguns.client.projectile;

import com.gregguns.projectile.GenericProjectile;
import com.gregguns.projectile.SimpleProjectile;

import cpw.mods.fml.client.registry.RenderingRegistry;

/**
 * Binds the projectile entities to their renderers. Called from {@code GunsClientModule.init}. Client only.
 */
public final class ProjectileRenderers {

    private ProjectileRenderers() {}

    /**
     * Registers one renderer per entity class (a stable renderer instance per class keeps Angelica's batching verdict
     * stable).
     */
    public static void register() {
        RenderingRegistry.registerEntityRenderingHandler(GenericProjectile.class, new RenderBullet());
        RenderingRegistry.registerEntityRenderingHandler(SimpleProjectile.class, new RenderSimpleBullet());
    }
}
