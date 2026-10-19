package com.gregguns.client.utility;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;

import com.gregguns.utility.charge.ChargeModel;
import com.gregguns.utility.charge.ChargeType;

/**
 * Draws a charge's {@link ChargeModel} boxes with the charge's own texture: each face samples its unwrap rectangle, and
 * the armed state swaps to the charge's armed texture (the detonator plate glows red). Client only.
 */
// Spec: docs/techguns-spec/09-machines-utility/explosive-charges.md (World rendering)
final class ChargeBoxRenderer {

    private static final ResourceLocation TNT = texture("tnt_charge");
    private static final ResourceLocation TNT_ARMED = texture("tnt_charge_armed");
    private static final ResourceLocation EXP = texture("exp_charge");
    private static final ResourceLocation EXP_ARMED = texture("exp_charge_armed");

    private ChargeBoxRenderer() {}

    private static ResourceLocation texture(String name) {
        return new ResourceLocation("gregguns", "textures/models/charges/" + name + ".png");
    }

    /**
     * Draws the boxes of a charge in the cell whose minimum corner is at the current GL origin, binding the texture.
     *
     * @param type        which charge
     * @param orientation the placed face
     * @param armed       true draws the armed texture
     */
    static void draw(ChargeType type, int orientation, boolean armed) {
        boolean advanced = type == ChargeType.ADVANCED;
        Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(advanced ? (armed ? EXP_ARMED : EXP) : (armed ? TNT_ARMED : TNT));
        Tessellator t = Tessellator.instance;
        t.startDrawingQuads();
        for (ChargeModel.Box box : ChargeModel.boxes(type, orientation)) {
            for (int face = 0; face < 6; face++) {
                float[][] q = box.quad(face);
                float[][] uv = box.uv(face);
                float[] n = box.normal(face);
                t.setNormal(n[0], n[1], n[2]);
                for (int i = 0; i < 4; i++) {
                    t.addVertexWithUV(
                        q[i][0] / 16,
                        q[i][1] / 16,
                        q[i][2] / 16,
                        uv[i][0] / ChargeModel.TEX_SIZE,
                        uv[i][1] / ChargeModel.TEX_SIZE);
                }
            }
        }
        t.draw();
    }
}
