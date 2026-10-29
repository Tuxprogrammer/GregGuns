package com.gregguns.entity.npc.spawn;

import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.MobSpawnerBaseLogic;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

/**
 * How a GregGuns NPC behaves when a vanilla mob spawner with GregGuns {@link SpawnerData} places it.
 *
 * <p>
 * The light rule: vanilla asks {@code getCanSpawnHere()} before it applies {@code SpawnData}, and a monster's check
 * refuses bright spots, so a lit silo complex would never spawn its guards. An NPC therefore asks
 * {@link #placedByMarkedSpawner}: is it a natural-spawn candidate (never, if the {@code CheckSpawn} event saw it), and
 * if not, is there a GregGuns-marked spawner of its own type in the box that spawner spawns in? Only then does it use
 * {@link #canSpawnFromSpawner}, which keeps peaceful, collision and liquid checks and drops the light checks. Natural
 * spawning, spawn eggs and plain vanilla spawners keep the full vanilla rules. The scan reads only loaded chunks and
 * never loads one. Server side.
 *
 * <p>
 * A marked spawner counts only while it stands on the block worldgen placed it at ({@link #markedAtOrigin}): a
 * spawner carried elsewhere with its NBT (a JABBA diamond dolly) is a plain vanilla spawner of its mob, with the light
 * rules, no danger floor and no blast exception.
 */
// Spec: docs/gregguns-spec/10-entities-npcs/spawning.md (spawner NPCs)
// Source: Minecraft 1.7.10 MobSpawnerBaseLogic#updateSpawner (setLocationAndAngles, getCanSpawnHere, then
// func_98265_a); EntityMob#getCanSpawnHere and #getBlockPathWeight (the two light checks); EntityLiving#getCanSpawnHere
// (the collision and liquid checks kept here); SpawnerAnimals#findChunksForSpawning (CheckSpawn fires before
// getCanSpawnHere)
public final class SpawnerOrigin {

    private SpawnerOrigin() {}

    /** Implemented by the NPCs a GregGuns spawner places, so the natural-spawn gate can mark candidates. */
    public interface NaturalCandidate {

        /** Called from the {@code CheckSpawn} event: this entity is a natural-spawn candidate, keep every rule. */
        void markNaturalCandidate();
    }

    /**
     * @param npc              the NPC about to be checked, already positioned
     * @param naturalCandidate whether the {@code CheckSpawn} event saw it
     * @return true when a GregGuns-marked spawner of its type is placing it
     */
    public static boolean placedByMarkedSpawner(EntityLiving npc, boolean naturalCandidate) {
        World world = npc.worldObj;
        if (naturalCandidate || world == null || world.isRemote) {
            return false;
        }
        String name = EntityList.getEntityString(npc);
        if (name == null) {
            return false;
        }
        int r = SpawnerData.SCAN_RANGE;
        int cx0 = MathHelper.floor_double(npc.posX - r) >> 4;
        int cx1 = MathHelper.floor_double(npc.posX + r) >> 4;
        int cz0 = MathHelper.floor_double(npc.posZ - r) >> 4;
        int cz1 = MathHelper.floor_double(npc.posZ + r) >> 4;
        for (int cx = cx0; cx <= cx1; cx++) {
            for (int cz = cz0; cz <= cz1; cz++) {
                if (!world.getChunkProvider()
                    .chunkExists(cx, cz)) {
                    continue; // never load a chunk for this
                }
                Chunk chunk = world.getChunkFromChunkCoords(cx, cz);
                for (TileEntity t : chunk.chunkTileEntityMap.values()) {
                    if (t instanceof TileEntityMobSpawner && !t.isInvalid()
                        && SpawnerData.inSpawnBox(npc.posX - t.xCoord, npc.posY - t.yCoord, npc.posZ - t.zCoord)
                        && spawnsMarked((TileEntityMobSpawner) t, name)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * @param tile a spawner tile
     * @param name an entity name
     * @return true when the spawner's current entry is that entity and the spawner is {@link #markedAtOrigin}
     */
    public static boolean spawnsMarked(TileEntityMobSpawner tile, String name) {
        return name.equals(
            tile.func_145881_a()
                .getEntityNameToSpawn())
            && markedAtOrigin(tile);
    }

    /**
     * Whether a spawner is a GregGuns spawner on the block worldgen placed it at: its current entry carries
     * {@link SpawnerData} whose origin is the tile's own position. Every entry of a GregGuns spawner carries the same
     * data, so the current one stands for all of them.
     *
     * @param tile a spawner tile, may be null
     * @return true for a GregGuns spawner that has not been moved
     */
    public static boolean markedAtOrigin(TileEntityMobSpawner tile) {
        if (tile == null) {
            return false;
        }
        MobSpawnerBaseLogic.WeightedRandomMinecart entry = tile.func_145881_a()
            .getRandomEntity();
        if (entry == null) {
            return false;
        }
        NBTTagCompound data = entry.field_98222_b;
        return SpawnerData.atOrigin(data, tile.xCoord, tile.yCoord, tile.zCoord);
    }

    /**
     * Whether an NPC whose merged spawn NBT carries {@link SpawnerData} is being placed by that spawner on its own
     * origin: the origin is within the spawn box of the NPC, its chunk is loaded, and the tile there is a spawner
     * {@link #markedAtOrigin}. False for a spawner carried away from its origin, so its NPCs get no danger floor.
     *
     * @param npc the NPC, positioned, with a server world
     * @param tag its merged NBT
     * @return true when the GregGuns setup applies
     */
    public static boolean placedFromOrigin(EntityLiving npc, NBTTagCompound tag) {
        World world = npc.worldObj;
        if (world == null || world.isRemote || !SpawnerData.originNear(tag, npc.posX, npc.posY, npc.posZ)) {
            return false;
        }
        NBTTagCompound inner = tag.getCompoundTag(SpawnerData.KEY);
        int x = inner.getInteger(SpawnerData.ORIGIN_X);
        int y = inner.getInteger(SpawnerData.ORIGIN_Y);
        int z = inner.getInteger(SpawnerData.ORIGIN_Z);
        if (!world.getChunkProvider()
            .chunkExists(x >> 4, z >> 4)) {
            return false; // never load a chunk for this
        }
        TileEntity t = world.getTileEntity(x, y, z);
        return t instanceof TileEntityMobSpawner && !t.isInvalid() && markedAtOrigin((TileEntityMobSpawner) t);
    }

    /**
     * The spot check for a spawner NPC: vanilla's without the two light checks.
     *
     * @param npc the positioned NPC
     * @return true when it may spawn there
     */
    public static boolean canSpawnFromSpawner(EntityLiving npc) {
        World w = npc.worldObj;
        return w.difficultySetting != EnumDifficulty.PEACEFUL && w.checkNoEntityCollision(npc.boundingBox)
            && w.getCollidingBoundingBoxes(npc, npc.boundingBox)
                .isEmpty()
            && !w.isAnyLiquid(npc.boundingBox);
    }
}
