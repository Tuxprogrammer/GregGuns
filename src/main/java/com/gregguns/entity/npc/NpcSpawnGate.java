package com.gregguns.entity.npc;

import com.gregguns.core.config.NpcConfig;

/**
 * The {@code npcs} config flags that switch a mob kind off: {@code miniBosses}, {@code mutants}, {@code alienBug} and
 * {@code cyberdemon}. A kind whose flag is off is refused by every way of placing it: spawn eggs, camp spawns and the
 * natural-spawn check ({@code getCanSpawnHere}, which vanilla mob spawners also ask). Mobs already in the world are
 * left alone. The flags for zombies, military and bandits gate the natural-spawn table only
 * ({@link com.gregguns.entity.npc.spawn.SpawnTable}); the helicopter flag gates {@link NpcCampPort}.
 *
 * <p>
 * Usage: {@code NpcSpawnGate.allowed(mob)}. Server side; the flag is read live, so a config reload applies at once.
 */
// Spec: docs/gregguns-spec/10-entities-npcs/spawning.md, docs/design/0005-world-presence.md
public final class NpcSpawnGate {

    private NpcSpawnGate() {}

    /**
     * @param mob a kind; may be null
     * @return whether the current config lets that kind be placed
     */
    public static boolean allowed(NpcMob mob) {
        return allowed(mob, NpcConfig.miniBosses, NpcConfig.mutants, NpcConfig.alienBug, NpcConfig.cyberdemon);
    }

    /**
     * @param mob        a kind; may be null
     * @param miniBosses the {@code miniBosses} flag
     * @param mutants    the {@code mutants} flag
     * @param alienBug   the {@code alienBug} flag
     * @param cyberdemon the {@code cyberdemon} flag
     * @return false only for a kind whose own flag is off
     */
    public static boolean allowed(NpcMob mob, boolean miniBosses, boolean mutants, boolean alienBug,
        boolean cyberdemon) {
        if (mob == null) {
            return true;
        }
        switch (mob) {
            case PSYCHO_STEVE:
            case DICTATOR_DAVE:
                return miniBosses;
            case SUPER_MUTANT_BASIC:
            case SUPER_MUTANT_HEAVY:
            case SUPER_MUTANT_ELITE:
                return mutants;
            case ALIEN_BUG:
                return alienBug;
            case CYBER_DEMON:
                return cyberdemon;
            default:
                return true;
        }
    }
}
