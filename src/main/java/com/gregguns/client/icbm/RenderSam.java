package com.gregguns.client.icbm;

import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Draws the SAM in flight. Placeholder: the SAM shows as its flame and smoke trail (spawned by the entity) until the
 * I4 model lands and replaces this by path. Client only.
 */
// Spec: docs/gregguns-icbm-spec/05-detection-defense/air-defense-battery.md (SAM)
@SideOnly(Side.CLIENT)
public final class RenderSam extends Render {

    @Override
    public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTicks) {
        // Nothing to draw yet: the trail particles mark the SAM.
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return null;
    }
}
