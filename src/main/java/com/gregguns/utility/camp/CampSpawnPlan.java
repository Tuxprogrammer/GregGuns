package com.gregguns.utility.camp;

import java.util.function.IntSupplier;

/**
 * The Camp Flag's spawn clock as plain Java: a count check every 60 ticks and a spawn timer that runs while the camp is
 * under its cap. The caller owns the world; this class only says when to spawn. No wall clock.
 *
 * <p>
 * Usage: call {@link #tick} once per server tick with a way to count the camp's living NPCs; spawn when it returns
 * true.
 */
// Spec: docs/techguns-spec/09-machines-utility/misc-tile-entities.md section 2 (Spawning); fixes MUTL-38, MUTL-39
public final class CampSpawnPlan {

    /** Ticks between two NPC counts. */
    public static final int CHECK_INTERVAL = 60;

    private final int spawnInterval;
    private int checkTimer;
    private int spawnTimer;
    private boolean enabled;

    /**
     * @param spawnInterval ticks between spawns while the camp is below its cap
     */
    public CampSpawnPlan(int spawnInterval) {
        this.spawnInterval = Math.max(1, spawnInterval);
        this.spawnTimer = this.spawnInterval;
    }

    /**
     * The NPC cap of a camp.
     *
     * @param sizeX  camp size on X
     * @param sizeZ  camp size on Z
     * @param factor the multiplier, 0.75 by default
     * @return int(((sizeX + sizeZ) / 2) * factor), never negative
     */
    public static int defaultCap(int sizeX, int sizeZ, float factor) {
        return Math.max(0, (int) (((sizeX + sizeZ) / 2.0f) * factor));
    }

    /**
     * Advances one tick.
     *
     * @param initialized whether the flag was initialized; an uninitialized flag never counts or spawns (MUTL-39)
     * @param maxNpcs     the cap
     * @param spawnPoints how many spawn points the flag has
     * @param npcNames    how many NPC names the flag has
     * @param counter     counts the camp's living NPCs; called at most once per {@link #CHECK_INTERVAL} ticks
     * @return true when one NPC should be spawned now
     */
    public boolean tick(boolean initialized, int maxNpcs, int spawnPoints, int npcNames, IntSupplier counter) {
        if (!initialized) {
            return false;
        }
        if (--checkTimer <= 0) {
            checkTimer = CHECK_INTERVAL;
            enabled = counter.getAsInt() < maxNpcs;
            if (!enabled) {
                spawnTimer = spawnInterval;
            }
        }
        if (!enabled || spawnPoints <= 0 || npcNames <= 0) {
            return false;
        }
        if (--spawnTimer <= 0) {
            spawnTimer = spawnInterval;
            return true;
        }
        return false;
    }

    /** @return whether spawning is currently enabled (the last count was below the cap) */
    public boolean enabled() {
        return enabled;
    }
}
