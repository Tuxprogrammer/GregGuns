package com.gregguns.client.projectile;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.gregguns.GregGuns;
import com.gregguns.client.render.GunRenderers;
import com.gregguns.projectile.Ballistics;
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

    /** Every bit of state this renderer touches. */
    private static final int STATE_BITS = GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT
        | GL11.GL_DEPTH_BUFFER_BIT
        | GL11.GL_CURRENT_BIT
        | GL11.GL_LIGHTING_BIT;

    @Override
    public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTicks) {
        BeamVisual beam = (BeamVisual) entity;
        float alpha = BeamMath.alpha(beam.ticksExisted, beam.getLifetime(), partialTicks);
        if (alpha <= 0.0F || beam.getLength() <= 0.0F) {
            return;
        }
        double half = HALF_WIDTH * BeamMath.flicker(beam.getSeed(), beam.ticksExisted);
        float heading = beam.prevRotationYaw + (beam.rotationYaw - beam.prevRotationYaw) * partialTicks;
        float pitch = beam.prevRotationPitch + (beam.rotationPitch - beam.prevRotationPitch) * partialTicks;
        double length = beam.getLength();
        double[] origin = { x, y, z };
        double[] shifted = fromMuzzle(origin, heading, pitch, length);
        if (shifted != null) {
            if (shifted.length == 0) {
                return;
            }
            x = shifted[0];
            y = shifted[1];
            z = shifted[2];
            heading = Ballistics.yawOf(shifted[3], shifted[5]);
            pitch = Ballistics.pitchOf(shifted[3], shifted[4], shifted[5]);
            length = shifted[6];
        }
        bindTexture(getEntityTexture(entity));
        // Everything below is translucent FX: save the enable, blend, depth-mask and lighting state and restore it
        // exactly, so later passes (water under shaders) see what they expect.
        GL11.glPushAttrib(STATE_BITS);
        GL11.glPushMatrix();
        try {
            GL11.glTranslated(x, y, z);
            // Modeled along +X; yaw is atan2(x, z), so turn by yaw - 90 about Y, then tilt about Z (as RenderBullet).
            GL11.glRotatef(heading - 90.0F, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(pitch, 0.0F, 0.0F, 1.0F);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            GL11.glDepthMask(false);
            Tessellator t = Tessellator.instance;
            t.startDrawingQuads();
            t.setBrightness(240);
            t.setColorRGBA_F(1.0F, 1.0F, 1.0F, alpha);
            quad(t, length, half, 0.0);
            quad(t, length, 0.0, half);
            t.draw();
        } finally {
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }

    /**
     * In first person, a beam that starts at the viewer's own eye is moved to the held gun's muzzle (and keeps its end
     * point), so it does not start behind the near plane and cover the gun.
     *
     * @return {@code null} for no change, an empty array to skip the beam, else {start xyz, dir xyz, length}
     */
    private double[] fromMuzzle(double[] start, float heading, float pitch, double length) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.gameSettings.thirdPersonView != 0 || mc.thePlayer == null
            || mc.renderViewEntity != mc.thePlayer
            || !BeamOrigin.isOwnShot(start, new double[] { 0.0, 0.0, 0.0 })) {
            return null;
        }
        double[] eye = GunRenderers.firstPersonMuzzle(mc.thePlayer.getHeldItem());
        if (eye == null) {
            return null;
        }
        double[] muzzle = BeamOrigin.eyeToWorld(eye, renderManager.playerViewY, renderManager.playerViewX);
        double yaw = Math.toRadians(heading);
        double p = Math.toRadians(pitch);
        double[] dir = { Math.sin(yaw) * Math.cos(p), Math.sin(p), Math.cos(yaw) * Math.cos(p) };
        double[] r = BeamOrigin.fromMuzzle(start, dir, length, muzzle);
        return r == null ? new double[0] : r;
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
