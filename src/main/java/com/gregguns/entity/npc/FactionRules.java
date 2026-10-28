package com.gregguns.entity.npc;

/**
 * The symmetric faction hostility table. Members of the same faction never fight; HOSTILE fights TURRET and NEUTRAL;
 * TURRET and NEUTRAL do not fight each other.
 */
// Spec: docs/techguns-spec/10-entities-npcs/ai.md (section 1, Factions)
public final class FactionRules {

    private FactionRules() {}

    /**
     * @param a first faction, or null for an entity with no faction
     * @param b second faction, or null for an entity with no faction
     * @return true when the two factions are hostile to each other; always false when either is null
     */
    public static boolean isHostile(NpcFaction a, NpcFaction b) {
        if (a == null || b == null || a == b) {
            return false;
        }
        return a == NpcFaction.HOSTILE || b == NpcFaction.HOSTILE;
    }

    /**
     * Whether a shot from {@code shooter} passes through {@code target} instead of hitting it: both belong to the same
     * NPC faction, and members of one faction never hurt each other. A guard firing past a comrade at a player leaves
     * the comrade untouched.
     *
     * @param shooter whoever fired, or null
     * @param target  the entity the shot meets
     * @return true when both are {@link HasNpcFaction} of one faction
     */
    // Deviation: Techguns bullets hit any entity on their path, so crowded guards wounded each other.
    public static boolean sparesFriend(Object shooter, Object target) {
        if (!(shooter instanceof HasNpcFaction) || !(target instanceof HasNpcFaction) || shooter == target) {
            return false;
        }
        NpcFaction a = ((HasNpcFaction) shooter).npcFaction();
        return a != null && a == ((HasNpcFaction) target).npcFaction();
    }
}
