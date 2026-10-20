package com.gregguns.entity.npc;

/**
 * Which NPCs persist (never despawn) and which follow the vanilla distance rules. Pure: no Minecraft types. The mini
 * bosses (Psycho Steve, Dictator Dave) and the Attack Helicopter persist; every other NPC is an ordinary monster that
 * vanilla removes when no player is near (instantly beyond 128 blocks, randomly beyond 32), so camp and natural spawns
 * do not pile up. Techguns let the bosses vanish when the player walked away (NPC-28).
 *
 * <p>
 * Usage: {@code DespawnRules.isPersistent(NpcMob.PSYCHO_STEVE)} is true; {@code NpcEntity.canDespawn} returns its
 * negation. Both sides, though only the server despawns.
 */
// Spec: docs/techguns-spec/10-entities-npcs/index.md (NPC-28), docs/gregguns-spec/10-entities-npcs/spawning.md
public final class DespawnRules {

    private DespawnRules() {}

    /**
     * @param mob an NPC kind; may be null for an unknown entity
     * @return true if that kind never despawns by distance
     */
    public static boolean isPersistent(NpcMob mob) {
        if (mob == null) {
            return false;
        }
        switch (mob) {
            case PSYCHO_STEVE:
            case DICTATOR_DAVE:
            case ATTACK_HELICOPTER:
                return true;
            default:
                return false;
        }
    }

    /**
     * @param mob an NPC kind; may be null
     * @return true if vanilla distance despawning applies (the negation of {@link #isPersistent})
     */
    public static boolean followsVanillaDespawn(NpcMob mob) {
        return !isPersistent(mob);
    }
}
