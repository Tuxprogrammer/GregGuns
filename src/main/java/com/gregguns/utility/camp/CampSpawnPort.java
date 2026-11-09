package com.gregguns.utility.camp;

import net.minecraft.world.World;

/**
 * What the Camp Flag needs from the NPC module, which M7 supplies. Until then {@link #NONE} counts nothing and spawns
 * nothing. Server side.
 */
// Spec: docs/techguns-spec/09-machines-utility/misc-tile-entities.md section 2; M6 plan decision 14
public interface CampSpawnPort {

    /** The default port: no NPCs exist yet. */
    CampSpawnPort NONE = new CampSpawnPort() {

        @Override
        public int countNpcs(World world, java.util.List<String> names, int minX, int minY, int minZ, int maxX,
            int maxY, int maxZ) {
            return 0;
        }

        @Override
        public boolean spawn(World world, String npcName, double x, double y, double z) {
            return false;
        }

        @Override
        public void onFlagBroken(World world, int x, int y, int z) {}
    };

    /**
     * Counts the living NPCs of a camp.
     *
     * @param world the world
     * @param names the NPC entity names the flag spawns
     * @param minX  box minimum X (blocks)
     * @param minY  box minimum Y
     * @param minZ  box minimum Z
     * @param maxX  box maximum X
     * @param maxY  box maximum Y
     * @param maxZ  box maximum Z
     * @return how many living entities of those names are in the box
     */
    int countNpcs(World world, java.util.List<String> names, int minX, int minY, int minZ, int maxX, int maxY,
        int maxZ);

    /**
     * Spawns one NPC.
     *
     * @param world   the world
     * @param npcName the entity name
     * @param x       block-centered X
     * @param y       Y
     * @param z       block-centered Z
     * @return true when something was spawned
     */
    boolean spawn(World world, String npcName, double x, double y, double z);

    /**
     * Spawns a camp's commander at his post (a red base's Dictator Dave, D353). The real port keeps him near the post
     * when he is not fighting. The default spawns nothing.
     *
     * @param world   the world
     * @param npcName the entity name
     * @param x       block-centered X of the post
     * @param y       Y of the post
     * @param z       block-centered Z of the post
     * @param radius  how far he strays from the post when idle, blocks
     * @return true when he was spawned
     */
    default boolean spawnAtPost(World world, String npcName, double x, double y, double z, int radius) {
        return false;
    }

    /**
     * Called when a flag is broken, after the block is gone. The real port sends a helicopter against the player; it
     * only acts when {@code UtilityConfig.campFlagBreakHelicopter} is on, which the caller checks.
     *
     * @param world the world
     * @param x     the flag's X
     * @param y     the flag's Y
     * @param z     the flag's Z
     */
    void onFlagBroken(World world, int x, int y, int z);
}
