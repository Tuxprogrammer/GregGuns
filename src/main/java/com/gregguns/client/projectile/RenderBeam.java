package com.gregguns.client.projectile;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.gregguns.GregGuns;
import com.gregguns.projectile.BeamMath;
import com.gregguns.projectile.BeamVisual;

/**
 * Draws a {@link BeamVisual}: two crossed textured quads from the beam's start along its direction, additively
 * blended, fading out over the beam's life with a seeded width flicker ({@link BeamMath}). The texture is
 * {@code textures/entity/laser_beam.png} or {@code blue_beam.png}, stretched over the whole beam. Plain GL and the
 * shared {@link Tessellator}; GL state is restored in a {@code finally}. Client only.
 */
// Spec: docs/techguns-spec/04-projectiles-fx/renderers.md (RenderLaserBeam2)
public final class RenderBeam extends Render {

    private static final ResourceLocation LASER = new ResourceLocation(
        GregGuns.MODID,
        "textures/entity/laser_beam.png");
    private static final ResourceLocation BLUE = new ResourceLocation(GregGuns.MODID, "textures/entity/blue_beam.png");

    /** Half the width of the beam at full strength, blocks. */
    private static final double HALF_WIDTH = 0.06;

    @Override
    public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTicks) {
        BeamVisual beam = (BeamVisual) entity;
        float alpha = BeamMath.alpha(beam.ticksExisted, beam.getLifetime(), partialTicks);
        if (alpha <= 0.0F || beam.getLength() <= 0.0F) {
            return;
        }
        double half = HALF_WIDTH * BeamMath.flicker(beam.getSeed(), beam.ticksExisted);
        bindTexture(getEntityTexture(entity));
        float heading = beam.prevRotationYaw + (beam.rotationYaw - beam.prevRotationYaw) * partialTicks;
        float pitch = beam.prevRotationPitch + (beam.rotationPitch - beam.prevRotationPitch) * partialTicks;
        boolean lighting = GL11.glIsEnabled(GL11.GL_LIGHTING);
        GL11.glPushMatrix();
        try {
            GL11.glTranslated(x, y, z);
            // Modeled along +X; yaw is atan2(x, z), so turn by yaw - 90 about Y, then tilt about Z (as RenderBullet).
            GL11.glRotatef(heading - 90.0F, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(pitch, 0.0F, 0.0F, 1.0F);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glEnable(GL11.GL_BLEND);
            OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ZERO);
            GL11.glDepthMask(false);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);
            Tessellator t = Tessellator.instance;
            t.startDrawingQuads();
            t.setBrightness(240);
            t.setColorRGBA_F(1.0F, 1.0F, 1.0F, alpha);
            double length = beam.getLength();
            quad(t, length, half, 0.0);
            quad(t, length, 0.0, half);
            t.draw();
        } finally {
            GL11.glDepthMask(true);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glEnable(GL11.GL_CULL_FACE);
            if (lighting) {
                GL11.glEnable(GL11.GL_LIGHTING);
            }
            GL11.glPopMatrix();
        }
    }

    /** One quad from the origin to {@code length} along X, wide in Y or Z, with the whole texture on it. */
    private static void quad(Tessellator t, double length, double halfY, double halfZ) {
        t.addVertexWithUV(0.0, -halfY, -halfZ, 0.0, 1.0);
        t.addVertexWithUV(length, -halfY, -halfZ, 1.0, 1.0);
        t.addVertexWithUV(length, halfY, halfZ, 1.0, 0.0);
        t.addVertexWithUV(0.0, halfY, halfZ, 0.0, 0.0);
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return ((BeamVisual) entity).getStyle() == BeamVisual.STYLE_BLUE ? BLUE : LASER;
    }
}
