package com.gregguns.client.utility;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.gregguns.GregGuns;
import com.gregguns.client.render.GlStateGuard;
import com.gregguns.utility.camp.FlagPostGeometry;

/**
 * Draws the Camp Flag post and its waving flag. It reads every number from {@link FlagPostGeometry}, so replacing that
 * data replaces the model; this class only turns boxes into quads. Client only.
 */
// Spec: docs/techguns-spec/09-machines-utility/misc-tile-entities.md section 2 (Rendering); model notes:
// docs/techguns-spec/09-machines-utility/model-notes/flag_post.md
public final class CampFlagRenderer extends TileEntitySpecialRenderer {

    private static final ResourceLocation TEXTURE = new ResourceLocation(GregGuns.MODID, FlagPostGeometry.TEXTURE);
    private static final float UNIT = 1.0F / 16.0F;

    private final ModelBase base = new ModelBase() {};
    private final ModelRenderer[] post = new ModelRenderer[FlagPostGeometry.POST_BOXES.size()];
    private final ModelRenderer[] panels = new ModelRenderer[FlagPostGeometry.PANELS.size()];

    /** Builds the model parts from the geometry data. */
    public CampFlagRenderer() {
        base.textureWidth = FlagPostGeometry.TEXTURE_SIZE;
        base.textureHeight = FlagPostGeometry.TEXTURE_SIZE;
        for (int i = 0; i < post.length; i++) {
            post[i] = part(FlagPostGeometry.POST_BOXES.get(i));
        }
        for (int i = 0; i < panels.length; i++) {
            panels[i] = part(FlagPostGeometry.PANELS.get(i).box);
        }
    }

    /** One box as a part. The model draws with +Y down, so the box is placed at minus its top edge. */
    private ModelRenderer part(FlagPostGeometry.Box b) {
        ModelRenderer r = new ModelRenderer(base, b.u, b.v);
        r.setTextureSize(FlagPostGeometry.TEXTURE_SIZE, FlagPostGeometry.TEXTURE_SIZE);
        r.addBox(b.x, -(b.y + b.sizeY), b.z, Math.round(b.sizeX), Math.round(b.sizeY), Math.round(b.sizeZ));
        return r;
    }

    @Override
    public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float partial) {
        bindTexture(TEXTURE);
        // Cull face, rescale normal and color change below; the guard restores them and the matrix on every exit.
        try (GlStateGuard g = GlStateGuard.matrixAndAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT)) {
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glTranslatef((float) x + 0.5F, (float) y, (float) z + 0.5F);
            // The parts are built with +Y down; flip so the post stands up.
            GL11.glScalef(1.0F, -1.0F, 1.0F);
            GL11.glEnable(GL12.GL_RESCALE_NORMAL);
            GL11.glDisable(GL11.GL_CULL_FACE);
            for (ModelRenderer r : post) {
                r.render(UNIT);
            }
            // The wave steps once per world tick; no partial-tick smoothing (model notes section 7).
            float time = Minecraft.getMinecraft().theWorld == null ? 0.0F
                : (float) Minecraft.getMinecraft().theWorld.getTotalWorldTime();
            // Panels are chained: each hinge sits where the previous panel's hinge plus its own offset lands.
            for (int i = 0; i < panels.length; i++) {
                FlagPostGeometry.Panel panel = FlagPostGeometry.PANELS.get(i);
                GL11.glTranslatef(panel.hingeX * UNIT, 0.0F, 0.0F);
                GL11.glRotatef(FlagPostGeometry.swingDegrees(panel, time), 0.0F, 1.0F, 0.0F);
                panels[i].render(UNIT);
            }
        }
        GlStateGuard.rebindBlockAtlas();
    }
}
