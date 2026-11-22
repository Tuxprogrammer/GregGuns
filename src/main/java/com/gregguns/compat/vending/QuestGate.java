package com.gregguns.compat.vending;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import com.gregguns.api.Tier;

/**
 * The quest a tier's Vending Machine trades wait for: the GTNH pack's own tier quest ("Welcome to Tier 0.5, Steam!",
 * "Welcome to Tier 1, LV", "Tier 2 (MV)" ... "Tier 7 (ZPM)"). Every pack player completes these on the way through the
 * tiers, so a player who adds GregGuns mid-game finds the shop open up to the tier they have reached, and no GregGuns
 * quest has to be done first. That is the quest book's own rule: a tier gate is a pack quest, never GregGuns' chapter
 * order. Pack quests have legacy numeric IDs, which BetterQuesting stores as the UUID {@code (0, id)}. Pure logic, both
 * sides.
 */
// Spec: docs/gregguns-spec/06-items/coins-and-vending.md (Gates); docs/gregguns-spec/12-questbook/index.md (Referenced
// pack quests: 44, 88, 1148, 160, 176, 213, 1493, 2610)
// Source (the pack's tier quests, questIDHigh 0, in the shipped quest database; LV to ZPM are also the first
// prerequisite of the matching gg.skip.<tier> trigger in tools/questbook/quests/skip.json):
// reference/gtnh/2.9.0-RC-2/.minecraft/config/betterquesting/DefaultQuests/Quests/Tier*/
public final class QuestGate {

    private static final Map<Tier, Integer> PACK_TIER_QUEST = new EnumMap<>(Tier.class);

    static {
        PACK_TIER_QUEST.put(Tier.STEAM, 44); // Welcome to Tier 0.5, Steam!
        PACK_TIER_QUEST.put(Tier.LV, 88); // Welcome to Tier 1, LV
        PACK_TIER_QUEST.put(Tier.MV, 1148); // Tier 2 (MV)
        PACK_TIER_QUEST.put(Tier.HV, 160); // Tier 3 (HV)
        PACK_TIER_QUEST.put(Tier.EV, 176); // Tier 4 (EV)
        PACK_TIER_QUEST.put(Tier.IV, 213); // Tier 5 (IV)
        PACK_TIER_QUEST.put(Tier.LUV, 1493); // Tier 6 (LuV)
        PACK_TIER_QUEST.put(Tier.ZPM, 2610); // Tier 7 (ZPM)
    }

    private QuestGate() {}

    /**
     * @param tier a tier
     * @return the pack's legacy quest number of the tier's quest
     */
    public static int packQuest(Tier tier) {
        Integer id = PACK_TIER_QUEST.get(tier);
        if (id == null) {
            throw new IllegalArgumentException("no pack tier quest for " + tier);
        }
        return id;
    }

    /**
     * @param tier a tier
     * @return the key the trade builder looks the gate up by, e.g. {@code pack.lv}
     */
    public static String gateKey(Tier tier) {
        return "pack." + tier.name()
            .toLowerCase(Locale.ROOT);
    }

    /**
     * @param tier a tier
     * @return the BetterQuesting ID of the tier's pack quest: the legacy number as the low half, zero as the high half
     */
    public static UUID questId(Tier tier) {
        return new UUID(0L, packQuest(tier));
    }
}
