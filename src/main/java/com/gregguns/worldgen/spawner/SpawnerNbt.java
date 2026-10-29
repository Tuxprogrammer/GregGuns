package com.gregguns.worldgen.spawner;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import com.gregguns.entity.npc.spawn.SpawnerData;

/**
 * Builds and reads the tile NBT of a GregGuns vanilla mob spawner: the {@code MobSpawnerBaseLogic} fields from a
 * {@link SpawnerSettings}, one {@code SpawnPotentials} entry per mob (weight 1 each, so the pick is even), and the
 * GregGuns {@link SpawnerData} in every entry's {@code Properties}, carrying the danger floor and the block the
 * spawner is placed at (its origin). {@code EntityId} and {@code SpawnData} name the first entry, so even the first
 * cycle takes the {@code SpawnData} path. Pure apart from the NBT classes.
 *
 * <p>
 * Usage: {@code new TileEntityMobSpawner().readFromNBT(SpawnerNbt.build(names, first, settings, minDanger, x, y, z))}.
 */
// Spec: docs/gregguns-spec/11-worldgen/structure-placement.md (spawner marks)
// Source: Minecraft 1.7.10 MobSpawnerBaseLogic#readFromNBT (field names and types: shorts, EntityId string,
// SpawnPotentials list of {Type, Weight, Properties}, SpawnData compound) and WeightedRandomMinecart(NBTTagCompound)
public final class SpawnerNbt {

    /** The spawner tile id vanilla registers. */
    public static final String TILE_ID = "MobSpawner";

    private static final int COMPOUND = 10;

    private SpawnerNbt() {}

    /**
     * @param entityNames the entity names the spawner spawns, at least one
     * @param first       the index of the entry the first cycle uses
     * @param settings    the spawner settings
     * @param minDanger   the danger floor 0 to 5 carried to each NPC
     * @param x           world X of the spawner block, written as the tile's position and its origin
     * @param y           world Y
     * @param z           world Z
     * @return the tile compound
     * @throws IllegalArgumentException when no entity is named
     */
    public static NBTTagCompound build(List<String> entityNames, int first, SpawnerSettings settings, int minDanger,
        int x, int y, int z) {
        if (entityNames.isEmpty()) {
            throw new IllegalArgumentException("a spawner needs at least one entity");
        }
        String head = entityNames.get(Math.floorMod(first, entityNames.size()));
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("id", TILE_ID);
        tag.setInteger("x", x);
        tag.setInteger("y", y);
        tag.setInteger("z", z);
        tag.setString("EntityId", head);
        tag.setShort("Delay", (short) settings.firstDelay());
        tag.setShort("MinSpawnDelay", (short) settings.minDelay());
        tag.setShort("MaxSpawnDelay", (short) settings.maxDelay());
        tag.setShort("SpawnCount", (short) settings.spawnCount());
        tag.setShort("MaxNearbyEntities", (short) settings.maxNearby());
        tag.setShort("RequiredPlayerRange", (short) settings.playerRange());
        tag.setShort("SpawnRange", (short) settings.spawnRange());
        NBTTagList potentials = new NBTTagList();
        for (String name : entityNames) {
            NBTTagCompound entry = new NBTTagCompound();
            entry.setString("Type", name);
            entry.setInteger("Weight", 1);
            entry.setTag("Properties", SpawnerData.spawnData(minDanger, x, y, z));
            potentials.appendTag(entry);
        }
        tag.setTag("SpawnPotentials", potentials);
        tag.setTag("SpawnData", SpawnerData.spawnData(minDanger, x, y, z));
        return tag;
    }

    /**
     * @param tile a spawner tile compound, as {@code TileEntityMobSpawner#writeToNBT} writes it
     * @return the entity names of its {@code SpawnPotentials}, in order; empty when it has none
     */
    public static List<String> entities(NBTTagCompound tile) {
        List<String> out = new ArrayList<>();
        NBTTagList list = tile.getTagList("SpawnPotentials", COMPOUND);
        for (int i = 0; i < list.tagCount(); i++) {
            out.add(
                list.getCompoundTagAt(i)
                    .getString("Type"));
        }
        return out;
    }

    /**
     * @param tile a spawner tile compound
     * @return true when every {@code SpawnPotentials} entry and the {@code SpawnData} carry GregGuns spawner data
     */
    public static boolean isMarked(NBTTagCompound tile) {
        NBTTagList list = tile.getTagList("SpawnPotentials", COMPOUND);
        if (list.tagCount() == 0 || !SpawnerData.isFromSpawner(tile.getCompoundTag("SpawnData"))) {
            return false;
        }
        for (int i = 0; i < list.tagCount(); i++) {
            if (!SpawnerData.isFromSpawner(
                list.getCompoundTagAt(i)
                    .getCompoundTag("Properties"))) {
                return false;
            }
        }
        return true;
    }

    /**
     * @param tile a spawner tile compound
     * @return the danger floor its {@code SpawnData} carries, or 0
     */
    public static int minDanger(NBTTagCompound tile) {
        return SpawnerData.minDanger(tile.getCompoundTag("SpawnData"));
    }
}
