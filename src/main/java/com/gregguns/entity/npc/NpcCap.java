package com.gregguns.entity.npc;

import java.util.Collection;

/**
 * Counting and capping GregGuns NPCs. Pure: callers pass entity registry names, never entities. Counting matches by
 * {@link NpcMob#byName}, so a short camp-list name and the full registered name both match (NPC-17: Techguns compared
 * a short name with the full one, so camps never counted anything and spawned without limit).
 *
 * <p>
 * Usage: {@code NpcCap.count(namesInChunk, wanted)}, then {@code NpcCap.allows(inChunk, nearPlayer, chunkCap,
 * playerCap)} before a natural spawn. A cap of 0 forbids spawning. Both sides; the server uses it.
 */
// Spec: docs/techguns-spec/10-entities-npcs/index.md (NPC-17), docs/gregguns-spec/10-entities-npcs/spawning.md
public final class NpcCap {

    private NpcCap() {}

    /**
     * Counts the entities whose registered name is one of the wanted kinds.
     *
     * @param entityNames registered names of the entities in the area ({@code EntityList.getEntityString})
     * @param wanted      kinds to count, as full or short names; null or empty counts every GregGuns NPC
     * @return how many of {@code entityNames} are a wanted kind
     */
    public static int count(Iterable<String> entityNames, Collection<String> wanted) {
        int n = 0;
        for (String name : entityNames) {
            if (matches(name, wanted)) {
                n++;
            }
        }
        return n;
    }

    /**
     * @param entityName the registered name of one entity; may be null
     * @param wanted     kinds to match (full or short names); null or empty means any GregGuns NPC
     * @return true if the entity is a GregGuns NPC and, when {@code wanted} is given, one of those kinds
     */
    public static boolean matches(String entityName, Collection<String> wanted) {
        NpcMob mob = NpcMob.byName(entityName);
        if (mob == null) {
            return false;
        }
        if (wanted == null || wanted.isEmpty()) {
            return true;
        }
        for (String w : wanted) {
            if (NpcMob.byName(w) == mob) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param inChunk    NPCs already in the candidate chunk
     * @param nearPlayer NPCs already around the nearest player
     * @param chunkCap   {@code NpcConfig.perChunkCap}
     * @param playerCap  {@code NpcConfig.perPlayerCap}
     * @return true if one more NPC may spawn: both counts are strictly below their cap
     */
    public static boolean allows(int inChunk, int nearPlayer, int chunkCap, int playerCap) {
        return inChunk < chunkCap && nearPlayer < playerCap;
    }
}
