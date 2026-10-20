package com.gregguns.entity.npc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

/**
 * Counts living NPCs in the world without ever loading a chunk: only chunks the provider already holds are looked at
 * (the Techguns query loaded them, which could stall a tick and keep camps alive: WGEN-27). Server side.
 *
 * <p>
 * Usage: {@code NpcCounter.countInBox(world, names, minX, minY, minZ, maxX, maxY, maxZ)}; the per-chunk and per-player
 * counts feed {@link NpcCap#allows}.
 */
// Spec: docs/techguns-spec/11-worldgen/index.md (WGEN-27), docs/techguns-spec/10-entities-npcs/index.md (NPC-17)
public final class NpcCounter {

    /** Horizontal radius (blocks) of the area counted "around a player". */
    public static final int PLAYER_RADIUS = 64;

    private NpcCounter() {}

    /**
     * @param world the world
     * @param names kinds to count (full or short names); null or empty counts every GregGuns NPC
     * @param minX  box minimum X (blocks, inclusive)
     * @param minY  box minimum Y
     * @param minZ  box minimum Z
     * @param maxX  box maximum X
     * @param maxY  box maximum Y
     * @param maxZ  box maximum Z
     * @return living matching entities in the loaded chunks the box touches; unloaded chunks count zero
     */
    public static int countInBox(World world, Collection<String> names, int minX, int minY, int minZ, int maxX,
        int maxY, int maxZ) {
        AxisAlignedBB box = AxisAlignedBB.getBoundingBox(minX, minY, minZ, maxX + 1.0, maxY + 1.0, maxZ + 1.0);
        List<String> found = new ArrayList<>();
        for (int cx = minX >> 4; cx <= maxX >> 4; cx++) {
            for (int cz = minZ >> 4; cz <= maxZ >> 4; cz++) {
                // chunkExists only consults the loaded set; it never generates or loads (WGEN-27).
                if (!world.getChunkProvider()
                    .chunkExists(cx, cz)) {
                    continue;
                }
                Chunk chunk = world.getChunkFromChunkCoords(cx, cz);
                List<Entity> inChunk = new ArrayList<>();
                chunk.getEntitiesWithinAABBForEntity(null, box, inChunk, null);
                for (Entity e : inChunk) {
                    if (e instanceof EntityLivingBase && e.isEntityAlive()) {
                        found.add(EntityList.getEntityString(e));
                    }
                }
            }
        }
        return NpcCap.count(found, names);
    }

    /**
     * @param world the world
     * @param cx    chunk X
     * @param cz    chunk Z
     * @return living GregGuns NPCs in that chunk, 0 if it is not loaded
     */
    public static int countInChunk(World world, int cx, int cz) {
        return countInBox(world, null, cx << 4, 0, cz << 4, (cx << 4) + 15, 255, (cz << 4) + 15);
    }

    /**
     * @param world  the world
     * @param player the player
     * @return living GregGuns NPCs within {@link #PLAYER_RADIUS} blocks horizontally of the player (loaded chunks only)
     */
    public static int countNearPlayer(World world, EntityPlayer player) {
        int x = (int) Math.floor(player.posX);
        int z = (int) Math.floor(player.posZ);
        return countInBox(
            world,
            null,
            x - PLAYER_RADIUS,
            0,
            z - PLAYER_RADIUS,
            x + PLAYER_RADIUS,
            255,
            z + PLAYER_RADIUS);
    }
}
