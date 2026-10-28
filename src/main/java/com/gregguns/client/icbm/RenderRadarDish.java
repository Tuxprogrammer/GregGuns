package com.gregguns.client.icbm;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.obj.WavefrontObject;

import org.lwjgl.opengl.GL11;

import com.gregguns.GregGuns;
import com.gregguns.icbm.defense.EntityRadarDish;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Draws a radar's dish (D125): the NTM-derived dish model (D47, LGPL) turned about the vertical axis by the angle the
 * entity computes from the game time, scaled by the entity's size. The entity stands on the top plane of the radar's
 * structure, at the middle of its footprint. Client only.
 */
// Spec: docs/gregguns-icbm-spec/05-detection-defense/radar.md (The dish)
@SideOnly(Side.CLIENT)
public final class RenderRadarDish extends Render {

    private static final ResourceLocation MODEL = new ResourceLocation(
        GregGuns.MODID,
        "models/thirdparty/ntm/radar_dish.obj");
    private static final ResourceLocation TEXTURE = new ResourceLocation(
        GregGuns.MODID,
        "textures/models/thirdparty/ntm/radar_dish.png");
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
                GregGuns.LOG.error("Could not load the radar dish model {}", MODEL, e);
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
        EntityRadarDish dish = (EntityRadarDish) entity;
        float s = dish.scale();
        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);
        GL11.glRotatef(-dish.angle(partialTicks), 0.0F, 1.0F, 0.0F);
        GL11.glScalef(s, s, s);
        GL11.glDisable(GL11.GL_CULL_FACE);
        int light = entity.getBrightnessForRender(partialTicks);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light % 65536, light / 65536);
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
