package com.gregguns.client.utility;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.gregguns.utility.charge.ChargeModel;
import com.gregguns.utility.charge.ChargeType;

/**
 * Draws a charge's {@link ChargeModel} boxes with one flat texture. PLACEHOLDER rendering: every face shows the whole
 * texture, and the armed state is a red tint. A clean-room model replaces this, not the callers. Client only.
 */
// Spec: docs/techguns-spec/09-machines-utility/explosive-charges.md (World rendering)
final class ChargeBoxRenderer {

    /** The closest existing texture (there is no charge texture yet); listed as an asset gap. */
    static final ResourceLocation TEXTURE = new ResourceLocation(
        "gregguns",
        "textures/blocks/casings/works_casing.png");

    private ChargeBoxRenderer() {}

    /**
     * Draws the boxes of a charge in the cell whose minimum corner is at the current GL origin.
     *
     * @param type        which charge
     * @param orientation the placed face
     * @param armed       true tints the boxes red
     */
    static void draw(ChargeType type, int orientation, boolean armed) {
        GL11.glColor4f(1.0F, armed ? 0.35F : 1.0F, armed ? 0.35F : 1.0F, 1.0F);
        Tessellator t = Tessellator.instance;
        for (ChargeModel.Box box : ChargeModel.boxes(type, orientation)) {
            float[] b = box.bounds;
            drawBox(t, b[0] / 16, b[1] / 16, b[2] / 16, b[3] / 16, b[4] / 16, b[5] / 16);
        }
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void drawBox(Tessellator t, float x0, float y0, float z0, float x1, float y1, float z1) {
        t.startDrawingQuads();
        t.setNormal(0, -1, 0);
        quad(t, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
        t.setNormal(0, 1, 0);
        quad(t, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0);
        t.setNormal(0, 0, -1);
        quad(t, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0);
        t.setNormal(0, 0, 1);
        quad(t, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
        t.setNormal(-1, 0, 0);
        quad(t, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
        t.setNormal(1, 0, 0);
        quad(t, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1);
        t.draw();
    }

    private static void quad(Tessellator t, float ax, float ay, float az, float bx, float by, float bz, float cx,
        float cy, float cz, float dx, float dy, float dz) {
        t.addVertexWithUV(ax, ay, az, 0, 1);
        t.addVertexWithUV(bx, by, bz, 1, 1);
        t.addVertexWithUV(cx, cy, cz, 1, 0);
        t.addVertexWithUV(dx, dy, dz, 0, 0);
    }
}
