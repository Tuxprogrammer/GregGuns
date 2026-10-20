package com.gregguns.core.testing;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
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
    private ScenarioChunks.Hold chunks;

    private WorldScenarioContext(World world, EntityPlayer player, int x, int y, int z, long seed) {
        this.world = world;
        this.player = player;
        this.x = x;
        this.y = y;
        this.z = z;
        this.random = new Random(seed);
    }

    /**
     * Makes a context for a scenario: picks its origin from its grid slot, force-loads the area with Forge chunk
     * tickets (released by {@link #cleanup()}), and resets the area (see {@link #prepareArea}).
     *
     * <p>
     * The base is the overworld spawn column, or the invoking player's column; the origin is the base plus the slot's
     * grid offset ({@link ScenarioArea}) at the fixed height {@link ScenarioArea#ORIGIN_Y}.
     *
     * @param scenario    the scenario, whose name seeds the randomness and picks the slot
     * @param overworld   the overworld, used for the base when there is no player
     * @param player      the invoking player, or {@code null} from the console
     * @param sortedNames the names of every registered scenario, sorted (the slot is the index of this one)
     * @return the context, ready to run in
     */
    public static WorldScenarioContext create(Scenario scenario, World overworld, EntityPlayer player,
        List<String> sortedNames) {
        long seed = SEED_BASE ^ scenario.name()
            .hashCode();
        World world;
        int baseX;
        int baseZ;
        if (player != null) {
            world = player.worldObj;
            baseX = (int) Math.floor(player.posX);
            baseZ = (int) Math.floor(player.posZ);
        } else {
            world = overworld;
            ChunkCoordinates spawn = overworld.getSpawnPoint();
            baseX = spawn.posX;
            baseZ = spawn.posZ;
        }
        int[] offset = ScenarioArea.offsetOf(ScenarioArea.slotOf(sortedNames, scenario.name()));
        WorldScenarioContext ctx = new WorldScenarioContext(
            world,
            player,
            baseX + offset[0],
            ScenarioArea.ORIGIN_Y,
            baseZ + offset[1],
            seed);
        int radius = ScenarioArea.clampRadius(scenario.areaRadius());
        ctx.chunks = ScenarioChunks.hold(world, ctx.x, ctx.z, radius);
        try {
            ctx.prepareArea(radius);
        } catch (RuntimeException e) {
            ctx.chunks.release();
            throw e;
        }
        return ctx;
    }

    /**
     * Resets the scenario's area before it runs, whatever an earlier run (or StructureLib, or GT) left there: kills
     * every non-player entity in it, sets {@link ScenarioArea#CLEAR_HEIGHT} layers above the origin to air (which also
     * removes tile entities), and lays a stone floor on the layer below. Written straight to the world, not through
     * {@link #setBlock}, so {@link #cleanup()} does not "restore" the residue afterwards. The area is a dev-only arena
     * at a fixed height, so nothing worth keeping is overwritten.
     */
    private void prepareArea(int radius) {
        AxisAlignedBB box = AxisAlignedBB.getBoundingBox(
            x - radius,
            y - 2,
            z - radius,
            x + radius + 1,
            y + ScenarioArea.CLEAR_HEIGHT,
            z + radius + 1);
        for (Object o : world.getEntitiesWithinAABB(Entity.class, box)) {
            if (!(o instanceof EntityPlayer)) {
                ((Entity) o).setDead();
            }
        }
        for (int bx = x - radius; bx <= x + radius; bx++) {
            for (int bz = z - radius; bz <= z + radius; bz++) {
                if (world.getBlock(bx, y - 1, bz) != Blocks.stone || world.getBlockMetadata(bx, y - 1, bz) != 0) {
                    world.setBlock(bx, y - 1, bz, Blocks.stone, 0, 2);
                }
                for (int by = y; by < y + ScenarioArea.CLEAR_HEIGHT; by++) {
                    if (world.getBlock(bx, by, bz) != Blocks.air || world.getTileEntity(bx, by, bz) != null) {
                        world.setBlock(bx, by, bz, Blocks.air, 0, 2);
                    }
                }
            }
        }
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
        remove(entity);
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

    /**
     * Kills an entity. A fake player must also leave the world's player list: {@code setDead} alone leaves it there,
     * where it stays in the arena and soaks up projectiles aimed at later scenarios' targets.
     */
    private void remove(Entity entity) {
        if (entity instanceof EntityPlayer) {
            world.removeEntity(entity);
        } else {
            entity.setDead();
        }
    }

    @Override
    public void cleanup() {
        for (Entity e : spawned) {
            remove(e);
        }
        spawned.clear();
        for (int i = changedPositions.size() - 1; i >= 0; i--) {
            int[] p = changedPositions.get(i);
            world.setBlock(p[0], p[1], p[2], changedBlocks.get(i), p[3], 2);
        }
        changedPositions.clear();
        changedBlocks.clear();
        if (chunks != null) {
            chunks.release();
            chunks = null;
        }
    }
}
