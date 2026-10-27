package com.gregguns.client.icbm;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.gregguns.icbm.silo.EntitySiloDoors;
import com.gregguns.icbm.silo.SiloDoors;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Draws the silo's two door panels (D89): thin slabs that lie across the 3x3 shaft opening at the rim, each hinged on
 * its outer edge, and swing up and outward to rest leaning out on each side. The entity stands at the middle of the
 * opening on the rim's top plane. The panels use the Gantry Casing texture. Client only.
 */
// Spec: docs/gregguns-icbm-spec/03-missiles/missile-silo.md (The doors)
@SideOnly(Side.CLIENT)
public final class RenderSiloDoors extends Render {

    private static final ResourceLocation TEXTURE = new ResourceLocation(
        "gregguns",
        "textures/blocks/casings/gantry_casing.png");
    /** Half the shaft width: the hinge sits this far from the middle. */
    private static final double HINGE = 1.5;
    /** Panel length from hinge to the split, blocks. */
    private static final double LENGTH = 1.5;
    /** Panel depth along the rim, blocks. */
    private static final double DEPTH = 3.0;
    /** Panel thickness, blocks: 2/16. */
    private static final double THICK = 0.125;

    @Override
    public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTicks) {
        EntitySiloDoors doors = (EntitySiloDoors) entity;
        float angle = (float) (doors.progress(partialTicks) * SiloDoors.OPEN_DEGREES);
        Minecraft.getMinecraft().renderEngine.bindTexture(TEXTURE);
        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);
        GL11.glDisable(GL11.GL_CULL_FACE);
        int light = entity.getBrightnessForRender(partialTicks);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light % 65536, light / 65536);
        // Left panel: hinge at -x, turns counterclockwise (its free edge rises and leans to -x).
        GL11.glPushMatrix();
        GL11.glTranslated(-HINGE, 0.0, 0.0);
        GL11.glRotatef(angle, 0.0F, 0.0F, 1.0F);
        panel(0.0);
        GL11.glPopMatrix();
        // Right panel: hinge at +x, the mirror image.
        GL11.glPushMatrix();
        GL11.glTranslated(HINGE, 0.0, 0.0);
        GL11.glRotatef(-angle, 0.0F, 0.0F, 1.0F);
        panel(-LENGTH);
        GL11.glPopMatrix();
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glPopMatrix();
    }

    /**
     * A slab from {@code x0} to {@code x0 + LENGTH} along x, {@code THICK} below the hinge line, {@code DEPTH} deep.
     */
    private static void panel(double x0) {
        double x1 = x0 + LENGTH;
        double y0 = -THICK;
        double y1 = 0.0;
        double z0 = -DEPTH / 2.0;
        double z1 = DEPTH / 2.0;
        Tessellator t = Tessellator.instance;
        t.startDrawingQuads();
        // top (full light)
        t.setColorOpaque_F(1.0F, 1.0F, 1.0F);
        quad(t, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, LENGTH, DEPTH);
        // bottom
        t.setColorOpaque_F(0.5F, 0.5F, 0.5F);
        quad(t, x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0, LENGTH, DEPTH);
        // sides
        t.setColorOpaque_F(0.8F, 0.8F, 0.8F);
        quad(t, x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0, LENGTH, THICK);
        quad(t, x1, y0, z1, x0, y0, z1, x0, y1, z1, x1, y1, z1, LENGTH, THICK);
        quad(t, x0, y0, z1, x0, y0, z0, x0, y1, z0, x0, y1, z1, DEPTH, THICK);
        quad(t, x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0, DEPTH, THICK);
        t.draw();
    }

    private static void quad(Tessellator t, double ax, double ay, double az, double bx, double by, double bz, double cx,
        double cy, double cz, double dx, double dy, double dz, double u, double v) {
        t.addVertexWithUV(ax, ay, az, 0.0, v);
        t.addVertexWithUV(bx, by, bz, u, v);
        t.addVertexWithUV(cx, cy, cz, u, 0.0);
        t.addVertexWithUV(dx, dy, dz, 0.0, 0.0);
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return TEXTURE;
    }
}
