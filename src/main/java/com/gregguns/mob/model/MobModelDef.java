package com.gregguns.mob.model;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

/**
 * A data-driven mob model: the sheet size, a tree of parts with boxes, and a list of animations. It is read from
 * {@code assets/gregguns/entity_models/<name>.json}, which {@code tools/asset-gen/entity_models.py} writes together
 * with the texture painted to fit it. Pure (Gson only), so tests and tools use it on a server.
 *
 * <p>
 * Everything is validated up front; a bad file fails with a message that names the part, box or animation and the
 * fix. The format is described in {@code tools/asset-gen/entity_models.md}.
 */
// Spec: tools/asset-gen/entity_models.md; docs/techguns-spec/10-entities-npcs/rendering-and-models.md
public final class MobModelDef {

    /** The six parts a biped model must have, in {@code ModelBiped} order. */
    public static final List<String> BIPED_PARTS = Collections
        .unmodifiableList(Arrays.asList("head", "body", "rightArm", "leftArm", "rightLeg", "leftLeg"));

    private static final String NAME_PATTERN = "[A-Za-z0-9_]+";

    private final String name;
    private final int textureWidth;
    private final int textureHeight;
    private final boolean biped;
    private final List<MobPartDef> parts;
    private final List<MobAnimDef> animations;
    private final Map<String, Integer> index = new LinkedHashMap<>();

    /**
     * @param name          the model name (the file name without {@code .json})
     * @param textureWidth  sheet width in pixels
     * @param textureHeight sheet height in pixels
     * @param biped         whether the model maps onto {@code ModelBiped}
     * @param parts         every part, in file order
     * @param animations    every animation, in file order
     * @throws IllegalArgumentException with a descriptive message when the model is not valid
     */
    public MobModelDef(String name, int textureWidth, int textureHeight, boolean biped, List<MobPartDef> parts,
        List<MobAnimDef> animations) {
        this.name = name;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.biped = biped;
        this.parts = Collections.unmodifiableList(new ArrayList<>(parts));
        this.animations = Collections.unmodifiableList(new ArrayList<>(animations));
        for (int i = 0; i < this.parts.size(); i++) {
            index.put(
                this.parts.get(i)
                    .name(),
                i);
        }
        validate();
    }

    /** @return the model name */
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

    /** @return true when the model maps onto {@code ModelBiped} */
    public boolean biped() {
        return biped;
    }

    /** @return every part, in file order */
    public List<MobPartDef> parts() {
        return parts;
    }

    /** @return every animation, in file order */
    public List<MobAnimDef> animations() {
        return animations;
    }

    /**
     * @param partName a part name
     * @return its index in {@link #parts()}, or -1 when there is no such part
     */
    public int indexOf(String partName) {
        Integer i = index.get(partName);
        return i == null ? -1 : i;
    }

    // ------------------------------------------------------------------------------------------ validation

    private void validate() {
        if (!name.matches(NAME_PATTERN)) {
            throw bad("model name '" + name + "' must be letters, digits and underscores");
        }
        if (textureWidth <= 0 || textureHeight <= 0) {
            throw bad("texture size must be positive, got " + textureWidth + "x" + textureHeight);
        }
        if (parts.isEmpty()) {
            throw bad("a model needs at least one part");
        }
        Set<String> seen = new HashSet<>();
        for (MobPartDef p : parts) {
            if (!p.name()
                .matches(NAME_PATTERN)) {
                throw bad("part name '" + p.name() + "' must be letters, digits and underscores");
            }
            if (!seen.add(p.name())) {
                throw bad("duplicate part name '" + p.name() + "'");
            }
        }
        for (MobPartDef p : parts) {
            if (p.parent() != null) {
                if (!index.containsKey(p.parent())) {
                    throw bad("part '" + p.name() + "' has unknown parent '" + p.parent() + "'");
                }
                if (p.parent()
                    .equals(p.name())) {
                    throw bad("part '" + p.name() + "' is its own parent");
                }
            }
            checkCycle(p);
            for (MobBoxDef b : p.boxes()) {
                validateBox(p, b);
            }
        }
        for (int i = 0; i < animations.size(); i++) {
            validateAnim(i, animations.get(i));
        }
        if (biped) {
            validateBiped();
        }
    }

    private void checkCycle(MobPartDef start) {
        MobPartDef p = start;
        for (int hops = 0; hops <= parts.size(); hops++) {
            if (p.parent() == null) {
                return;
            }
            p = parts.get(index.get(p.parent()));
            if (p == start) {
                throw bad("parent loop through part '" + start.name() + "'");
            }
        }
        throw bad("parent loop above part '" + start.name() + "'");
    }

