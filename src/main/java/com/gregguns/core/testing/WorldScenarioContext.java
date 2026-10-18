package com.gregguns.core.testing;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;

/**
 * The real {@link ScenarioContext}, backed by a server world. Server side only.
 */
public final class WorldScenarioContext implements ScenarioContext {

    /** Fixed base so seeds are stable across runs and builds. */
    private static final long SEED_BASE = 0x6772656767756E73L;

    private final World world;
    private final EntityPlayer player;
    private final int x;
    private final int y;
    private final int z;
    private final Random random;
    private final List<Entity> spawned = new ArrayList<>();
    private final List<int[]> changedPositions = new ArrayList<>();
    private final List<Block> changedBlocks = new ArrayList<>();

    private WorldScenarioContext(World world, EntityPlayer player, int x, int y, int z, long seed) {
        this.world = world;
        this.player = player;
        this.x = x;
        this.y = y;
        this.z = z;
        this.random = new Random(seed);
    }

    /**
     * Makes a context for a scenario.
     *
     * @param scenario  the scenario, whose name seeds the randomness
     * @param overworld the overworld, used for the origin when there is no player
     * @param player    the invoking player, or {@code null} from the console
     * @return the context; the origin is the player's block, or the overworld spawn column's surface
     */
    public static WorldScenarioContext create(Scenario scenario, World overworld, EntityPlayer player) {
        long seed = SEED_BASE ^ scenario.name()
            .hashCode();
        if (player != null) {
            return new WorldScenarioContext(
                player.worldObj,
                player,
                (int) Math.floor(player.posX),
                (int) Math.floor(player.posY),
                (int) Math.floor(player.posZ),
                seed);
        }
        ChunkCoordinates spawn = overworld.getSpawnPoint();
        int surface = overworld.getTopSolidOrLiquidBlock(spawn.posX, spawn.posZ);
        return new WorldScenarioContext(overworld, null, spawn.posX, surface, spawn.posZ, seed);
    }

    @Override
    public World world() {
        return world;
    }

    @Override
    public int originX() {
        return x;
    }

    @Override
    public int originY() {
        return y;
    }

    @Override
    public int originZ() {
        return z;
    }

    @Override
    public EntityPlayer player() {
        return player;
    }

    @Override
    public Random random() {
        return random;
    }

    @Override
    public <T extends Entity> T spawn(T entity) {
        world.spawnEntityInWorld(entity);
        spawned.add(entity);
        return entity;
    }

    @Override
    public void despawn(Entity entity) {
        entity.setDead();
        spawned.remove(entity);
    }

    @Override
    public void clearArea(int radius, int height) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = 0; dy < height; dy++) {
                    setBlock(dx, dy, dz, Blocks.air, 0);
                }
            }
        }
    }

    @Override
    public void setBlock(int dx, int dy, int dz, Block block, int meta) {
        int bx = x + dx;
        int by = y + dy;
        int bz = z + dz;
        // Remember only the first change at a position, so cleanup restores the original block.
        boolean known = false;
        for (int[] p : changedPositions) {
            if (p[0] == bx && p[1] == by && p[2] == bz) {
                known = true;
                break;
            }
        }
        if (!known) {
            changedPositions.add(new int[] { bx, by, bz, world.getBlockMetadata(bx, by, bz) });
            changedBlocks.add(world.getBlock(bx, by, bz));
        }
        world.setBlock(bx, by, bz, block, meta, 2);
    }

    @Override
    public void cleanup() {
        for (Entity e : spawned) {
            e.setDead();
        }
        spawned.clear();
        for (int i = changedPositions.size() - 1; i >= 0; i--) {
            int[] p = changedPositions.get(i);
            world.setBlock(p[0], p[1], p[2], changedBlocks.get(i), p[3], 2);
        }
        changedPositions.clear();
        changedBlocks.clear();
    }
}
