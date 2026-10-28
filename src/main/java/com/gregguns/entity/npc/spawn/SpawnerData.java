package com.gregguns.entity.npc.spawn;

import net.minecraft.nbt.NBTTagCompound;

/**
 * The GregGuns part of a vanilla mob spawner's {@code SpawnData}: one compound, {@value #KEY}, that tells an NPC
 * that a GregGuns spawner placed it and the danger it starts at least at ({@value #MIN_DANGER}, from a structure's
 * rolled ceiling). Vanilla copies {@code SpawnData} over the new entity's own NBT and calls {@code readFromNBT} before
 * the
 * entity joins the world, so the NPC reads this key there, rolls its equipment and drops the key (it is never saved
 * back). Pure apart from the NBT classes.
 *
 * <p>
 * Usage (worldgen): {@code SpawnerData.spawnData(minDanger)} as each {@code SpawnPotentials} entry's
 * {@code Properties}. Usage (NPC): {@code if (SpawnerData.isFromSpawner(tag)) setDanger(SpawnerData.danger(place,
 * SpawnerData.minDanger(tag)))}.
 */
// Spec: docs/gregguns-spec/10-entities-npcs/spawning.md (spawner NPCs)
// Source: Minecraft 1.7.10 MobSpawnerBaseLogic#func_98265_a (SpawnData merged into writeToNBTOptional, then
// readFromNBT, then spawnEntityInWorld; onSpawnWithEgg is skipped when SpawnData is set)
public final class SpawnerData {

    /** The compound key in {@code SpawnData}. */
    public static final String KEY = "GGSpawner";
    /** The lowest danger 0 to 5 the NPC starts at, inside {@link #KEY}. */
    public static final String MIN_DANGER = "MinDanger";
    /**
     * How far, in blocks horizontally, an NPC looks for the spawner that is placing it. Vanilla spawns within
     * {@code SpawnRange} of the spawner; every GregGuns spawner uses a range no larger than this.
     */
    public static final int SCAN_RANGE = 4;
    /** Vanilla spawns one block below, level with or one block above the spawner. */
    public static final int SCAN_VERTICAL = 1;
    /** The highest spawn danger. */
    public static final int MAX_DANGER = 5;

    private static final int COMPOUND = 10;

    private SpawnerData() {}

    /**
     * @param minDanger the lowest danger the NPC starts at, 0 to 5
     * @return a fresh {@code SpawnData} compound holding only the GregGuns key
     */
    public static NBTTagCompound spawnData(int minDanger) {
        NBTTagCompound inner = new NBTTagCompound();
        inner.setInteger(MIN_DANGER, clamp(minDanger));
        NBTTagCompound data = new NBTTagCompound();
        data.setTag(KEY, inner);
        return data;
    }

    /**
     * @param tag an entity's NBT, or a {@code SpawnData} compound; may be null
     * @return true when it carries the GregGuns spawner key
     */
    public static boolean isFromSpawner(NBTTagCompound tag) {
        return tag != null && tag.hasKey(KEY, COMPOUND);
    }

    /**
     * @param tag an entity's NBT or a {@code SpawnData} compound
     * @return its minimum danger 0 to 5, or 0 when it has none
     */
    public static int minDanger(NBTTagCompound tag) {
        if (!isFromSpawner(tag)) {
            return 0;
        }
        return clamp(
            tag.getCompoundTag(KEY)
                .getInteger(MIN_DANGER));
    }

    /**
     * @param placeDanger the danger the spawn position gives (distance and biome)
     * @param minDanger   the spawner's floor
     * @return the larger of the two, 0 to 5
     */
    public static int danger(int placeDanger, int minDanger) {
        return clamp(Math.max(placeDanger, minDanger));
    }

    /**
     * @param dx entity X minus spawner X (the block corner, as vanilla uses)
     * @param dy entity Y minus spawner Y
     * @param dz entity Z minus spawner Z
     * @return true when a vanilla spawner at that offset could have placed the entity there
     */
    public static boolean inSpawnBox(double dx, double dy, double dz) {
        return Math.abs(dx) <= SCAN_RANGE && Math.abs(dz) <= SCAN_RANGE && Math.abs(dy) <= SCAN_VERTICAL;
    }

    private static int clamp(int danger) {
        return Math.max(0, Math.min(MAX_DANGER, danger));
    }
}
