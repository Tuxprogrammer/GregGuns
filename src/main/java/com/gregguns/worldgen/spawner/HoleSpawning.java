package com.gregguns.worldgen.spawner;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;

import com.gregguns.entity.npc.InfernalHooks;
import com.gregguns.entity.npc.NpcCounter;
import com.gregguns.entity.npc.NpcEntity;
import com.gregguns.entity.npc.NpcMob;
import com.gregguns.entity.npc.NpcRegistry;
import com.gregguns.entity.npc.spawn.NpcSpawnHandler;

/**
 * One spawn attempt of a hole spawner: the player-range gate, the nearby count, the caps and the placement of each NPC.
 * It reuses the M7 pieces: {@link NpcRegistry} for what exists, {@link NpcCounter} for counting without loading a chunk
 * (NPC-17, WGEN-27), {@link NpcSpawnHandler#dangerAt} for the equipment tier and
 * {@link InfernalHooks#markSpawnerNpc} for the Infernal Mobs opt-out. Server side.
 *
 * <p>
 * Usage: {@code HoleSpawning.attempt(world, x, y, z, kind, HoleSpawning.MARKER, world::spawnEntityInWorld)}. The marker
 * and the placer are parameters only so scenarios can watch them; production passes the two constants.
 */
// Spec: PLAN-M8 G11 and contract addendum (marker BEFORE spawning); fixes NPC-17 (counting) and WGEN-27 (no chunk load)
public final class HoleSpawning {

    /** Tries for one NPC to find a free spot before it is skipped. */
    private static final int SPOT_TRIES = 8;

    /** Marks an NPC that a spawner block is about to place; called before the entity joins the world. */
    public interface Marker {

        /** @return true when the marker was written */
        boolean mark(EntityLivingBase npc);
    }

    /** Adds a positioned, marked NPC to the world. */
    public interface Placer {

        /** @return true when the entity joined the world */
        boolean place(EntityLiving npc);
    }

    /** The production marker: the M7 Infernal Mobs opt-out ({@code infernalAllowSpawnerNpcs}). */
    public static final Marker MARKER = InfernalHooks::markSpawnerNpc;

    private HoleSpawning() {}

    /**
     * @param world the world
     * @param kind  the hole kind
     * @param x     hole block X
     * @param y     hole block Y
     * @param z     hole block Z
     * @return the NPCs of {@code kind} living near the hole, counted in loaded chunks only
     */
    public static int countNearby(World world, HoleKind kind, int x, int y, int z) {
        return NpcCounter.countInBox(
            world,
            kind.mobNames(),
            x - HoleCaps.COUNT_RADIUS,
            y - HoleCaps.COUNT_BELOW,
            z - HoleCaps.COUNT_RADIUS,
            x + HoleCaps.COUNT_RADIUS,
            y + HoleCaps.COUNT_ABOVE,
            z + HoleCaps.COUNT_RADIUS);
    }

    /**
     * Runs one attempt. Does nothing unless a player is within {@link HoleCaps#PLAYER_RANGE} and the hole's own chunk is
     * loaded; never loads a chunk: a candidate spot in an unloaded chunk is skipped.
     *
     * @param world  the world
     * @param x      hole block X
     * @param y      hole block Y
     * @param z      hole block Z
     * @param kind   which NPCs to place
     * @param marker called on each NPC before it joins the world
     * @param placer adds each NPC to the world
     * @return the NPCs placed, possibly empty
     */
    public static List<EntityLiving> attempt(World world, int x, int y, int z, HoleKind kind, Marker marker,
        Placer placer) {
        List<EntityLiving> placed = new ArrayList<>();
        if (world.isRemote || !chunkLoaded(world, x, z)) {
            return placed;
        }
        if (world.getClosestPlayer(x + 0.5, y + 0.5, z + 0.5, HoleCaps.PLAYER_RANGE) == null) {
            return placed;
        }
        int want = HoleCaps.toSpawn(countNearby(world, kind, x, y, z));
        Random rand = world.rand;
        for (int i = 0; i < want; i++) {
            NpcMob mob = kind.pick(NpcRegistry::isRegistered, rand);
            if (mob == null) {
                break; // none of this kind is registered (for example Alien Bugs are off in the config)
            }
            Entity e = EntityList.createEntityByName(mob.fullName(), world);
            if (!(e instanceof EntityLiving)) {
                continue;
            }
            EntityLiving npc = (EntityLiving) e;
            if (!positionAt(world, npc, x, y, z, rand)) {
                continue;
            }
            // The contract (PLAN-M8 addendum): mark first, so Infernal Mobs never sees an unmarked spawner NPC.
            marker.mark(npc);
            if (npc instanceof NpcEntity) {
                ((NpcEntity) npc).setDanger(NpcSpawnHandler.dangerAt(world, npc.posX, npc.posZ));
            }
            npc.onSpawnWithEgg(null);
            if (placer.place(npc)) {
                placed.add(npc);
            }
        }
        return placed;
    }

    private static boolean chunkLoaded(World world, int x, int z) {
        return world.getChunkProvider()
            .chunkExists(x >> 4, z >> 4);
    }

    /** Finds a collision-free spot within {@link HoleCaps#SPREAD} of the hole in a loaded chunk and moves the NPC. */
    private static boolean positionAt(World world, EntityLiving npc, int x, int y, int z, Random rand) {
        for (int t = 0; t < SPOT_TRIES; t++) {
            int px = x + rand.nextInt(2 * HoleCaps.SPREAD + 1) - HoleCaps.SPREAD;
            int pz = z + rand.nextInt(2 * HoleCaps.SPREAD + 1) - HoleCaps.SPREAD;
            if (!chunkLoaded(world, px, pz)) {
                continue; // reading a block there would load the chunk
            }
            for (int dy = 1; dy >= 0; dy--) {
                npc.setLocationAndAngles(px + 0.5, y + dy, pz + 0.5, rand.nextFloat() * 360.0F, 0.0F);
                if (world.getCollidingBoundingBoxes(npc, npc.boundingBox)
                    .isEmpty() && !world.isAnyLiquid(npc.boundingBox)) {
                    return true;
                }
            }
        }
        return false;
    }
}
