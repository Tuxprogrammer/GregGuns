package com.gregguns.core.testing;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

/**
 * What a scenario gets: the world, an origin, and helpers that track every change so the runner can undo it. The
 * dedicated-server console has no player, so {@link #player()} may be {@code null} and the origin is the overworld
 * spawn. Server side only.
 */
public interface ScenarioContext {

    /** @return the world the scenario runs in (the overworld on the console) */
    World world();

    /** @return the origin block X */
    int originX();

    /** @return the origin block Y (standing height above the ground) */
    int originY();

    /** @return the origin block Z */
    int originZ();

    /** @return the invoking player, or {@code null} when run from the console */
    EntityPlayer player();

    /** @return randomness seeded from the scenario name, so every run draws the same numbers */
    Random random();

    /**
     * Adds an entity to the world and remembers it for cleanup.
     *
     * @param entity the entity, positioned by the caller
     * @param <T>    the entity type
     * @return the same entity
     */
    <T extends Entity> T spawn(T entity);

    /**
     * Positions an entity, then adds it to the world. Moving an entity after it joined leaves it filed in the chunk of
     * its first position (the origin), where area queries such as the turret scan cannot find it.
     *
     * @param entity the entity
     * @param x      X
     * @param y      Y
     * @param z      Z
     * @param yaw    yaw in degrees
     * @param pitch  pitch in degrees
     * @param <T>    the entity type
     * @return the same entity
     */
    default <T extends Entity> T spawnAt(T entity, double x, double y, double z, float yaw, float pitch) {
        entity.setLocationAndAngles(x, y, z, yaw, pitch);
        return spawn(entity);
    }

    /**
     * Removes an entity now (not at cleanup).
     *
     * @param entity an entity previously spawned through {@link #spawn}
     */
    void despawn(Entity entity);

    /**
     * Sets the box around the origin to air, remembering the old blocks so cleanup restores them. Block states only:
     * tile entities are not preserved, so run scenarios away from machines.
     *
     * @param radius horizontal half-size
     * @param height blocks upward from the origin Y
     */
    void clearArea(int radius, int height);

    /**
     * Places a block relative to the origin, remembering the old one for cleanup.
     *
     * @param dx    offset X
     * @param dy    offset Y
     * @param dz    offset Z
     * @param block the block
     * @param meta  the metadata
     */
    void setBlock(int dx, int dy, int dz, Block block, int meta);

    /** Removes everything spawned and restores every changed block. Called by the runner; idempotent. */
    void cleanup();
}
