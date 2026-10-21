package com.gregguns.compat.questbook;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.google.gson.JsonObject;

/**
 * The live quest database, as the installer needs it. The BetterQuesting-backed implementation is
 * {@code BetterQuestingTarget}; tests use an in-memory one.
 */
public interface QuestbookTarget {

    Set<UUID> questIds();

    Set<UUID> lineIds();

    /** The tab order, first tab first. */
    List<UUID> lineOrder();

    /** Adds the quest, or replaces its definition and keeps every player's progress on it. */
    void putQuest(UUID id, JsonObject definition);

    /** Adds the line, or replaces it (its properties and the quest positions on it). Other lines are untouched. */
    void putLine(UUID id, JsonObject definition, Map<UUID, JsonObject> positions);

    /** Sets the tab order. Called only when the order changed. */
    void setLineOrder(List<UUID> order);

    /** Writes the database to disk. */
    void save();

    /** Sends the changes to connected clients. */
    void sync();
}
