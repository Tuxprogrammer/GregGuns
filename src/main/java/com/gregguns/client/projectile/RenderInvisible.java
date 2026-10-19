package com.gregguns.client.projectile;

import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

/**
 * Draws nothing. For an entity whose whole look is the particles it spawns (the Flamethrower's flame). Client only.
 */
// Spec: docs/techguns-spec/04-projectiles-fx/renderers.md (RenderDummy)
public final class RenderInvisible extends Render {

    @Override
    public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTicks) {}

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return null;
    }
}
