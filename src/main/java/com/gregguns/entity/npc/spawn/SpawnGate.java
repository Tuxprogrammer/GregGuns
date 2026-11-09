package com.gregguns.entity.npc.spawn;

import com.gregguns.core.config.NpcConfig;
import com.gregguns.entity.npc.NpcCap;
import com.gregguns.entity.npc.NpcMob;

/**
 * The natural-spawn decision for one candidate NPC, as a pure function: realm (from the dimension), the realm's table
 * (faction flag and weight), danger, the End's keep-out around its center, then the caps and the boss spacing. The
 * first failing rule names the result, cheap rules first so the chunk scans run last. Dimensions outside every list are
 * denied silently, with no log output (NPC-16).
 *
 * <p>
 * Usage: {@code SpawnGate.decide(table, mob, realm, danger, centerDistance, inChunk, nearPlayer, bossesNear, limits)}.
 * Both sides; the server uses it.
 */
// Spec: docs/gregguns-spec/10-entities-npcs/spawning.md (GGS-10-23, GGS-10-26, GGS-10-41, GGS-10-43),
// docs/techguns-spec/10-entities-npcs/index.md (NPC-16, NPC-17)
public final class SpawnGate {

    /** The outcome of {@link #decide}. */
    public enum Decision {

        /** Not a naturally spawning kind (egg, camp, spawner or dev mobs): the gate stays out of it. */
        NOT_MANAGED,
        /** The mob may spawn here. */
        ALLOW,
        /** The dimension is in no spawn list. */
        DENY_DIMENSION,
        /** The mob has no row in this dimension's realm (a Bandit in the Nether, a Commando in the Overworld). */
        DENY_REALM,
        /** The faction is off or the weight is 0. */
        DENY_DISABLED,
        /** The danger is below the mob's table level. */
        DENY_DANGER,
        /** Too close to the End's center: the dragon's island stays clear. */
        DENY_DISTANCE,
        /** The chunk or the nearby player is at its cap. */
        DENY_CAP,
        /** A boss of the same kind is already within the boss spacing. */
        DENY_BOSS
    }

    /** The config numbers the gate reads, as a snapshot. */
    public static final class Limits {

        public final int chunkCap;
        public final int playerCap;
        public final int endMinDistance;

        /**
         * @param chunkCap       {@code NpcConfig.perChunkCap}
         * @param playerCap      {@code NpcConfig.perPlayerCap}
         * @param endMinDistance {@code NpcConfig.endMinDistance}
         */
        public Limits(int chunkCap, int playerCap, int endMinDistance) {
            this.chunkCap = chunkCap;
            this.playerCap = playerCap;
            this.endMinDistance = endMinDistance;
        }

        /** @return the live config */
        public static Limits fromConfig() {
            return new Limits(NpcConfig.perChunkCap, NpcConfig.perPlayerCap, NpcConfig.endMinDistance);
        }

        /** @return limits that never deny: for the rule-only first pass, before anything is counted */
        static Limits rulesOnly(int endMinDistance) {
            return new Limits(Integer.MAX_VALUE, Integer.MAX_VALUE, endMinDistance);
        }
    }

    private SpawnGate() {}

    /**
     * @param table          the spawn table
     * @param mob            the candidate
     * @param realm          the realm of the dimension ({@link SpawnRealm#of}); null for an unlisted dimension
     * @param danger         the danger at the position, 0 to 5
     * @param centerDistance horizontal distance to (0, 0), blocks; read in the End only
     * @param inChunk        NPCs in the candidate chunk
     * @param nearPlayer     NPCs around the nearest player
     * @param bossesNear     NPCs of the candidate's kind within {@code NpcConfig.bossSpacing}; read for bosses only
     * @param limits         the caps and the End keep-out
     * @return the decision
     */
    public static Decision decide(SpawnTable table, NpcMob mob, SpawnRealm realm, int danger, double centerDistance,
        int inChunk, int nearPlayer, int bossesNear, Limits limits) {
        if (!isNatural(mob)) {
            return Decision.NOT_MANAGED;
        }
        if (realm == null) {
            return Decision.DENY_DIMENSION;
        }
        if (!hasRow(mob, realm)) {
            return Decision.DENY_REALM;
        }
        SpawnTable.Entry e = table.entry(mob, realm);
        if (e == null) {
            return Decision.DENY_DISABLED;
        }
        if (e.level() > danger) {
            return Decision.DENY_DANGER;
        }
        if (realm == SpawnRealm.END && centerDistance < limits.endMinDistance) {
            return Decision.DENY_DISTANCE;
        }
        if (!NpcCap.allows(inChunk, nearPlayer, limits.chunkCap, limits.playerCap)) {
            return Decision.DENY_CAP;
        }
        return e.boss() && bossesNear > 0 ? Decision.DENY_BOSS : Decision.ALLOW;
    }

    /**
     * @param mob a kind
     * @return true if the mob is one of the natural-spawn kinds of any realm (even when the config turned it off)
     */
    public static boolean isNatural(NpcMob mob) {
        return mob != null
            && (hasRow(mob, SpawnRealm.OVERWORLD) || hasRow(mob, SpawnRealm.NETHER) || hasRow(mob, SpawnRealm.END));
    }

    /**
     * @param mob   a kind
     * @param realm a realm
     * @return true if the realm's table has a row for that kind, whatever the config says
     */
    public static boolean hasRow(NpcMob mob, SpawnRealm realm) {
        if (mob == null || realm == null) {
            return false;
        }
        switch (mob) {
            case ZOMBIE_FARMER:
            case ZOMBIE_MINER:
            case ZOMBIE_SOLDIER:
            case SKELETON_SOLDIER:
            case PSYCHO_STEVE:
            case BANDIT:
                return realm == SpawnRealm.OVERWORLD;
            case ZOMBIE_PIGMAN_SOLDIER:
            case SUPER_MUTANT_BASIC:
            case SUPER_MUTANT_HEAVY:
            case SUPER_MUTANT_ELITE:
            case CYBER_DEMON:
                return realm == SpawnRealm.NETHER;
            case COMMANDO:
                return realm == SpawnRealm.END;
            default:
                return false;
        }
    }
}
