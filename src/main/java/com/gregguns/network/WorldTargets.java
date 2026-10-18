package com.gregguns.network;

import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * Target lookups for packet validation. None of them loads a chunk, so a packet naming far-away coordinates cannot
 * force chunk loading (CORE-08). Use them as the supplier in {@code rules.targetOfType(...)}.
 *
 * Side: both (World and Entity are common classes).
 */
public final class WorldTargets {

    private WorldTargets() {}

    /**
     * Returns the tile entity at the position, or {@code null} when the chunk is not loaded or there is no tile.
     *
     * @param world the world to look in
     * @param x     block x
     * @param y     block y
     * @param z     block z
     */
    public static TileEntity tile(World world, int x, int y, int z) {
        if (world == null || !world.blockExists(x, y, z)) {
            return null;
        }
        return world.getTileEntity(x, y, z);
    }

    /**
     * Returns the entity with the id, or {@code null} when missing.
     *
     * @param world    the world to look in
     * @param entityId the network entity id
     */
    public static Entity entity(World world, int entityId) {
        return world == null ? null : world.getEntityByID(entityId);
    }
}
