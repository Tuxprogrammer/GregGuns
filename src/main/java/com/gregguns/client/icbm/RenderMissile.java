package com.gregguns.client.icbm;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.obj.WavefrontObject;

import org.lwjgl.opengl.GL11;

import com.gregguns.icbm.missile.EntityMissile;
import com.gregguns.icbm.missile.MissileCasings;
import com.gregguns.icbm.missile.MissileTier;
import com.gregguns.icbm.warhead.WarheadCasing;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Draws the flying missile with the same body and warhead models as the placed stack (D31), so arming shows no jump.
 * The model is turned so its nose points along the heading. Client only.
 */
// Spec: docs/gregguns-icbm-spec/03-missiles/missile-entity.md (Rendering)
@SideOnly(Side.CLIENT)
public final class RenderMissile extends Render {

    @Override
    public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTicks) {
        EntityMissile missile = (EntityMissile) entity;
        MissileTier tier = missile.tier();
        float pitch = missile.prevRotationPitch + (missile.rotationPitch - missile.prevRotationPitch) * partialTicks;
        float heading = missile.prevRotationYaw + (missile.rotationYaw - missile.prevRotationYaw) * partialTicks;
        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);
        // Turn about the middle of the body so the arc reads naturally, then put the base back.
        double mid = tier.heightBlocks() / 2.0;
        GL11.glTranslated(0.0, mid, 0.0);
        GL11.glRotatef(-heading, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(pitch, 1.0F, 0.0F, 0.0F);
        GL11.glTranslated(0.0, -mid, 0.0);
        GL11.glTranslated(-0.5, 0.0, -0.5);
        GL11.glDisable(GL11.GL_CULL_FACE);
        draw(WarheadModels.body(tier), WarheadModels.bodyTextureFile(tier), 0.0);
        WarheadCasing casing = MissileCasings.byNumber(missile.casingNumber());
        draw(WarheadModels.get(casing), WarheadModels.textureFile(casing), tier.heightBlocks());
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glPopMatrix();
    }

    private void draw(WavefrontObject model, ResourceLocation texture, double up) {
        if (model == null) {
            return;
        }
        Minecraft.getMinecraft().renderEngine.bindTexture(texture);
        GL11.glPushMatrix();
        GL11.glTranslated(0.0, up, 0.0);
        model.renderAll();
        GL11.glPopMatrix();
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return WarheadModels.bodyTextureFile(((EntityMissile) entity).tier());
    }
}
