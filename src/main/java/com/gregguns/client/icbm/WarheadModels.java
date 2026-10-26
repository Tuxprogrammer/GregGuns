package com.gregguns.client.icbm;

import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.obj.WavefrontObject;

import com.gregguns.GregGuns;
import com.gregguns.icbm.missile.MissileTier;
import com.gregguns.icbm.warhead.WarheadCasing;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Loads the warhead OBJ models with Forge's loader and the paths of their textures. The models come from the guides
 * track (HBM's Nuclear Tech Mod, LGPL-3.0, modified); see CREDITS.md. Client only.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/warhead-blocks.md (Model and rendering)
@SideOnly(Side.CLIENT)
final class WarheadModels {

    private static final String MODEL_DIR = "models/thirdparty/ntm/";
    private static final String TEXTURE_DIR = "textures/models/thirdparty/ntm/";

    private static final Map<WarheadCasing, WavefrontObject> MODELS = new EnumMap<>(WarheadCasing.class);
    private static final Map<MissileTier, WavefrontObject> BODIES = new EnumMap<>(MissileTier.class);

    private WarheadModels() {}

    static ResourceLocation modelLocation(WarheadCasing casing) {
        return new ResourceLocation("gregguns", MODEL_DIR + casing.blockName() + ".obj");
    }

    static ResourceLocation textureFile(WarheadCasing casing) {
        return new ResourceLocation("gregguns", TEXTURE_DIR + casing.blockName() + ".png");
    }

    /** @return the atlas name of the casing's texture sprite */
    static String spriteName(WarheadCasing casing) {
        return "gregguns:" + casing.blockName();
    }

    /**
     * @param casing the casing
     * @return the loaded model, or null when it cannot be read (logged once)
     */
    static synchronized WavefrontObject get(WarheadCasing casing) {
        if (!MODELS.containsKey(casing)) {
            WavefrontObject model = null;
            ResourceLocation loc = modelLocation(casing);
            try (InputStream in = Minecraft.getMinecraft()
                .getResourceManager()
                .getResource(loc)
                .getInputStream()) {
                model = new WavefrontObject(loc.toString(), in);
            } catch (Exception e) {
                GregGuns.LOG.error("Could not load the warhead model {}", loc, e);
            }
            MODELS.put(casing, model);
        }
        return MODELS.get(casing);
    }

    /** Forgets loaded models, after a resource reload. */
    static synchronized void clear() {
        MODELS.clear();
        BODIES.clear();
    }

    /** @return the model file of a missile body */
    static ResourceLocation bodyModelLocation(MissileTier tier) {
        return new ResourceLocation("gregguns", MODEL_DIR + tier.blockName() + ".obj");
    }

    /** @return the texture file of a missile body */
    static ResourceLocation bodyTextureFile(MissileTier tier) {
        return new ResourceLocation("gregguns", TEXTURE_DIR + tier.blockName() + ".png");
    }

    /** @return the atlas name of the body's texture sprite */
    static String bodySpriteName(MissileTier tier) {
        return "gregguns:" + tier.blockName();
    }

    /**
     * @param tier the tier
     * @return the loaded body model, or null when it cannot be read (logged once)
     */
    static synchronized WavefrontObject body(MissileTier tier) {
        if (!BODIES.containsKey(tier)) {
            WavefrontObject model = null;
            ResourceLocation loc = bodyModelLocation(tier);
            try (InputStream in = Minecraft.getMinecraft()
                .getResourceManager()
                .getResource(loc)
                .getInputStream()) {
                model = new WavefrontObject(loc.toString(), in);
            } catch (Exception e) {
                GregGuns.LOG.error("Could not load the missile body model {}", loc, e);
            }
            BODIES.put(tier, model);
        }
        return BODIES.get(tier);
    }
}
