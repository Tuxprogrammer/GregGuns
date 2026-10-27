package com.gregguns.client.icbm;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.obj.WavefrontObject;

import org.lwjgl.opengl.GL11;

import com.gregguns.GregGuns;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Draws the SAM in flight: the NTM-derived SAM model (D47, LGPL) turned so its nose points along the heading. The flame
 * and smoke trail comes from the entity. Client only.
 */
// Spec: docs/gregguns-icbm-spec/05-detection-defense/air-defense-battery.md (SAM)
@SideOnly(Side.CLIENT)
public final class RenderSam extends Render {

    private static final ResourceLocation MODEL = new ResourceLocation(
        GregGuns.MODID,
        "models/thirdparty/ntm/sam_missile.obj");
    private static final ResourceLocation TEXTURE = new ResourceLocation(
        GregGuns.MODID,
        "textures/models/thirdparty/ntm/sam_missile.png");
    private static WavefrontObject model;
    private static boolean failed;

    private static WavefrontObject model() {
        if (model == null && !failed) {
            try (java.io.InputStream in = Minecraft.getMinecraft()
                .getResourceManager()
                .getResource(MODEL)
                .getInputStream()) {
                model = new WavefrontObject(MODEL.toString(), in);
            } catch (Exception e) {
                failed = true;
                GregGuns.LOG.error("Could not load the SAM model {}", MODEL, e);
            }
        }
        return model;
    }

    @Override
    public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTicks) {
        WavefrontObject m = model();
        if (m == null) {
            return;
        }
        // The heading is the last step; the model's nose is up, so 0 degrees of pitch is straight up (as the missile).
        double dx = entity.posX - entity.prevPosX;
        double dy = entity.posY - entity.prevPosY;
        double dz = entity.posZ - entity.prevPosZ;
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        float heading = len > 1e-6 ? (float) Math.toDegrees(Math.atan2(-dx, dz)) : 0.0F;
        float pitch = len > 1e-6 ? (float) Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, dy / len)))) : 0.0F;
        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);
        GL11.glRotatef(-heading, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(pitch, 1.0F, 0.0F, 0.0F);
        GL11.glTranslated(0.0, -0.5, 0.0);
        GL11.glDisable(GL11.GL_CULL_FACE);
        Minecraft.getMinecraft().renderEngine.bindTexture(TEXTURE);
        m.renderAll();
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glPopMatrix();
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return TEXTURE;
    }
}
