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
                        && spawnsMarked(((TileEntityMobSpawner) t).func_145881_a(), name)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * @param logic a spawner's logic
     * @param name  an entity name
     * @return true when the spawner's current entry is that entity and carries GregGuns {@link SpawnerData}
     */
    public static boolean spawnsMarked(MobSpawnerBaseLogic logic, String name) {
        MobSpawnerBaseLogic.WeightedRandomMinecart entry = logic.getRandomEntity();
        if (entry == null || !name.equals(logic.getEntityNameToSpawn())) {
            return false;
        }
        NBTTagCompound data = entry.field_98222_b;
        return SpawnerData.isFromSpawner(data);
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
