package com.gregguns.compat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import betterquesting.questing.QuestDatabase;

/**
 * Reads which quests BetterQuesting's server database holds. Loaded only when BetterQuesting is (callers check
 * {@code Loader.isModLoaded("betterquesting")} first), so no BetterQuesting class loads otherwise. Server side.
 */
// Source (QuestDatabase.INSTANCE.get(UUID) returns null for an unknown quest):
// https://github.com/GTNewHorizons/BetterQuesting/blob/3.8.89-GTNH/src/main/java/betterquesting/questing/QuestDatabase.java
public final class BetterQuestingQuests {

    private BetterQuestingQuests() {}

    /**
     * @param byKey quest key to quest ID
     * @return the entries whose quest is in the database
     */
    public static Map<String, UUID> present(Map<String, UUID> byKey) {
        Map<String, UUID> out = new HashMap<>();
        for (Map.Entry<String, UUID> e : byKey.entrySet()) {
            if (QuestDatabase.INSTANCE.get(e.getValue()) != null) {
                out.put(e.getKey(), e.getValue());
            }
        }
        return out;
    }
}
