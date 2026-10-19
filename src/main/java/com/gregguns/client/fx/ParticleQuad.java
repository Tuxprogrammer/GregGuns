package com.gregguns.client.fx;

import net.minecraft.client.renderer.Tessellator;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Writes one particle quad to a {@link Tessellator}: brightness, one color, then four vertices each with its UV set
 * first. It never calls {@code setNormal} or {@code disableColor}, so Angelica's particle capture accepts it and
 * instances the draw (GGS-04-06).
 *
 * <p>
 * Side: client only.
 */
// Spec: docs/gregguns-spec/04-projectiles-fx/particle-rendering.md (GGS-04-06)
// Source: Angelica 2.2.28 ParticleCaptureTessellator, which spills on a color change after a vertex, on a vertex with
// no UV set, a fifth vertex, setNormal and disableColor:
// https://github.com/GTNewHorizons/Angelica/blob/2.2.28/src/main/java/com/gtnewhorizons/angelica/rendering/particles/ParticleCaptureTessellator.java#L137-L152
@SideOnly(Side.CLIENT)
final class ParticleQuad {

    private ParticleQuad() {}

    /**
     * @param t          the tessellator the effect renderer passed to {@code renderParticle}
     * @param corners    twelve doubles, four camera-relative vertices (see {@code QuadMath.corners})
     * @param uv         eight floats, one UV pair per vertex (see {@code QuadMath.uv})
     * @param rgb        packed 0xRRGGBB
     * @param alpha      opacity, 0 to 1
     * @param brightness lightmap value; {@code 0xF000F0} is full-bright
     */
    static void emit(Tessellator t, double[] corners, float[] uv, int rgb, float alpha, int brightness) {
        t.setBrightness(brightness);
        t.setColorRGBA(
            (rgb >> 16) & 0xFF,
            (rgb >> 8) & 0xFF,
            rgb & 0xFF,
            Math.max(0, Math.min(255, Math.round(alpha * 255F))));
        for (int i = 0; i < 4; i++) {
            t.addVertexWithUV(corners[i * 3], corners[i * 3 + 1], corners[i * 3 + 2], uv[i * 2], uv[i * 2 + 1]);
        }
    }
}
