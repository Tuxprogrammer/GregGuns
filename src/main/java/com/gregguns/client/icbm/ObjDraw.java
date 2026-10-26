package com.gregguns.client.icbm;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraftforge.client.model.obj.Face;
import net.minecraftforge.client.model.obj.GroupObject;
import net.minecraftforge.client.model.obj.Vertex;
import net.minecraftforge.client.model.obj.WavefrontObject;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Adds an OBJ model to the tessellator as quads, mapping its texture coordinates into one block-atlas sprite, with the
 * simple directional shading the warhead renderer uses. Shared by the warhead and missile body block renderers.
 * Client only.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/warhead-blocks.md (Model and rendering)
@SideOnly(Side.CLIENT)
final class ObjDraw {

    private ObjDraw() {}

    /**
     * @param model the model
     * @param icon  the atlas sprite holding its texture
     * @param bx    X offset of the model origin
     * @param by    Y offset
     * @param bz    Z offset
     * @return true when something was added
     */
    static boolean draw(WavefrontObject model, IIcon icon, double bx, double by, double bz) {
        if (model == null || icon == null) {
            return false;
        }
        Tessellator t = Tessellator.instance;
        for (GroupObject group : model.groupObjects) {
            for (Face face : group.faces) {
                Vertex n = face.faceNormal != null ? face.faceNormal : face.calculateFaceNormal();
                // Simple directional shading: top bright, bottom dark, sides in between.
                float shade = 0.8F + 0.2F * n.y;
                t.setColorOpaque_F(shade, shade, shade);
                int count = face.vertices.length;
                for (int i = 0; i < 4; i++) {
                    int k = Math.min(i, count - 1);
                    Vertex v = face.vertices[k];
                    double u = face.textureCoordinates != null ? face.textureCoordinates[k].u : 0;
                    double w = face.textureCoordinates != null ? face.textureCoordinates[k].v : 0;
                    t.addVertexWithUV(
                        bx + v.x,
                        by + v.y,
                        bz + v.z,
                        icon.getInterpolatedU(u * 16.0),
                        icon.getInterpolatedV(w * 16.0));
                }
            }
        }
        return true;
    }
}
