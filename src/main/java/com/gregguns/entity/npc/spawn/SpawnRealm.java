package com.gregguns.entity.npc.spawn;

import com.gregguns.core.config.NpcConfig;

/**
 * The three natural-spawn realms: the Overworld table (zombies, soldiers, bandits, Psycho Steve), the Nether table
 * (Zombie Pigman Soldiers, Super Mutants, the Cyberdemon) and the End table (Commandos). Each realm has its own
 * dimension list in the config, its own biome set for the spawn lists and its own danger rule. Pure: no Minecraft
 * types.
 *
 * <p>
 * Usage: {@code SpawnRealm.of(world.provider.dimensionId, SpawnRealm.Dimensions.fromConfig())}; null means natural NPC
 * spawning is off in that dimension. Both sides; the server uses it.
 */
// Spec: docs/gregguns-spec/10-entities-npcs/spawning.md (GGS-10-23, GGS-10-41 to GGS-10-43), design record 0005, D353
public enum SpawnRealm {

    /** Every biome that is neither Nether nor End; dimensions in {@code npcs.spawnDimensions}. */
    OVERWORLD,
    /** The Nether biomes; dimensions in {@code npcs.netherSpawnDimensions}. */
    NETHER,
    /** The End biomes; dimensions in {@code npcs.endSpawnDimensions}. */
    END;

    /** Coordinate scale of the Nether: one Nether block is eight Overworld blocks. */
    public static final int NETHER_SCALE = 8;

    /** The three dimension lists, as a snapshot so tests need no config. */
    public static final class Dimensions {

        public final int[] overworld;
        public final int[] nether;
        public final int[] end;

        /**
         * @param overworld dimension ids of the Overworld table
         * @param nether    dimension ids of the Nether table
         * @param end       dimension ids of the End table
         */
        public Dimensions(int[] overworld, int[] nether, int[] end) {
            this.overworld = overworld == null ? new int[0] : overworld.clone();
            this.nether = nether == null ? new int[0] : nether.clone();
            this.end = end == null ? new int[0] : end.clone();
        }

        /** @return the pack defaults: Overworld 0, Nether -1, End 1 */
        public static Dimensions defaults() {
            return new Dimensions(new int[] { 0 }, new int[] { -1 }, new int[] { 1 });
        }

        /** @return a snapshot of {@link NpcConfig} */
        public static Dimensions fromConfig() {
            return new Dimensions(
                NpcConfig.spawnDimensions,
                NpcConfig.netherSpawnDimensions,
                NpcConfig.endSpawnDimensions);
        }
    }

    /**
     * The realm of a dimension. A dimension listed in more than one list takes the first: Overworld, then Nether, then
     * End.
     *
     * @param dimension a dimension id
     * @param dims      the dimension lists
     * @return the realm, or null when natural NPC spawning is off there
     */
    public static SpawnRealm of(int dimension, Dimensions dims) {
        if (contains(dims.overworld, dimension)) {
            return OVERWORLD;
        }
        if (contains(dims.nether, dimension)) {
            return NETHER;
        }
        return contains(dims.end, dimension) ? END : null;
    }

    /**
     * The spawn danger of a spot in this realm, 0 to {@link SpawnDanger#MAX}.
     * <ul>
     * <li>Overworld: distance from the world spawn plus the biome's class.</li>
     * <li>Nether: the Overworld distance the spot stands for (Nether distance times {@link #NETHER_SCALE}), plus 1:
     * the Nether is as rough as a cold or rugged biome.</li>
     * <li>End: the distance from the End's center, plus 2: the End is as harsh as a wasteland.</li>
     * </ul>
     *
     * @param distance    the realm's own distance: from the world spawn for the Overworld and the Nether (Nether
     *                    blocks, unscaled), from the End's center (0, 0) for the End
     * @param biomeDanger the biome class part, 0 to 2; read in the Overworld only
     * @param level0      {@code NpcConfig.distanceLevel0}
     * @param level1      {@code NpcConfig.distanceLevel1}
     * @param level2      {@code NpcConfig.distanceLevel2}
     * @return the danger
     */
    public int danger(double distance, int biomeDanger, int level0, int level1, int level2) {
        switch (this) {
            case NETHER:
                return SpawnDanger
                    .total(SpawnDanger.distance(distance * NETHER_SCALE, level0, level1, level2), NETHER_BASE_DANGER);
            case END:
                return SpawnDanger.total(SpawnDanger.distance(distance, level0, level1, level2), END_BASE_DANGER);
            default:
                return SpawnDanger.total(SpawnDanger.distance(distance, level0, level1, level2), biomeDanger);
        }
    }

    /** The Nether's fixed biome part of the danger. */
    public static final int NETHER_BASE_DANGER = 1;
    /** The End's fixed biome part of the danger. */
    public static final int END_BASE_DANGER = 2;

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
