package com.gregguns.core.selfcheck;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

/**
 * Self-check over {@code sounds.json}: every sound it references has an {@code .ogg} on the classpath.
 *
 * <p>
 * Format (Minecraft 1.7.10): an object of event name to {@code {"category": ..., "sounds": [...]}}, where each sound is
 * a path string or an object with a {@code name}. A path without a domain belongs to the mod's own domain. Spec:
 * CONTRIBUTING.md (Testing; Assets: sounds). Both sides, no Minecraft types.
 */
public final class SoundChecks {

    /** Classpath location of the sound event registry. */
    public static final String SOUNDS_JSON_PATH = "assets/gregguns/sounds.json";

    private static final String DOMAIN = "gregguns";

    private SoundChecks() {}

    /**
     * Checks the sound registry.
     *
     * @param resources the mod's classpath resources
     * @return one message per violation. An absent or empty ({@code {}}) {@code sounds.json} defines no events, so it
     *         has none.
     */
    public static List<String> check(ResourceSource resources) {
        List<String> violations = new ArrayList<>();
        if (!resources.exists(SOUNDS_JSON_PATH)) {
            return violations;
        }
        JsonObject root;
        try {
            JsonElement parsed = new JsonParser().parse(resources.readText(SOUNDS_JSON_PATH));
            if (!parsed.isJsonObject()) {
                violations.add(SOUNDS_JSON_PATH + " must be a JSON object");
                return violations;
            }
            root = parsed.getAsJsonObject();
        } catch (IOException | JsonParseException e) {
            violations.add("cannot parse " + SOUNDS_JSON_PATH + ": " + e.getMessage());
            return violations;
        }
        for (Map.Entry<String, JsonElement> event : root.entrySet()) {
            checkEvent(event.getKey(), event.getValue(), resources, violations);
        }
        return violations;
    }

    private static void checkEvent(String event, JsonElement value, ResourceSource resources, List<String> violations) {
        String where = "sound event " + event + ": ";
        if (!value.isJsonObject() || !value.getAsJsonObject()
            .has("sounds")
            || !value.getAsJsonObject()
                .get("sounds")
                .isJsonArray()) {
            violations.add(where + "needs a \"sounds\" array");
            return;
        }
        for (JsonElement sound : value.getAsJsonObject()
            .getAsJsonArray("sounds")) {
            String name = soundName(sound);
            if (name == null) {
                violations.add(where + "has a sound entry that is neither a string nor an object with a name");
                continue;
            }
            if (isEventReference(sound)) {
                continue;
            }
            int colon = name.indexOf(':');
            String domain = colon < 0 ? DOMAIN : name.substring(0, colon);
            String path = colon < 0 ? name : name.substring(colon + 1);
            String file = "assets/" + domain + "/sounds/" + path + ".ogg";
            if (!resources.exists(file)) {
                violations.add(where + "file " + file + " is missing");
            }
        }
    }

    private static String soundName(JsonElement sound) {
        if (sound.isJsonPrimitive() && sound.getAsJsonPrimitive()
            .isString()) {
            return sound.getAsString();
        }
        if (sound.isJsonObject() && sound.getAsJsonObject()
            .has("name")) {
            return sound.getAsJsonObject()
                .get("name")
                .getAsString();
        }
        return null;
    }

    /** An entry with {@code "type": "event"} points at another event, not at a file. */
    private static boolean isEventReference(JsonElement sound) {
        if (!sound.isJsonObject()) {
            return false;
        }
        JsonObject object = sound.getAsJsonObject();
        return object.has("type") && "event".equals(
            object.get("type")
                .getAsString());
    }
}
