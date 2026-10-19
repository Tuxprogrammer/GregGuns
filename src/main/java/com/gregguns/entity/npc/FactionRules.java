package com.gregguns.entity.npc;

/**
 * The symmetric faction hostility table. Members of the same faction never fight; HOSTILE fights TURRET and NEUTRAL;
 * TURRET and NEUTRAL do not fight each other.
 */
// Spec: docs/techguns-spec/10-entities-npcs/ai.md (section 1, Factions)
public final class FactionRules {

    private FactionRules() {}

    /**
     * @param a first faction, or null for an entity with no faction
     * @param b second faction, or null for an entity with no faction
     * @return true when the two factions are hostile to each other; always false when either is null
     */
    public static boolean isHostile(NpcFaction a, NpcFaction b) {
        if (a == null || b == null || a == b) {
            return false;
        }
        return a == NpcFaction.HOSTILE || b == NpcFaction.HOSTILE;
    }
}