    private void validateBox(MobPartDef p, MobBoxDef b) {
        String where = "part '" + p.name() + "' box '" + b.name() + "'";
        int[] s = b.size();
        if (s[0] < 0 || s[1] < 0 || s[2] < 0) {
            throw bad(where + ": size must not be negative, got " + Arrays.toString(s));
        }
        if (s[0] == 0 && s[1] == 0 && s[2] == 0) {
            throw bad(where + ": size is all zero");
        }
        if (b.u() < 0 || b.v() < 0) {
            throw bad(where + ": uv must not be negative");
        }
        int footW = 2 * (s[2] + s[0]);
        int footH = s[2] + s[1];
        if (b.u() + footW > textureWidth || b.v() + footH > textureHeight) {
            throw bad(
                where + ": its unwrap ("
                    + footW
                    + "x"
                    + footH
                    + " at "
                    + b.u()
                    + ","
                    + b.v()
                    + ") runs off the "
                    + textureWidth
                    + "x"
                    + textureHeight
                    + " sheet");
        }
        for (float v : b.offset()) {
            if (Float.isNaN(v) || Float.isInfinite(v)) {
                throw bad(where + ": offset must be finite");
            }
        }
    }

    private void validateAnim(int i, MobAnimDef a) {
        String where = "animation #" + i
            + " ("
            + a.type()
                .json()
            + ")";
        if (indexOf(a.part()) < 0) {
            throw bad(where + ": unknown part '" + a.part() + "'");
        }
        switch (a.type()) {
            case SWING:
            case IDLE:
            case SPIN:
                if (a.axis() < 0 || a.axis() > 2) {
                    throw bad(where + ": axis must be x, y or z");
                }
                break;
            case POSE:
                if (a.when() == null) {
                    throw bad(where + ": a pose needs 'when'");
                }
                float[] ang = a.angles();
                boolean any = a.followYaw() != 0.0F || a.followPitch() != 0.0F;
                for (float v : ang) {
                    any |= !Float.isNaN(v);
                }
                if (!any) {
                    throw bad(where + ": a pose needs 'angles' or a follow factor");
                }
                if (a.windowTo() <= a.windowFrom()) {
                    throw bad(where + ": window must run from a smaller to a larger progress");
                }
                break;
            default:
                break;
        }
    }

    private void validateBiped() {
        for (String need : BIPED_PARTS) {
            if (indexOf(need) < 0) {
                throw bad("a biped model needs a part named '" + need + "' (all of " + BIPED_PARTS + ")");
            }
        }
        for (MobPartDef p : parts) {
            if (p.parent() == null && !BIPED_PARTS.contains(p.name())) {
                throw bad(
                    "biped model: root part '" + p.name()
                        + "' is not one of the six biped parts; give it a parent so it is drawn with one");
            }
            if (BIPED_PARTS.contains(p.name()) && p.parent() != null) {
                throw bad("biped model: part '" + p.name() + "' must be a root, not a child of '" + p.parent() + "'");
            }
        }
    }

    private IllegalArgumentException bad(String msg) {
        return new IllegalArgumentException("mob model '" + name + "': " + msg);
    }

    // ------------------------------------------------------------------------------------------ parsing

    private static final Set<String> MODEL_KEYS = keys(
        "name",
        "textureWidth",
        "textureHeight",
        "biped",
        "parts",
        "animations");
    private static final Set<String> PART_KEYS = keys("name", "parent", "pivot", "rotate", "fullbright", "boxes");
    private static final Set<String> BOX_KEYS = keys("name", "uv", "offset", "size", "inflate", "mirror", "material");
    private static final Map<String, Set<String>> ANIM_KEYS = new HashMap<>();

    static {
        ANIM_KEYS.put("look", keys("type", "part", "yaw", "pitch", "maxYaw", "maxPitch"));
        ANIM_KEYS.put("swing", keys("type", "part", "axis", "amplitude", "speed", "phase"));
        ANIM_KEYS.put("idle", keys("type", "part", "axis", "amplitude", "speed", "phase", "offset"));
        ANIM_KEYS.put("spin", keys("type", "part", "axis", "rate"));
        ANIM_KEYS
            .put("pose", keys("type", "part", "when", "mode", "angles", "followYaw", "followPitch", "blend", "window"));
    }

    private static Set<String> keys(String... k) {
        return new HashSet<>(Arrays.asList(k));
    }

