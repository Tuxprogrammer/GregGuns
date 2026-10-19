package com.gregguns.client.mob;

import net.minecraft.entity.EntityLiving;
import net.minecraft.util.ResourceLocation;

import com.gregguns.mob.model.MobModels;

import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * One-call renderer registration for data-driven mobs. Call these from client init (the client proxy), not from
 * common code. A model name is the file name under {@code assets/gregguns/entity_models}, without {@code .json}; a
 * texture name is the file name under {@code assets/gregguns/textures/entity}, without {@code .png}. A missing or
 * invalid model file throws at registration with a message naming what to fix.
 *
 * <p>
 * Client only.
 */
// Spec: tools/asset-gen/entity_models.md
@SideOnly(Side.CLIENT)
public final class MobRenderers {

    private MobRenderers() {}

    /**
     * @param texture a texture name
     * @return its location, {@code gregguns:textures/entity/<texture>.png}
     */
    public static ResourceLocation texture(String texture) {
        return new ResourceLocation("gregguns", "textures/entity/" + texture + ".png");
    }

    /**
     * Registers a humanoid NPC renderer: a vanilla biped renderer over a {@code biped: true} model file.
     *
     * @param entity  the entity class
     * @param model   the model name
     * @param texture the skin location; use {@link #texture(String)} for a mod skin
     * @param scale   uniform scale about the feet, 1.0 for normal size
     * @param shadow  shadow radius in blocks
     * @param gunX    held-gun offset in blocks, in the right arm's frame
     * @param gunY    held-gun offset in blocks
     * @param gunZ    held-gun offset in blocks
     * @return the renderer, already registered
     */
    public static RenderDataBiped biped(Class<? extends EntityLiving> entity, String model, ResourceLocation texture,
        float scale, float shadow, float gunX, float gunY, float gunZ) {
        RenderDataBiped r = new RenderDataBiped(
            new DataBipedModel(MobModels.load(model)),
            texture,
            scale,
            shadow,
            gunX,
            gunY,
            gunZ);
        RenderingRegistry.registerEntityRenderingHandler(entity, r);
        return r;
    }

    /**
     * Same as {@link #biped(Class, String, ResourceLocation, float, float, float, float, float)} with a mod skin named
     * by file.
     *
     * @param entity  the entity class
     * @param model   the model name
     * @param texture the skin name under {@code textures/entity}
     * @param scale   uniform scale about the feet
     * @param shadow  shadow radius in blocks
     * @param gunX    held-gun offset in blocks
     * @param gunY    held-gun offset in blocks
     * @param gunZ    held-gun offset in blocks
     * @return the renderer, already registered
     */
    public static RenderDataBiped biped(Class<? extends EntityLiving> entity, String model, String texture, float scale,
        float shadow, float gunX, float gunY, float gunZ) {
        return biped(entity, model, texture(texture), scale, shadow, gunX, gunY, gunZ);
    }

    /**
     * Registers a renderer for a non-humanoid mob.
     *
     * @param entity  the entity class
     * @param model   the model name
     * @param texture the sheet location
     * @param scale   uniform scale about the feet, 1.0 for normal size
     * @param shadow  shadow radius in blocks
     * @return the renderer, already registered
     */
    public static RenderDataLiving living(Class<? extends EntityLiving> entity, String model, ResourceLocation texture,
        float scale, float shadow) {
        RenderDataLiving r = new RenderDataLiving(new DataMobModel(MobModels.load(model)), texture, scale, shadow);
        RenderingRegistry.registerEntityRenderingHandler(entity, r);
        return r;
    }

    /**
     * Same as {@link #living(Class, String, ResourceLocation, float, float)} with a mod sheet named by file.
     *
     * @param entity  the entity class
     * @param model   the model name
     * @param texture the sheet name under {@code textures/entity}
     * @param scale   uniform scale about the feet
     * @param shadow  shadow radius in blocks
     * @return the renderer, already registered
     */
    public static RenderDataLiving living(Class<? extends EntityLiving> entity, String model, String texture,
        float scale, float shadow) {
        return living(entity, model, texture(texture), scale, shadow);
    }
}
