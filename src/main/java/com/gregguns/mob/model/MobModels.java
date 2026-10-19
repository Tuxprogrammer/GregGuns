package com.gregguns.mob.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * The loader for mob model files, with a cache. Pure; the client turns a definition into parts.
 */
// Spec: tools/asset-gen/entity_models.md
public final class MobModels {

    private static final Map<String, MobModelDef> CACHE = new HashMap<>();

    private MobModels() {}

    /**
     * @param name a model name
     * @return the resource path of its file, for example {@code /assets/gregguns/entity_models/demo_biped.json}
     */
    public static String resourcePath(String name) {
        return "/assets/gregguns/entity_models/" + name + ".json";
    }

    /**
     * Loads (once) a model from the classpath.
     *
     * @param name the model name, the file name without {@code .json}
     * @return the model
     * @throws IllegalStateException when the file is missing or invalid; the message says what is wrong. A mob with
     *                               no model cannot be drawn, so this fails loudly at renderer registration
     */
    public static synchronized MobModelDef load(String name) {
        MobModelDef cached = CACHE.get(name);
        if (cached != null) {
            return cached;
        }
        try (InputStream in = MobModels.class.getResourceAsStream(resourcePath(name))) {
            if (in == null) {
                throw new IllegalStateException("missing mob model file " + resourcePath(name));
            }
            MobModelDef def = MobModelDef.parse(name, new InputStreamReader(in, StandardCharsets.UTF_8));
            CACHE.put(name, def);
            return def;
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }
}
