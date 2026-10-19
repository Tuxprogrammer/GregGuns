package com.gregguns.armor.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.gregguns.GregGuns;

/**
 * The sets that have a custom worn model, and the loader for their box files. A set that is not listed uses the
 * vanilla biped armor layers (a 64 x 32 layout, or that layout at an integer multiple, which UV-maps correctly because
 * UVs are fractions of the sheet). Pure; the client turns a definition into a {@code ModelBiped} in
 * {@code com.gregguns.client.armor}.
 */
// Spec: docs/gregguns-spec/05-armor/implementation-notes.md ARM-DEV-13
public final class ArmorModels {

    /** Set ids with a box model, in the order of the spec. */
    public static final List<String> CUSTOM_SETS = Collections
        .unmodifiableList(Arrays.asList("t3_exo", "t3_power", "steam", "t2_riot", "t2_beret"));

    private static final Map<String, ArmorModelDef> CACHE = new HashMap<>();

    private ArmorModels() {}

    /**
     * @param setId a set id
     * @return the resource path of its box file, for example {@code /assets/gregguns/armor_models/steam.json}
     */
    public static String resourcePath(String setId) {
        return "/assets/gregguns/armor_models/" + setId + ".json";
    }

    /**
     * @param setId a set id
     * @return whether the set has a custom model
     */
    public static boolean hasModel(String setId) {
        return CUSTOM_SETS.contains(setId);
    }

    /**
     * Loads (once) a set's model from the classpath.
     *
     * @param setId a set id from {@link #CUSTOM_SETS}
     * @return the model, or null when the set has none or its file is broken (logged)
     */
    public static synchronized @Nullable ArmorModelDef get(String setId) {
        if (!hasModel(setId)) {
            return null;
        }
        ArmorModelDef cached = CACHE.get(setId);
        if (cached != null) {
            return cached;
        }
        try (InputStream in = ArmorModels.class.getResourceAsStream(resourcePath(setId))) {
            if (in == null) {
                GregGuns.LOG.error("Missing armor model file {}", resourcePath(setId));
                return null;
            }
            ArmorModelDef def = ArmorModelDef.parse(new InputStreamReader(in, StandardCharsets.UTF_8));
            CACHE.put(setId, def);
            return def;
        } catch (IOException e) {
            GregGuns.LOG.error("Cannot read armor model {}", setId, e);
            return null;
        }
    }
}
