package com.gregguns.client.projectile;

import com.gregguns.projectile.BeamVisual;
import com.gregguns.projectile.EnergyBlastProjectile;
import com.gregguns.projectile.FlamethrowerProjectile;
import com.gregguns.projectile.GenericProjectile;
import com.gregguns.projectile.LaserProjectile;
import com.gregguns.projectile.SimpleProjectile;
import com.gregguns.projectile.TeslaProjectile;

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
        // The flame is drawn by the particles its client copy spawns each tick; the entity itself draws nothing.
        RenderingRegistry.registerEntityRenderingHandler(FlamethrowerProjectile.class, new RenderInvisible());
        // The laser is an instant hitscan drawn by its BeamVisual; the carrier entity itself draws nothing.
        RenderingRegistry.registerEntityRenderingHandler(LaserProjectile.class, new RenderInvisible());
        RenderingRegistry.registerEntityRenderingHandler(TeslaProjectile.class, new RenderInvisible());
        RenderingRegistry.registerEntityRenderingHandler(BeamVisual.class, new RenderBeam());
        // Impact and trail effects of the energy blast come with the FX core.
        RenderingRegistry.registerEntityRenderingHandler(EnergyBlastProjectile.class, new RenderInvisible());
    }
}
