package com.gregguns.entity.npc;

/**
 * The faction an NPC entity belongs to. Pure: no Minecraft types. Entities expose theirs through an interface added
 * with the NPC base entity; an entity with no faction (the Attack Helicopter) is represented as {@code null} and is
 * never hostile by this table.
 */
// Spec: docs/techguns-spec/10-entities-npcs/ai.md (section 1, Factions)
public enum NpcFaction {

    /** Ordinary hostile NPCs and the Alien Bug. */
    HOSTILE,
    /** Turrets placed by players' bases. */
    TURRET,
    /** Neutral NPCs. Hostile to the hostile faction only. */
    NEUTRAL
}
