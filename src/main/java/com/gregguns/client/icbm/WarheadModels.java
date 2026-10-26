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

    private static final Map<String, WavefrontObject> MODELS = new java.util.HashMap<>();

    /** The nuclear warhead has its own model, on the T3 casing (1x1x2, scaled 0.8). */
    static final String NUCLEAR_MODEL = "warhead_nuclear";
    private static final Map<MissileTier, WavefrontObject> BODIES = new EnumMap<>(MissileTier.class);

    private WarheadModels() {}

    /** @return every warhead model name: one per casing plus the nuclear model */
    static java.util.List<String> allNames() {
        java.util.List<String> out = new java.util.ArrayList<>();
        for (WarheadCasing c : WarheadCasing.values()) {
            out.add(c.blockName());
        }
        out.add(NUCLEAR_MODEL);
        return out;
    }

    /**
     * @param casing    the casing
     * @param explosive the warhead's explosive id, or null for the casing's default look
     * @return the model name to draw
     */
    static String modelName(WarheadCasing casing, String explosive) {
        return casing == WarheadCasing.T3 && "nuclear".equals(explosive) ? NUCLEAR_MODEL : casing.blockName();
    }

    /** @return the casing's default model (kept for the missile renderer) */
    static WavefrontObject get(WarheadCasing casing) {
        return get(casing.blockName());
    }

    /** @return the casing's default texture file (kept for the missile renderer) */
    static ResourceLocation textureFile(WarheadCasing casing) {
        return textureFile(casing.blockName());
    }

    static ResourceLocation modelLocation(String name) {
        return new ResourceLocation("gregguns", MODEL_DIR + name + ".obj");
    }

    static ResourceLocation textureFile(String name) {
        return new ResourceLocation("gregguns", TEXTURE_DIR + name + ".png");
    }

    /** @return the atlas name of the model's texture sprite */
    static String spriteName(String name) {
        return "gregguns:" + name;
    }

    /**
     * @param name a model name from {@link #allNames()}
     * @return the loaded model, or null when it cannot be read (logged once)
     */
    static synchronized WavefrontObject get(String name) {
        if (!MODELS.containsKey(name)) {
            WavefrontObject model = null;
            ResourceLocation loc = modelLocation(name);
            try (InputStream in = Minecraft.getMinecraft()
                .getResourceManager()
                .getResource(loc)
                .getInputStream()) {
                model = new WavefrontObject(loc.toString(), in);
            } catch (Exception e) {
                GregGuns.LOG.error("Could not load the warhead model {}", loc, e);
            }
            MODELS.put(name, model);
        }
        return MODELS.get(name);
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
