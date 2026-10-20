package com.gregguns.client.utility;

import net.minecraft.client.Minecraft;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.gregguns.client.render.GlStateGuard;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Draws the Turret base block from {@link TurretGeometry}'s BASE boxes, in the tier sheet for the installed plate.
 * It is reached from the base's {@code renderTESR}, the hook GT's own tile entity renderer calls on a machine that
 * implements {@code IMTERenderer}, so GT's machine behavior is untouched. Client only.
 */
// Spec: docs/gregguns-spec/09-machines-utility/turret.md
@SideOnly(Side.CLIENT)
public final class TurretBaseRenderer {

    private static final float PIXEL = 1.0F / TurretGeometry.PIXELS_PER_BLOCK;
    private static TurretBaseModel model;

    private TurretBaseRenderer() {}

    /**
     * Draws the base in the block whose minimum corner is at the given offset from the camera.
     *
     * @param x          offset to the block's corner, in blocks
     * @param y          same, y
     * @param z          same, z
     * @param plateArmor the plate's armor value (picks the sheet)
     */
    public static void render(double x, double y, double z, int plateArmor) {
        if (model == null) {
            model = new TurretBaseModel();
        }
        Minecraft.getMinecraft().renderEngine.bindTexture(TurretRenderer.textureFor(plateArmor));
        try (GlStateGuard g = GlStateGuard.matrixAndAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT)) {
            GL11.glEnable(GL12.GL_RESCALE_NORMAL);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glTranslated(x + 0.5, y, z + 0.5);
            GL11.glScalef(1.0F, -1.0F, -1.0F);
            model.render(PIXEL);
        }
        GlStateGuard.rebindBlockAtlas();
    }
}
