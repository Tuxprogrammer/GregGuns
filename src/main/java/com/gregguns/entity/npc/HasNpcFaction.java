package com.gregguns.entity.npc;

/**
 * Implemented by every entity that belongs to an {@link NpcFaction}. An entity that does not implement it counts as a
 * non-NPC in {@link NpcTargetRules#shouldRetaliate}. Pure interface, so common code may hold it.
 */
// Spec: docs/techguns-spec/10-entities-npcs/ai.md (section 1, ITGNpcTeam)
public interface HasNpcFaction {

    /** @return this entity's faction; never null for an implementer */
    NpcFaction npcFaction();
}
