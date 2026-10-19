package com.gregguns.client.mob;

import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.gregguns.mob.model.MobAnimSource;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The renderer of a non-humanoid mob (a bug, a drone, a beast) drawn with a {@link DataMobModel}: a plain living
 * renderer with a fixed texture, a uniform scale about the feet and a shadow. Vanilla still does the hurt flash, the
 * death topple and the name tag.
 *
 * <p>
 * Client only. Create it through {@link MobRenderers#living}.
 */
// Spec: docs/techguns-spec/10-entities-npcs/rendering-and-models.md section 4; tools/asset-gen/entity_models.md
@SideOnly(Side.CLIENT)
public final class RenderDataLiving extends RenderLiving {

    private final DataMobModel model;
    private final ResourceLocation texture;
    private final float scale;

    /**
     * @param model   the model
     * @param texture the sheet
     * @param scale   uniform scale about the feet, 1.0 for normal size
     * @param shadow  shadow radius in blocks
     */
    public RenderDataLiving(DataMobModel model, ResourceLocation texture, float scale, float shadow) {
        super(model, shadow);
        this.model = model;
        this.texture = texture;
        this.scale = scale;
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return texture;
    }

    @Override
    protected void preRenderCallback(EntityLivingBase living, float partial) {
        if (scale != 1.0F) {
            GL11.glScalef(scale, scale, scale);
        }
        model.aiming = living instanceof MobAnimSource && ((MobAnimSource) living).isAiming();
        model.holding = living.getHeldItem() != null;
    }
}