    /**
     * Parses and validates a model file.
     *
     * @param modelName the model name (the file name without the extension)
     * @param in        the JSON text
     * @return the model
     * @throws IOException when the text is not a valid model file; the message says what to fix
     */
    public static MobModelDef parse(String modelName, Reader in) throws IOException {
        try {
            JsonObject root = new JsonParser().parse(in)
                .getAsJsonObject();
            noUnknownKeys(root, MODEL_KEYS, "model");
            String declared = str(root, "name", "model");
            if (!declared.equals(modelName)) {
                throw new IllegalArgumentException(
                    "the file is named '" + modelName + "' but declares name '" + declared + "'");
            }
            List<MobPartDef> parts = new ArrayList<>();
            for (JsonElement e : array(root, "parts", "model")) {
                parts.add(parsePart(obj(e, "part")));
            }
            List<MobAnimDef> anims = new ArrayList<>();
            if (root.has("animations")) {
                for (JsonElement e : array(root, "animations", "model")) {
                    anims.add(parseAnim(obj(e, "animation")));
                }
            }
            return new MobModelDef(
                declared,
                intOf(root, "textureWidth", "model"),
                intOf(root, "textureHeight", "model"),
                root.has("biped") && bool(root, "biped", "model"),
                parts,
                anims);
        } catch (RuntimeException ex) {
            // IllegalArgumentException, IllegalStateException and ClassCastException from Gson all end here.
            String msg = ex.getMessage();
            if (msg == null || msg.isEmpty()) {
                msg = ex.getClass()
                    .getSimpleName();
            }
            throw new IOException("bad mob model file '" + modelName + "': " + msg, ex);
        }
    }

    private static MobPartDef parsePart(JsonObject o) {
        noUnknownKeys(o, PART_KEYS, "part");
        String name = str(o, "name", "part");
        String where = "part '" + name + "'";
        List<MobBoxDef> boxes = new ArrayList<>();
        if (o.has("boxes")) {
            for (JsonElement e : array(o, "boxes", where)) {
                boxes.add(parseBox(obj(e, "box in " + where), where));
            }
        }
        return new MobPartDef(
            name,
            o.has("parent") && !o.get("parent")
                .isJsonNull() ? str(o, "parent", where) : null,
            o.has("pivot") ? floats(o, "pivot", 3, where) : new float[3],
            o.has("rotate") ? floats(o, "rotate", 3, where) : new float[3],
            o.has("fullbright") && bool(o, "fullbright", where),
            boxes);
    }

    private static MobBoxDef parseBox(JsonObject o, String partWhere) {
        noUnknownKeys(o, BOX_KEYS, "box in " + partWhere);
        String name = str(o, "name", "box in " + partWhere);
        String where = partWhere + " box '" + name + "'";
        float[] uv = floats(o, "uv", 2, where);
        float[] size = floats(o, "size", 3, where);
        for (int i = 0; i < 2; i++) {
            if (uv[i] != Math.rint(uv[i])) {
                throw new IllegalArgumentException(where + ": uv must be whole numbers");
            }
        }
        for (int i = 0; i < 3; i++) {
            if (size[i] != Math.rint(size[i])) {
                throw new IllegalArgumentException(where + ": size must be whole numbers, got " + size[i]);
            }
        }
        return new MobBoxDef(
            name,
            (int) uv[0],
            (int) uv[1],
            floats(o, "offset", 3, where),
            new int[] { (int) size[0], (int) size[1], (int) size[2] },
            o.has("inflate") ? num(o, "inflate", where) : 0.0F,
            o.has("mirror") && bool(o, "mirror", where),
            o.has("material") ? str(o, "material", where) : "");
    }

    private static MobAnimDef parseAnim(JsonObject o) {
        String typeName = str(o, "type", "animation");
        MobAnimDef.Type type = MobAnimDef.Type.fromJson(typeName);
        String where = "animation '" + typeName + "'";
        noUnknownKeys(o, ANIM_KEYS.get(typeName), where);
        String part = str(o, "part", where);
        where = where + " on '" + part + "'";
        MobAnimDef.Builder b = new MobAnimDef.Builder(type, part);
        switch (type) {
            case LOOK:
                b.lookFactors(opt(o, "yaw", 1.0F, where), opt(o, "pitch", 1.0F, where));
                b.lookLimits(opt(o, "maxYaw", 360.0F, where), opt(o, "maxPitch", 360.0F, where));
                break;
            case SWING:
                b.axis(axis(o, where))
                    .amplitude(req(o, "amplitude", where))
                    .speed(opt(o, "speed", MobAnimDef.DEFAULT_SWING_SPEED, where))
                    .phase(opt(o, "phase", 0.0F, where));
                break;
            case IDLE:
                b.axis(axis(o, where))
                    .amplitude(req(o, "amplitude", where))
                    .speed(opt(o, "speed", 0.1F, where))
                    .phase(opt(o, "phase", 0.0F, where))
                    .offset(opt(o, "offset", 0.0F, where));
                break;
            case SPIN:
                b.axis(axis(o, where))
                    .rate(req(o, "rate", where));
                break;
            default:
                b.when(MobAnimDef.When.fromJson(str(o, "when", where)));
                String mode = o.has("mode") ? str(o, "mode", where) : "add";
                if (!"add".equals(mode) && !"set".equals(mode)) {
                    throw new IllegalArgumentException(where + ": mode must be 'add' or 'set', got '" + mode + "'");
                }
                b.set("set".equals(mode));
                if (o.has("angles")) {
                    JsonObject a = obj(o.get("angles"), "angles of " + where);
                    noUnknownKeys(a, keys("x", "y", "z"), "angles of " + where);
                    b.angles(
                        opt(a, "x", Float.NaN, where),
                        opt(a, "y", Float.NaN, where),
                        opt(a, "z", Float.NaN, where));
                }
                b.follow(opt(o, "followYaw", 0.0F, where), opt(o, "followPitch", 0.0F, where));
                if (o.has("blend")) {
                    b.blend(MobAnimDef.Blend.fromJson(str(o, "blend", where)));
                }
                if (o.has("window")) {
                    float[] w = floats(o, "window", 2, where);
                    b.window(w[0], w[1]);
                }
                break;
        }
        return b.build();
    }

