package com.gregguns.compat.questbook;

import java.io.IOException;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Everything the shipped questbook installs, read and checked: the line, the position of each quest on it, and the
 * quests. Ids are BetterQuesting UUIDs read from the files' typed long keys, so they are the fixed uuid5 ids the
 * content generator wrote.
 */
public final class QuestbookBundle {

    public final QuestbookIndex index;
    public final UUID lineId;
    public final JsonObject line;
    /** Position files, keyed by the quest id inside them, in index order. */
    public final Map<UUID, JsonObject> positions;
    /** Quest files, keyed by the quest id inside them, in index order. */
    public final Map<UUID, JsonObject> quests;

    private QuestbookBundle(QuestbookIndex index, UUID lineId, JsonObject line, Map<UUID, JsonObject> positions,
        Map<UUID, JsonObject> quests) {
        this.index = index;
        this.lineId = lineId;
        this.line = line;
        this.positions = Collections.unmodifiableMap(positions);
        this.quests = Collections.unmodifiableMap(quests);
    }

    /**
     * Reads {@code index.json} and every file it names.
     *
     * @return the bundle, or {@code null} when {@code index.json} does not exist (the jar ships no questbook)
     * @throws QuestbookException when a named file is missing or malformed; nothing is half-read
     */
    public static QuestbookBundle load(QuestbookSource source) throws QuestbookException {
        try {
            String indexText = source.read("index.json");
            if (indexText == null) {
                return null;
            }
            QuestbookIndex index = QuestbookIndex.parse(indexText);
            JsonObject line = readObject(source, index.line);
            UUID lineId = id(line, "questLineIDHigh:4", "questLineIDLow:4", index.line);
            String orderToken = index.orderLineToken();
            if (!orderToken.isEmpty() && !norm(orderToken).equals(norm(token(lineId)))) {
                throw new QuestbookException("orderEntry id does not match the line id in " + index.line);
            }
            Map<UUID, JsonObject> positions = new LinkedHashMap<>();
            for (String path : index.linePositions) {
                JsonObject o = readObject(source, path);
                positions.put(id(o, "questIDHigh:4", "questIDLow:4", path), o);
            }
            Map<UUID, JsonObject> quests = new LinkedHashMap<>();
            for (String path : index.quests) {
                JsonObject o = readObject(source, path);
                quests.put(id(o, "questIDHigh:4", "questIDLow:4", path), o);
            }
            for (UUID positioned : positions.keySet()) {
                if (!quests.containsKey(positioned)) {
                    throw new QuestbookException("A position file names a quest that is not shipped: " + positioned);
                }
            }
            return new QuestbookBundle(index, lineId, line, positions, quests);
        } catch (IOException e) {
            throw new QuestbookException("Could not read the questbook resources", e);
        }
    }

    /** A UUID as BetterQuesting's file names and order file write it: the 16 bytes as URL-safe Base64 with padding. */
    public static String token(UUID id) {
        byte[] bytes = new byte[16];
        long hi = id.getMostSignificantBits();
        long lo = id.getLeastSignificantBits();
        for (int i = 0; i < 8; i++) {
            bytes[i] = (byte) (hi >>> (56 - 8 * i));
            bytes[8 + i] = (byte) (lo >>> (56 - 8 * i));
        }
        return Base64.getUrlEncoder()
            .encodeToString(bytes);
    }

    private static String norm(String base64) {
        return base64.replace('+', '-')
            .replace('/', '_');
    }

    private static JsonObject readObject(QuestbookSource source, String path) throws IOException, QuestbookException {
        String text = source.read(path);
        if (text == null) {
            throw new QuestbookException("index.json names a missing file: " + path);
        }
        try {
            return new JsonParser().parse(text)
                .getAsJsonObject();
        } catch (RuntimeException e) {
            throw new QuestbookException("Not a JSON object: " + path, e);
        }
    }

    private static UUID id(JsonObject o, String highKey, String lowKey, String path) throws QuestbookException {
        try {
            return new UUID(
                o.get(highKey)
                    .getAsLong(),
                o.get(lowKey)
                    .getAsLong());
        } catch (RuntimeException e) {
            throw new QuestbookException("Missing " + highKey + "/" + lowKey + " in " + path, e);
        }
    }
}
