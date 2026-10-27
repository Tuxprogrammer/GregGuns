package com.gregguns.client.icbm;

import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.gregguns.icbm.warhead.EntityFlyingBlock;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Draws a block lifted by the anti-gravitational warhead as a spinning block, like the inventory view of the block it
 * was. One draw call per entity, no world reads. Client only.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/blast-types.md (Anti-gravitational)
@SideOnly(Side.CLIENT)
public final class RenderFlyingBlock extends Render {

    private final RenderBlocks blocks = new RenderBlocks();

    /** Builds the renderer. */
    public RenderFlyingBlock() {
        this.shadowSize = 0.0F;
    }

    @Override
    public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTicks) {
        EntityFlyingBlock e = (EntityFlyingBlock) entity;
        bindTexture(TextureMap.locationBlocksTexture);
        GL11.glPushMatrix();
        GL11.glTranslatef((float) x, (float) y + 0.5F, (float) z);
        GL11.glRotatef((e.ticksExisted + partialTicks) * 3.0F, 0.0F, 1.0F, 0.3F);
        GL11.glScalef(0.98F, 0.98F, 0.98F);
        blocks.renderBlockAsItem(e.block(), e.meta(), 1.0F);
        GL11.glPopMatrix();
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return TextureMap.locationBlocksTexture;
    }
}
