package com.gregguns.entity.npc;

/**
 * Who an NPC may target, as pure rules. The entity layer adapts living entities to these arguments.
 *
 * <p>
 * Players are the only proactive targets; everything else is a target only in retaliation, and only when the attacker
 * is hostile to the victim.
 */
// Spec: docs/techguns-spec/10-entities-npcs/ai.md (sections 1 and 2), docs/gregguns-spec/10-entities-npcs/spawning.md
// (GGS-10-28)
// Deviation: players with disableDamage (creative, spectator, AngerMod spawn protection) are never targeted
// (GGS-10-28), and melee NPCs may hit non-player attackers (fixes NPC-24).
public final class NpcTargetRules {

    private NpcTargetRules() {}

    /**
     * Proactive target scan: may the NPC seek this player?
     *
     * @param disableDamage the player's {@code capabilities.disableDamage}
     * @return true when the player may be targeted
     */
    public static boolean canSeekPlayer(boolean disableDamage) {
        return !disableDamage;
    }

    /**
     * Retaliation: should a victim set an attacker as its revenge target?
     *
     * @param victim           the victim's faction, or null for none
     * @param attackerIsPlayer true when the shooter is a player; the vanilla revenge path handles players
     * @param attacker         the attacker's faction when it is an NPC, or null for a non-NPC non-player
     * @param attackerIsNpc    true when the attacker implements the NPC faction interface
     * @return true when the victim retaliates
     */
    public static boolean shouldRetaliate(NpcFaction victim, boolean attackerIsPlayer, boolean attackerIsNpc,
        NpcFaction attacker) {
        if (attackerIsPlayer) {
            return true;
        }
        if (attackerIsNpc) {
            return FactionRules.isHostile(victim, attacker);
        }
        return true;
    }

    /**
     * Whether a melee NPC may attack the given entity. Players qualify unless protected; a non-player qualifies only
     * as a revenge target that passed {@link #shouldRetaliate}.
     *
     * @param targetIsPlayer      true when the target is a player
     * @param playerDisableDamage the player's {@code disableDamage}, ignored for non-players
     * @param isRevengeTarget     true when the target is the NPC's current revenge target
     * @return true when the melee task may attack
     */
    public static boolean canMelee(boolean targetIsPlayer, boolean playerDisableDamage, boolean isRevengeTarget) {
        return targetIsPlayer ? !playerDisableDamage : isRevengeTarget;
    }
}
