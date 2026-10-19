package com.gregguns.client.mob;

import net.minecraft.client.renderer.entity.RenderBiped;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.gregguns.item.gun.ItemGun;
import com.gregguns.mob.model.MobAnimSource;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The renderer of a humanoid NPC drawn with a {@link DataBipedModel}. It is a vanilla {@link RenderBiped}, so the four
 * armor layers, the helmet slot and the held item work as for a zombie. On top of that it:
 * <ul>
 * <li>scales the model about the mob's feet (the shadow stays unscaled, as in the spec);</li>
 * <li>sets {@code aimedBow} on the main model and on both armor models, so the file's {@code when: aiming} poses
 * and the armor layers raise the arms together: true when the entity implements {@link MobAnimSource} and says it
 * aims, or else when a gun is in the right hand;</li>
 * <li>adds a per-mob hand offset when the held item is a gun, to move the gun clear of a bulky arm.</li>
 * </ul>
 *
 * <p>
 * Client only. Create it through {@link MobRenderers#biped}.
 */
// Spec: docs/techguns-spec/10-entities-npcs/rendering-and-models.md section 2; tools/asset-gen/entity_models.md
@SideOnly(Side.CLIENT)
public final class RenderDataBiped extends RenderBiped {

    private final DataBipedModel model;
    private final ResourceLocation texture;
    private final float scale;
    private final float yOffset;
    private final float gunX;
    private final float gunY;
    private final float gunZ;

    /**
     * @param model   the biped model
     * @param texture the skin sheet
     * @param scale   uniform scale about the feet, 1.0 for normal size
     * @param shadow  shadow radius in blocks
     * @param gunX    held-gun offset in blocks, in the right arm's frame
     * @param gunY    held-gun offset in blocks
     * @param gunZ    held-gun offset in blocks
     */
    public RenderDataBiped(DataBipedModel model, ResourceLocation texture, float scale, float shadow, float gunX,
        float gunY, float gunZ) {
        this(model, texture, scale, shadow, gunX, gunY, gunZ, 0.0F);
    }

    /**
     * @param model   the biped model
     * @param texture the skin sheet
     * @param scale   uniform scale about the feet, 1.0 for normal size
     * @param shadow  shadow radius in blocks
     * @param gunX    held-gun offset in blocks, in the right arm's frame
     * @param gunY    held-gun offset in blocks
     * @param gunZ    held-gun offset in blocks
     * @param yOffset lift applied after scaling, in blocks: positive moves the model up, negative down; 0 for none
     */
    public RenderDataBiped(DataBipedModel model, ResourceLocation texture, float scale, float shadow, float gunX,
        float gunY, float gunZ, float yOffset) {
        super(model, shadow);
        this.yOffset = yOffset;
        this.model = model;
        this.texture = texture;
        this.scale = scale;
        this.gunX = gunX;
        this.gunY = gunY;
        this.gunZ = gunZ;
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
        if (yOffset != 0.0F) {
            GL11.glTranslatef(0.0F, MobYOffset.scaledTranslation(yOffset, scale), 0.0F);
        }
        boolean aiming = isAiming(living);
        model.aimedBow = aiming;
        field_82423_g.aimedBow = aiming;
        field_82425_h.aimedBow = aiming;
    }

    private static boolean isAiming(EntityLivingBase living) {
        if (living instanceof MobAnimSource) {
            return ((MobAnimSource) living).isAiming();
        }
        return holdsGun(living);
    }

    private static boolean holdsGun(EntityLivingBase living) {
        ItemStack held = living.getHeldItem();
        return held != null && held.getItem() instanceof ItemGun;
    }

    /**
     * Vanilla draws the helmet and the held item; for a gun the hand offset is switched on around that call. The gun's
     * own item renderer draws it in the vanilla third-person hand frame (see the turret renderer for the same frame).
     */
    @Override
    protected void renderEquippedItems(EntityLivingBase living, float partial) {
        if (holdsGun(living)) {
            model.setHandOffset(gunX, gunY, gunZ);
            try {
                super.renderEquippedItems(living, partial);
            } finally {
                model.setHandOffset(0.0F, 0.0F, 0.0F);
            }
        } else {
            super.renderEquippedItems(living, partial);
        }
    }
}
