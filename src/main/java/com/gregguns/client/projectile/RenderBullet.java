package com.gregguns.client.projectile;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.gregguns.GregGuns;

/**
 * Draws a bullet as two crossed quads along its flight direction, using the top-left slice of
 * {@code textures/entity/bullet.png}. Plain GL and the shared {@link Tessellator}, no model parts, so Angelica can
 * treat the renderer consistently; every GL state change is undone in a {@code finally}. Client only.
 */
// Spec: docs/techguns-spec/04-projectiles-fx/renderers.md (RenderGenericBullet);
// docs/gregguns-spec/03-gun-visuals-audio/angelica-item-rendering.md (GGS-03-02, GGS-03-06)
public final class RenderBullet extends Render {

    private static final ResourceLocation TEXTURE = new ResourceLocation(GregGuns.MODID, "textures/entity/bullet.png");

    /** Half the length of the bullet along its flight, blocks. */
    private static final double HALF_LENGTH = 0.1;
    /** Half the width of each quad, blocks. */
    private static final double HALF_WIDTH = 0.025;
    /** The sampled slice of the 32 by 32 texture. */
    private static final double U_MAX = 0.5;
    private static final double V_MAX = 5.0 / 32.0;
    /** Ticks a bullet is hidden after spawning, so it does not flash at the muzzle. */
    private static final int HIDDEN_TICKS = 2;

    @Override
    public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTicks) {
        if (entity.ticksExisted < HIDDEN_TICKS) {
            return;
        }
        bindTexture(TEXTURE);
        float heading = entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partialTicks;
        float pitch = entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks;
        boolean lighting = GL11.glIsEnabled(GL11.GL_LIGHTING);
        GL11.glPushMatrix();
        try {
            GL11.glTranslated(x, y, z);
            // The bullet is modelled along +X; yaw is atan2(x, z), so turn by yaw - 90 about Y, then tilt about Z.
            GL11.glRotatef(heading - 90.0F, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(pitch, 0.0F, 0.0F, 1.0F);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_CULL_FACE);
            Tessellator t = Tessellator.instance;
            t.startDrawingQuads();
            t.setBrightness(entity.getBrightnessForRender(partialTicks));
            t.setColorOpaque_F(1.0F, 1.0F, 1.0F);
            // Quad in the XY plane.
            quad(t, 0.0, HALF_WIDTH, 0.0);
            // Quad in the XZ plane.
            quad(t, 0.0, 0.0, HALF_WIDTH);
            t.draw();
        } finally {
            GL11.glEnable(GL11.GL_CULL_FACE);
            if (lighting) {
                GL11.glEnable(GL11.GL_LIGHTING);
            }
            GL11.glPopMatrix();
        }
    }

    /** One quad centred on the origin, long in X, wide in Y or Z. */
    private static void quad(Tessellator t, double cx, double halfY, double halfZ) {
        t.addVertexWithUV(cx - HALF_LENGTH, -halfY, -halfZ, 0.0, V_MAX);
        t.addVertexWithUV(cx + HALF_LENGTH, -halfY, -halfZ, U_MAX, V_MAX);
        t.addVertexWithUV(cx + HALF_LENGTH, halfY, halfZ, U_MAX, 0.0);
        t.addVertexWithUV(cx - HALF_LENGTH, halfY, halfZ, 0.0, 0.0);
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return TEXTURE;
    }
}
