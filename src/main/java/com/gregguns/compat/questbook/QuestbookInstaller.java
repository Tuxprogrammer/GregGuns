package com.gregguns.compat.questbook;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.gson.JsonObject;

/**
 * Installs a {@link QuestbookBundle} into a {@link QuestbookTarget}: an idempotent upsert by fixed UUID that never
 * touches a quest or line outside the bundle. Quests an older GregGuns shipped and this one dropped are left alone.
 */
public final class QuestbookInstaller {

    private QuestbookInstaller() {}

    /** What an install does, worked out before anything is written. */
    public static final class Plan {

        public final List<UUID> addedQuests = new ArrayList<>();
        public final List<UUID> replacedQuests = new ArrayList<>();
        public boolean lineAdded;
        /** The tab order to set, or {@code null} when the line is already in it. */
        public List<UUID> newOrder;

        public int questCount() {
            return addedQuests.size() + replacedQuests.size();
        }
    }

    public static Plan plan(QuestbookBundle bundle, QuestbookTarget target) {
        Plan plan = new Plan();
        for (UUID id : bundle.quests.keySet()) {
            if (target.questIds()
                .contains(id)) {
                plan.replacedQuests.add(id);
            } else {
                plan.addedQuests.add(id);
            }
        }
        plan.lineAdded = !target.lineIds()
            .contains(bundle.lineId);
        List<UUID> order = target.lineOrder();
        List<UUID> inserted = insertOrder(order, bundle.lineId);
        plan.newOrder = inserted.equals(order) ? null : inserted;
        return plan;
    }

    /** The order with {@code lineId} appended after the existing lines; unchanged when it is already present. */
    public static List<UUID> insertOrder(List<UUID> order, UUID lineId) {
        List<UUID> out = new ArrayList<>(order);
        if (!out.contains(lineId)) {
            out.add(lineId);
        }
        return out;
    }

    /** Plans, writes, saves and syncs. Safe to run on every start. */
    public static Plan install(QuestbookBundle bundle, QuestbookTarget target) {
        Plan plan = plan(bundle, target);
        for (Map.Entry<UUID, JsonObject> quest : bundle.quests.entrySet()) {
            target.putQuest(quest.getKey(), quest.getValue());
        }
        target.putLine(bundle.lineId, bundle.line, bundle.positions);
        if (plan.newOrder != null) {
            target.setLineOrder(plan.newOrder);
        }
        target.save();
        target.sync();
        return plan;
    }
}