    private static int axis(JsonObject o, String where) {
        String a = str(o, "axis", where);
        switch (a) {
            case "x":
                return 0;
            case "y":
                return 1;
            case "z":
                return 2;
            default:
                throw new IllegalArgumentException(where + ": axis must be 'x', 'y' or 'z', got '" + a + "'");
        }
    }

    private static void noUnknownKeys(JsonObject o, Set<String> allowed, String where) {
        for (Map.Entry<String, JsonElement> e : o.entrySet()) {
            if (!allowed.contains(e.getKey())) {
                throw new IllegalArgumentException(
                    where + ": unknown field '" + e.getKey() + "' (allowed: " + new java.util.TreeSet<>(allowed) + ")");
            }
        }
    }

    private static JsonObject obj(JsonElement e, String where) {
        if (e == null || !e.isJsonObject()) {
            throw new IllegalArgumentException(where + " must be an object");
        }
        return e.getAsJsonObject();
    }

    private static JsonArray array(JsonObject o, String key, String where) {
        JsonElement e = o.get(key);
        if (e == null || !e.isJsonArray()) {
            throw new IllegalArgumentException(where + ": '" + key + "' must be a list");
        }
        return e.getAsJsonArray();
    }

    private static String str(JsonObject o, String key, String where) {
        JsonElement e = o.get(key);
        if (e == null || !e.isJsonPrimitive()
            || !e.getAsJsonPrimitive()
                .isString()) {
            throw new IllegalArgumentException(where + ": '" + key + "' is missing or not a string");
        }
        return e.getAsString();
    }

    private static boolean bool(JsonObject o, String key, String where) {
        JsonElement e = o.get(key);
        if (e == null || !e.isJsonPrimitive()
            || !e.getAsJsonPrimitive()
                .isBoolean()) {
            throw new IllegalArgumentException(where + ": '" + key + "' must be true or false");
        }
        return e.getAsBoolean();
    }

    private static float num(JsonObject o, String key, String where) {
        return numOf(o.get(key), key, where);
    }

    private static float numOf(JsonElement e, String key, String where) {
        if (e == null || !e.isJsonPrimitive()) {
            throw new IllegalArgumentException(where + ": '" + key + "' must be a number");
        }
        JsonPrimitive p = e.getAsJsonPrimitive();
        if (!p.isNumber()) {
            throw new IllegalArgumentException(where + ": '" + key + "' must be a number");
        }
        float v = p.getAsFloat();
        if (Float.isNaN(v) || Float.isInfinite(v)) {
            throw new IllegalArgumentException(where + ": '" + key + "' must be finite");
        }
        return v;
    }

    private static float req(JsonObject o, String key, String where) {
        if (!o.has(key)) {
            throw new IllegalArgumentException(where + ": '" + key + "' is required");
        }
        return num(o, key, where);
    }

    private static float opt(JsonObject o, String key, float dflt, String where) {
        return o.has(key) ? num(o, key, where) : dflt;
    }

    private static int intOf(JsonObject o, String key, String where) {
        float v = req(o, key, where);
        if (v != Math.rint(v)) {
            throw new IllegalArgumentException(where + ": '" + key + "' must be a whole number");
        }
        return (int) v;
    }

    private static float[] floats(JsonObject o, String key, int n, String where) {
        JsonArray a = array(o, key, where);
        if (a.size() != n) {
            throw new IllegalArgumentException(where + ": '" + key + "' needs " + n + " numbers, got " + a.size());
        }
        float[] out = new float[n];
        for (int i = 0; i < n; i++) {
            out[i] = numOf(a.get(i), key, where);
        }
        return out;
    }
}
