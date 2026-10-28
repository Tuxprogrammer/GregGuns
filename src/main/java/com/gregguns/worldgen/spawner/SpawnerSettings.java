package com.gregguns.worldgen.spawner;

/**
 * The settings GregGuns writes into a vanilla mob spawner ({@code minecraft:mob_spawner}), one value per
 * {@code MobSpawnerBaseLogic} NBT field, and a pure model of what vanilla does with them. Every {@link SpawnerKind}
 * uses {@link #STANDARD}: a player within {@link #playerRange()} blocks wakes it; each cycle makes
 * {@link #spawnCount()} tries; a try stops the cycle once {@link #maxNearby()} mobs of the picked type stand in the
 * count box; cycles are {@link #minDelay()} to {@link #maxDelay()} ticks apart. Pure: no Minecraft types.
 *
 * <p>
 * Vanilla counts per mob type (the entity class it is about to spawn), in a box of {@code 2 * spawnRange} blocks
 * horizontally and 4 blocks vertically around the spawner, and picks one weighted type per cycle. So "5 nearby" is
 * per type: a Zombie spawner can hold up to 5 of each of its three zombies.
 */
// Spec: docs/gregguns-spec/11-worldgen/structure-placement.md (spawner marks), PLAN-M8 section 2 ("5 near, 3 per try")
// Source: Minecraft 1.7.10 MobSpawnerBaseLogic#updateSpawner, #readFromNBT and #resetTimer (MCP-decompiled with
// Forge 10.13.4.1614; Forge does not patch this class)
public final class SpawnerSettings {

    /** The settings of every GregGuns spawner: 24 blocks, 5 near, 3 per cycle, every 5 to 6 seconds. */
    public static final SpawnerSettings STANDARD = new SpawnerSettings(3, 4, 100, 120, 5, 24, 40);

    private final int spawnCount;
    private final int spawnRange;
    private final int minDelay;
    private final int maxDelay;
    private final int maxNearby;
    private final int playerRange;
    private final int firstDelay;

    /**
     * @param spawnCount  tries per cycle ({@code SpawnCount})
     * @param spawnRange  horizontal spread of the spawn positions, in blocks ({@code SpawnRange})
     * @param minDelay    shortest gap between cycles, in ticks ({@code MinSpawnDelay})
     * @param maxDelay    longest gap between cycles, in ticks ({@code MaxSpawnDelay})
     * @param maxNearby   mobs of one type in the count box that stop a cycle ({@code MaxNearbyEntities})
     * @param playerRange a player must be closer than this, in blocks ({@code RequiredPlayerRange})
     * @param firstDelay  ticks, counted only while a player is in range, before the first cycle ({@code Delay})
     */
    public SpawnerSettings(int spawnCount, int spawnRange, int minDelay, int maxDelay, int maxNearby, int playerRange,
        int firstDelay) {
        this.spawnCount = spawnCount;
        this.spawnRange = spawnRange;
        this.minDelay = minDelay;
        this.maxDelay = maxDelay;
        this.maxNearby = maxNearby;
        this.playerRange = playerRange;
        this.firstDelay = firstDelay;
    }

    /** @return tries per cycle */
    public int spawnCount() {
        return spawnCount;
    }

    /** @return horizontal spread of the spawn positions, in blocks */
    public int spawnRange() {
        return spawnRange;
    }

    /** @return shortest gap between cycles, in ticks */
    public int minDelay() {
        return minDelay;
    }

    /** @return longest gap between cycles, in ticks */
    public int maxDelay() {
        return maxDelay;
    }

    /** @return mobs of one type near the spawner that stop a cycle */
    public int maxNearby() {
        return maxNearby;
    }

    /** @return the wake range, in blocks */
    public int playerRange() {
        return playerRange;
    }

    /** @return ticks before the first cycle */
    public int firstDelay() {
        return firstDelay;
    }

    /** @return the horizontal half-width of the box vanilla counts nearby mobs in: twice the spawn range */
    public int countRadius() {
        return 2 * spawnRange;
    }

    /**
     * Vanilla's cycle, modeled: each try counts first and ends the cycle at the cap, so a cycle places at most
     * {@link #spawnCount()} and never takes a type past {@link #maxNearby()}. A try can also fail its spot check, so
     * this is the most one cycle can place.
     *
     * @param nearby mobs of the cycle's type already in the count box
     * @return the most this cycle may place
     */
    public int mostThisCycle(int nearby) {
        return Math.max(0, Math.min(spawnCount, maxNearby - nearby));
    }

    /**
     * @param distanceSq squared distance from the spawner's center to the closest player
     * @return true when that player wakes the spawner (vanilla compares strictly)
     */
    public boolean playerInRange(double distanceSq) {
        return distanceSq < (double) playerRange * playerRange;
    }
}
