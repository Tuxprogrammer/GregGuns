package com.gregguns.armor.model;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * The box model of one armor set: a sheet size and a list of {@link ArmorBoxDef}. It is read from
 * {@code assets/gregguns/armor_models/<set>.json}, which {@code tools/asset-gen/armor_models.py} writes together with
 * the texture painted to fit it. Pure (Gson only), so tests and the exporter can use it on a server.
 */
// Spec: docs/gregguns-spec/05-armor/implementation-notes.md ARM-DEV-24
public final class ArmorModelDef {

    private final String name;
    private final int textureWidth;
    private final int textureHeight;
    private final List<ArmorBoxDef> boxes;

    /**
     * @param name          the set id this model dresses
     * @param textureWidth  sheet width in pixels
     * @param textureHeight sheet height in pixels
     * @param boxes         every box
     */
    public ArmorModelDef(String name, int textureWidth, int textureHeight, List<ArmorBoxDef> boxes) {
        this.name = name;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.boxes = Collections.unmodifiableList(new ArrayList<>(boxes));
    }

    /** @return the set id */
    public String name() {
        return name;
    }

    /** @return sheet width in pixels */
    public int textureWidth() {
        return textureWidth;
    }

    /** @return sheet height in pixels */
    public int textureHeight() {
        return textureHeight;
    }

    /** @return every box, in file order */
    public List<ArmorBoxDef> boxes() {
        return boxes;
    }

    /**
     * @param slot a piece
     * @return the boxes that piece draws
     */
    public List<ArmorBoxDef> boxesFor(ArmorBoxDef.Slot slot) {
        List<ArmorBoxDef> out = new ArrayList<>();
        for (ArmorBoxDef b : boxes) {
            if (b.slot() == slot) {
                out.add(b);
            }
        }
        return out;
    }

    /**
     * Parses a model file.
     *
     * @param in the JSON text
     * @return the model
     * @throws IOException when the text is not a valid model file
     */
    public static ArmorModelDef parse(Reader in) throws IOException {
        try {
            JsonObject root = new JsonParser().parse(in)
                .getAsJsonObject();
            List<ArmorBoxDef> boxes = new ArrayList<>();
            for (JsonElement e : root.getAsJsonArray("boxes")) {
                boxes.add(parseBox(e.getAsJsonObject()));
            }
            return new ArmorModelDef(
                root.get("name")
                    .getAsString(),
                root.get("textureWidth")
                    .getAsInt(),
                root.get("textureHeight")
                    .getAsInt(),
                boxes);
        } catch (RuntimeException ex) {
            throw new IOException("bad armor model file: " + ex.getMessage(), ex);
        }
    }

    private static ArmorBoxDef parseBox(JsonObject b) {
        JsonArray uv = b.getAsJsonArray("uv");
        return new ArmorBoxDef(
            b.get("name")
                .getAsString(),
            ArmorBoxDef.Part.fromJson(
                b.get("part")
                    .getAsString()),
            ArmorBoxDef.Slot.fromJson(
                b.get("slot")
                    .getAsString()),
            b.has("material") ? b.get("material")
                .getAsString() : "",
            uv.get(0)
                .getAsInt(),
            uv.get(1)
                .getAsInt(),
            floats(b.getAsJsonArray("offset")),
            ints(b.getAsJsonArray("size")),
            b.has("inflate") ? b.get("inflate")
                .getAsFloat() : 0.0f,
            b.has("mirror") && b.get("mirror")
                .getAsBoolean(),
            b.has("pivot") ? floats(b.getAsJsonArray("pivot")) : null,
            b.has("rotate") ? floats(b.getAsJsonArray("rotate")) : null);
    }

    private static float[] floats(JsonArray a) {
        float[] out = new float[a.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = a.get(i)
                .getAsFloat();
        }
        return out;
    }

    private static int[] ints(JsonArray a) {
        int[] out = new int[a.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = a.get(i)
                .getAsInt();
        }
        return out;
    }
}
