package com.gregguns.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.gregguns.client.anim.SpriteSheet;
import com.gregguns.client.render.profile.MuzzleFlashSpec;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Raw {@code GL11} and {@code Tessellator} drawing for muzzle flashes, written so no state leaks (Angelica rules
 * GGS-03-01 and GGS-03-02: plain GL, every push paired with a pop in a {@code finally}). Client only.
 *
 * <p>
 * Spec: rendering-framework.md sections 6.2 and 7; BUGS GVA-04 (blend function leaked) and GVA-05 (full-bright
 * lightmap left on). Both are avoided: the blend function, depth mask, lighting and cull state are saved with
 * {@code glPushAttrib}, and full brightness comes from the per-vertex brightness, so the global lightmap is never
 * touched.
 */
@SideOnly(Side.CLIENT)
final class GlFx {

    /** Packed block and sky light of 15 each. */
    private static final int FULL_BRIGHT = 15728880;
    private static final int STATE_BITS = GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT
        | GL11.GL_DEPTH_BUFFER_BIT
        | GL11.GL_CURRENT_BIT;

    private GlFx() {}

    /**
     * Draws the first-person muzzle flash screen effect in the muzzle frame. Call after the gun matrix is popped.
     * The frame is rotY(45), rotZ(90), T(1.5, -1.0, -0.5); the quad lies in the local XZ plane at
     * {@code scale x offset} with side {@code scale squared} (the Techguns ScreenEffect rule).
     *
     * @param spec     the flash data
     * @param progress progress through the flash window, 0 to 1
     */
    static void drawFirstPersonFlash(MuzzleFlashSpec spec, double progress) {
        int frame = spec.sheet()
            .frameAt(progress);
        if (frame < 0) {
            return;
        }
        GL11.glPushMatrix();
        try {
            GL11.glRotatef(45.0f, 0.0f, 1.0f, 0.0f);
            GL11.glRotatef(90.0f, 0.0f, 0.0f, 1.0f);
            GL11.glTranslatef(1.5f, -1.0f, -0.5f);
            GL11.glRotated(spec.rotX(), 1, 0, 0);
            GL11.glRotated(spec.rotY(), 0, 1, 0);
            GL11.glRotated(spec.rotZ(), 0, 0, 1);
            GL11.glScaled(spec.scale(), spec.scale(), spec.scale());
            double half = spec.scale() / 2.0;
            double x = spec.offX();
            double y = spec.offY();
            double z = spec.offZ();
            drawSprite(
                spec,
                frame,
                x - half,
                y,
                z - half,
                x - half,
                y,
                z + half,
                x + half,
                y,
                z + half,
                x + half,
                y,
                z - half);
        } finally {
            GL11.glPopMatrix();
        }
    }

    /**
     * Draws the third-person muzzle flash as two crossed planes through the barrel axis, in the gun frame. Other
     * viewers see this instead of the first-person screen effect.
     *
     * @param spec     the flash data
     * @param progress progress through the flash window
     * @param muzzle   muzzle position in the gun frame, blocks
     */
    static void drawThirdPersonFlash(MuzzleFlashSpec spec, double progress, float[] muzzle) {
        int frame = spec.sheet()
            .frameAt(progress);
        if (frame < 0) {
            return;
        }
        double len = spec.thirdPersonLength();
        double half = len / 2.0;
        double cx = muzzle[0];
        double cy = muzzle[1];
        double cz = muzzle[2] - half * 0.6;
        // Plane in X-Z, then plane in Y-Z. Both contain the barrel axis (Z).
        drawSprite(
            spec,
            frame,
            cx - half,
            cy,
            cz - half,
            cx - half,
            cy,
            cz + half,
            cx + half,
            cy,
            cz + half,
            cx + half,
            cy,
            cz - half);
        drawSprite(
            spec,
            frame,
            cx,
            cy - half,
            cz - half,
            cx,
            cy - half,
            cz + half,
            cx,
            cy + half,
            cz + half,
            cx,
            cy + half,
            cz - half);
    }

    private static void drawSprite(MuzzleFlashSpec spec, int frame, double x1, double y1, double z1, double x2,
        double y2, double z2, double x3, double y3, double z3, double x4, double y4, double z4) {
        SpriteSheet sheet = spec.sheet();
        double u0 = sheet.u0(frame);
        double u1 = sheet.u1(frame);
        double v0 = sheet.v0(frame);
        double v1 = sheet.v1(frame);
        Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(new ResourceLocation(spec.texture()));
        GL11.glPushAttrib(STATE_BITS);
        try {
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glDepthMask(false);
            GL11.glEnable(GL11.GL_BLEND);
            if (spec.additive()) {
                GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
            } else {
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            }
            Tessellator t = Tessellator.instance;
            t.startDrawingQuads();
            t.setBrightness(FULL_BRIGHT);
            t.setColorRGBA_F(1.0f, 1.0f, 1.0f, 1.0f);
            t.addVertexWithUV(x1, y1, z1, u0, v0);
            t.addVertexWithUV(x2, y2, z2, u0, v1);
            t.addVertexWithUV(x3, y3, z3, u1, v1);
            t.addVertexWithUV(x4, y4, z4, u1, v0);
            t.draw();
        } finally {
            GL11.glPopAttrib();
        }
    }
}
