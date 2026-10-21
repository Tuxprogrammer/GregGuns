package com.gregguns.compat.questbook;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * {@code index.json}: the list of files that make up the shipped quest line. Format 1 has the keys {@code format},
 * {@code line}, {@code linePositions}, {@code quests} and {@code orderEntry}.
 */
public final class QuestbookIndex {

    public static final int FORMAT = 1;

    public final String line;
    public final List<String> linePositions;
    public final List<String> quests;
    /** A {@code QuestLinesOrder.txt} line; only the text before the first colon is the line id. */
    public final String orderEntry;

    private QuestbookIndex(String line, List<String> linePositions, List<String> quests, String orderEntry) {
        this.line = line;
        this.linePositions = Collections.unmodifiableList(linePositions);
        this.quests = Collections.unmodifiableList(quests);
        this.orderEntry = orderEntry;
    }

    /** The Base64 line id from {@link #orderEntry} (the text before the first colon), or "" when there is none. */
    public String orderLineToken() {
        int colon = orderEntry.indexOf(':');
        return (colon < 0 ? orderEntry : orderEntry.substring(0, colon)).trim();
    }

    public static QuestbookIndex parse(String json) throws QuestbookException {
        JsonObject root;
        try {
            JsonElement parsed = new JsonParser().parse(json);
            if (!parsed.isJsonObject()) {
                throw new QuestbookException("index.json is not a JSON object");
            }
            root = parsed.getAsJsonObject();
        } catch (RuntimeException e) {
            throw new QuestbookException("index.json is not valid JSON", e);
        }
        try {
            if (!root.has("format") || root.get("format")
                .getAsInt() != FORMAT) {
                throw new QuestbookException("index.json has an unsupported format (expected " + FORMAT + ")");
            }
            String line = string(root, "line");
            if (line.isEmpty()) {
                throw new QuestbookException("index.json names no quest line");
            }
            return new QuestbookIndex(
                line,
                strings(root, "linePositions"),
                strings(root, "quests"),
                string(root, "orderEntry"));
        } catch (RuntimeException e) {
            throw new QuestbookException("index.json has a malformed entry", e);
        }
    }

    private static String string(JsonObject o, String key) {
        return o.has(key) && o.get(key)
            .isJsonPrimitive() ? o.get(key)
                .getAsString() : "";
    }

    private static List<String> strings(JsonObject o, String key) throws QuestbookException {
        List<String> out = new ArrayList<>();
        if (o.has(key)) {
            if (!o.get(key)
                .isJsonArray()) {
                throw new QuestbookException("index.json: " + key + " is not an array");
            }
            for (JsonElement e : o.getAsJsonArray(key)) {
                out.add(e.getAsString());
            }
        }
        return out;
    }
}
