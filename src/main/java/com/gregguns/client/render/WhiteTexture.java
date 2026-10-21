package com.gregguns.client.render;

import net.minecraft.client.renderer.texture.DynamicTexture;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * A 1 by 1 white texture for flat-color quads. Shader packs (Angelica) expect every gbuffers draw to sample a
 * texture, so flat color is drawn as vertex color times a white texel instead of {@code glDisable(GL_TEXTURE_2D)}.
 * Client only; created lazily on the render thread.
 */
@SideOnly(Side.CLIENT)
final class WhiteTexture {

    private static DynamicTexture texture;

    private WhiteTexture() {}

    /** Binds the white texture on the active texture unit. */
    static void bind() {
        if (texture == null) {
            texture = new DynamicTexture(1, 1);
            texture.getTextureData()[0] = 0xFFFFFFFF;
            texture.updateDynamicTexture();
        }
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture.getGlTextureId());
    }
}
