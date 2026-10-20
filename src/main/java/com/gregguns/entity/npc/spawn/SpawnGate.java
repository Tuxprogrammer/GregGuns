package com.gregguns.entity.npc.spawn;

import com.gregguns.entity.npc.NpcCap;
import com.gregguns.entity.npc.NpcMob;

/**
 * The natural-spawn decision for one candidate NPC, as a pure function: dimension, faction flag and weight, danger,
 * then the caps. The first failing rule names the result, cheap rules first so the chunk scans for the caps run last.
 * Dimensions outside the list are denied silently, with no log output (NPC-16).
 *
 * <p>
 * Usage: {@code SpawnGate.decide(table, mob, dim, dims, danger, inChunk, nearPlayer, chunkCap, playerCap)}. Both
 * sides; the server uses it.
 */
// Spec: docs/gregguns-spec/10-entities-npcs/spawning.md (GGS-10-23, GGS-10-26), docs/techguns-spec/10-entities-npcs/
// index.md (NPC-16, NPC-17)
public final class SpawnGate {

    /** The outcome of {@link #decide}. */
    public enum Decision {

        /** Not a naturally spawning kind (egg, camp, dev or Nether-only mobs): the gate stays out of it. */
        NOT_MANAGED,
        /** The mob may spawn here. */
        ALLOW,
        /** The dimension is not in the config list. */
        DENY_DIMENSION,
        /** The faction is off or the weight is 0. */
        DENY_DISABLED,
        /** The danger is below the mob's table level. */
        DENY_DANGER,
        /** The chunk or the nearby player is at its cap. */
        DENY_CAP
    }

    private SpawnGate() {}

    /**
     * @param table      the spawn table
     * @param mob        the candidate
     * @param dimension  the world's dimension id
     * @param dimensions the configured spawn dimensions
     * @param danger     the danger at the position, 0 to 5
     * @param inChunk    NPCs in the candidate chunk
     * @param nearPlayer NPCs around the nearest player
     * @param chunkCap   {@code NpcConfig.perChunkCap}
     * @param playerCap  {@code NpcConfig.perPlayerCap}
     * @return the decision
     */
    public static Decision decide(SpawnTable table, NpcMob mob, int dimension, int[] dimensions, int danger,
        int inChunk, int nearPlayer, int chunkCap, int playerCap) {
        if (!isNatural(mob)) {
            return Decision.NOT_MANAGED;
        }
        if (!contains(dimensions, dimension)) {
            return Decision.DENY_DIMENSION;
        }
        SpawnTable.Entry e = table.entry(mob);
        if (e == null) {
            return Decision.DENY_DISABLED;
        }
        if (e.level() > danger) {
            return Decision.DENY_DANGER;
        }
        return NpcCap.allows(inChunk, nearPlayer, chunkCap, playerCap) ? Decision.ALLOW : Decision.DENY_CAP;
    }

    /**
     * @param mob a kind
     * @return true if the mob is one of the natural-spawn kinds (even when the config turned it off)
     */
    public static boolean isNatural(NpcMob mob) {
        if (mob == null) {
            return false;
        }
        switch (mob) {
            case ZOMBIE_FARMER:
            case ZOMBIE_MINER:
            case ZOMBIE_SOLDIER:
            case SKELETON_SOLDIER:
            case PSYCHO_STEVE:
            case BANDIT:
                return true;
            default:
                return false;
        }
    }

    private static boolean contains(int[] ids, int id) {
        if (ids == null) {
            return false;
        }
        for (int i : ids) {
            if (i == id) {
                return true;
            }
        }
        return false;
    }
}
