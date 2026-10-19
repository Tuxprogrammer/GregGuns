package com.gregguns.client.mob;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.OpenGlHelper;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * A {@link ModelRenderer} with two extras for data-driven mob models: a {@code fullbright} flag that draws the part
 * and its children at the maximum lightmap with vanilla lighting off, and a hand offset added when the item renderer
 * attaches a held item to this part (so a bulky arm can hold a gun clear of its own box).
 *
 * <p>
 * Client only.
 */
// Spec: tools/asset-gen/entity_models.md
@SideOnly(Side.CLIENT)
final class RigRenderer extends ModelRenderer {

    /** Whether this part and its children draw at full light. */
    boolean glow;
    /** Blocks added in the part's frame after {@code postRender}; zero by default. */
    float handX;
    float handY;
    float handZ;

    RigRenderer(ModelBase owner) {
        super(owner);
    }

    /**
     * Sets the extra translation applied when a held item is attached to this part.
     *
     * @param x blocks
     * @param y blocks
     * @param z blocks
     */
    void setHandOffset(float x, float y, float z) {
        this.handX = x;
        this.handY = y;
        this.handZ = z;
    }

    @Override
    public void postRender(float scale) {
        super.postRender(scale);
        if (handX != 0.0F || handY != 0.0F || handZ != 0.0F) {
            GL11.glTranslatef(handX, handY, handZ);
        }
    }

    @Override
    public void render(float scale) {
        if (!glow || isHidden || !showModel) {
            super.render(scale);
            return;
        }
        // Source: vanilla 1.7.10 OpenGlHelper keeps the last lightmap coordinates in lastBrightnessX/Y; spider eyes
        // and enderman eyes restore them the same way.
        float bx = OpenGlHelper.lastBrightnessX;
        float by = OpenGlHelper.lastBrightnessY;
        boolean lit = GL11.glIsEnabled(GL11.GL_LIGHTING);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);
        if (lit) {
            GL11.glDisable(GL11.GL_LIGHTING);
        }
        super.render(scale);
        if (lit) {
            GL11.glEnable(GL11.GL_LIGHTING);
        }
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, bx, by);
    }
}
