package com.gregguns.entity.npc.spawn;

/**
 * The spawn danger level, 0 to 5, as pure functions: distance from the world spawn (0 to 3) plus the biome (0 to 2).
 * It picks which spawn tables are open and which equipment tier a new NPC rolls. No Minecraft types: the handler maps
 * the biome to a {@link BiomeClass} and the distance to blocks.
 *
 * <p>
 * Usage: {@code SpawnDanger.total(SpawnDanger.distance(dist, l0, l1, l2), BiomeClass.HARSH.danger())}. Both sides.
 */
// Spec: docs/techguns-spec/10-entities-npcs/spawning.md (1.3 danger level), docs/gregguns-spec/10-entities-npcs/
// spawning.md (GGS-10-23)
public final class SpawnDanger {

    /** Highest danger level. */
    public static final int MAX = 5;

    /** How dangerous a biome is. Deviation: 1.7.10 has no SANDY/SNOWY/SPOOKY types, the handler maps the closest. */
    public enum BiomeClass {

        /** Everything else, and the Nether and the End (0). */
        NEUTRAL(0),
        /** Cold or rugged (1). */
        SPARSE(1),
        /** Wasteland, desert, frozen, swamp or magical (2). */
        HARSH(2);

        private final int danger;

        BiomeClass(int danger) {
            this.danger = danger;
        }

        /** @return the biome part of the danger, 0 to 2 */
        public int danger() {
            return danger;
        }
    }

    private SpawnDanger() {}

    /**
     * @param distance horizontal distance to the world spawn, blocks
     * @param level0   distance below which the danger is 0 ({@code NpcConfig.distanceLevel0})
     * @param level1   distance below which the danger is 1
     * @param level2   distance below which the danger is 2; at or beyond it the danger is 3
     * @return the distance part, 0 to 3
     */
    public static int distance(double distance, int level0, int level1, int level2) {
        if (distance < level0) {
            return 0;
        }
        if (distance < level1) {
            return 1;
        }
        return distance < level2 ? 2 : 3;
    }

    /**
     * @param distanceDanger 0 to 3
     * @param biomeDanger    0 to 2
     * @return their sum, clamped to 0 to {@link #MAX}
     */
    public static int total(int distanceDanger, int biomeDanger) {
        return Math.max(0, Math.min(MAX, distanceDanger + biomeDanger));
    }
}
