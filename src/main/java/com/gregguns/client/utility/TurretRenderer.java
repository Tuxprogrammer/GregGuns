package com.gregguns.client.utility;

import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.MinecraftForgeClient;

import org.lwjgl.opengl.GL11;

import com.gregguns.GregGuns;
import com.gregguns.client.render.GlStateGuard;
import com.gregguns.utility.turret.entity.TurretDisplay;
import com.gregguns.utility.turret.entity.TurretEntity;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Draws the turret entity with {@link TurretModel}, in the sheet for the installed armor plate: 0 none, 5 iron, 10
 * steel, 15 aluminum, 20 carbon. The plate is synced to clients by {@link TurretEntity#displayArmorValue()}.
 * Client only.
 */
// Spec: docs/techguns-spec/09-machines-utility/model-notes/turret_head.md (section 5),
// docs/gregguns-spec/09-machines-utility/turret.md
@SideOnly(Side.CLIENT)
public final class TurretRenderer extends RenderLiving {

    private static final String DIR = "textures/models/machines/turret_sheet";
    private static final ResourceLocation[] TEXTURES = { tex(""), tex("_iron"), tex("_steel"), tex("_aluminum"),
        tex("_carbon") };

    /** How far ahead of the pitch pivot the gun's grip sits, in blocks. A guess, to tune by eye. */
    private static final float HAND_REACH = 0.25F;

    private static ResourceLocation tex(String suffix) {
        return new ResourceLocation(GregGuns.MODID, DIR + suffix + ".png");
    }

    /** Creates the renderer with a small round shadow. */
    public TurretRenderer() {
        super(new TurretModel(), 0.4F);
    }

    /**
     * @param armorValue the plate's armor value
     * @return the sheet for it: one tier per 5 points, clamped to the five tiers
     */
    static ResourceLocation textureFor(int armorValue) {
        return TEXTURES[Math.max(0, Math.min(TEXTURES.length - 1, armorValue / 5))];
    }

    /**
     * Draws the gun in the base's gun slot on the head's pitch part, with the gun's own third-person item renderer
     * (or its icon, when it has none). The frame is vanilla's held-item frame: the mount pivot, the head's pitch, an
     * arm turned to point forward, then the same hand and item transforms RenderBiped applies, so the gun's
     * third-person transform program lines up as it does in a hand.
     */
    // Spec: docs/gregguns-spec/09-machines-utility/turret.md; visual check owed.
    @Override
    protected void renderEquippedItems(EntityLivingBase living, float partial) {
        super.renderEquippedItems(living, partial);
        if (!(living instanceof TurretDisplay)) {
            return;
        }
        ItemStack gun = ((TurretDisplay) living).displayGun();
        if (gun == null) {
            return;
        }
        float pitch = living.prevRotationPitch + (living.rotationPitch - living.prevRotationPitch) * partial;
        boolean custom = MinecraftForgeClient.getItemRenderer(gun, ItemRenderType.EQUIPPED) != null;
        // The gun's own renderer may throw or leave attributes changed; the guard keeps both stacks balanced.
        try (GlStateGuard g = GlStateGuard.matrixAndAttrib(
            GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT)) {
            // Model frame: origin 24 px above the feet, y down. The pivot is PITCH_PIVOT_Y above the feet.
            GL11.glTranslatef(0.0F, (24.0F - TurretGeometry.PITCH_PIVOT_Y) / TurretGeometry.PIXELS_PER_BLOCK, 0.0F);
            GL11.glRotatef(pitch, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(-90.0F, 1.0F, 0.0F, 0.0F);
            GL11.glTranslatef(0.0F, HAND_REACH, 0.0F);
            if (custom) {
                float f = 0.375F;
                GL11.glTranslatef(0.0F, 0.1875F, -0.3125F);
                GL11.glRotatef(20.0F, 1.0F, 0.0F, 0.0F);
                GL11.glRotatef(45.0F, 0.0F, 1.0F, 0.0F);
                GL11.glScalef(-f, -f, f);
            } else {
                float f = 0.375F;
                GL11.glTranslatef(0.25F, 0.1875F, -0.1875F);
                GL11.glScalef(f, f, f);
                GL11.glRotatef(60.0F, 0.0F, 0.0F, 1.0F);
                GL11.glRotatef(-90.0F, 1.0F, 0.0F, 0.0F);
                GL11.glRotatef(20.0F, 0.0F, 0.0F, 1.0F);
            }
            RenderManager.instance.itemRenderer.renderItem(living, gun, 0);
        }
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return textureFor(entity instanceof TurretDisplay ? ((TurretDisplay) entity).displayArmorValue() : 0);
    }
}
