package com.gregguns.client.projectile;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.gregguns.GregGuns;

/**
 * Draws a dropping ball ({@code SimpleProjectile}) as a camera-facing square, 0.1 blocks across, with the whole
 * {@code textures/entity/handgun_bullet.png}. Plain GL and the shared {@link Tessellator}; GL state is restored in a
 * {@code finally}. Client only.
 */
// Spec: docs/techguns-spec/04-projectiles-fx/renderers.md (RenderSimpleProjectile)
public final class RenderSimpleBullet extends Render {

    private static final ResourceLocation TEXTURE = new ResourceLocation(
        GregGuns.MODID,
        "textures/entity/handgun_bullet.png");

    private static final double HALF_SIZE = 0.05;
    private static final int HIDDEN_TICKS = 2;

    @Override
    public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTicks) {
        if (entity.ticksExisted < HIDDEN_TICKS) {
            return;
        }
        bindTexture(TEXTURE);
        boolean lighting = GL11.glIsEnabled(GL11.GL_LIGHTING);
        GL11.glPushMatrix();
        try {
            GL11.glTranslated(x, y, z);
            GL11.glRotatef(180.0F - renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(-renderManager.playerViewX, 1.0F, 0.0F, 0.0F);
            GL11.glDisable(GL11.GL_LIGHTING);
            Tessellator t = Tessellator.instance;
            t.startDrawingQuads();
            t.setBrightness(entity.getBrightnessForRender(partialTicks));
            t.setColorOpaque_F(1.0F, 1.0F, 1.0F);
            t.addVertexWithUV(-HALF_SIZE, -HALF_SIZE, 0.0, 0.0, 1.0);
            t.addVertexWithUV(HALF_SIZE, -HALF_SIZE, 0.0, 1.0, 1.0);
            t.addVertexWithUV(HALF_SIZE, HALF_SIZE, 0.0, 1.0, 0.0);
            t.addVertexWithUV(-HALF_SIZE, HALF_SIZE, 0.0, 0.0, 0.0);
            t.draw();
        } finally {
            if (lighting) {
                GL11.glEnable(GL11.GL_LIGHTING);
            }
            GL11.glPopMatrix();
        }
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return TEXTURE;
    }
}
