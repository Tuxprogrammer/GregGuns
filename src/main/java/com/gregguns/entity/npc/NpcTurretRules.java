package com.gregguns.entity.npc;

/**
 * The camp turret's constants and target rules, as pure data and logic with no Minecraft types. The camp turret is a
 * stationary member of the {@link NpcFaction#HOSTILE} camp faction: it shoots players who can take damage, player
 * turrets and every NPC of an enemy faction, and never a member of its own faction.
 *
 * <p>
 * Usage: {@code NpcTurretRules.isTarget(false, false, true, NpcFaction.NEUTRAL, false)} is true. Both sides.
 */
// Spec: docs/techguns-spec/10-entities-npcs/per-mob.md (17. Turret), docs/techguns-spec/10-entities-npcs/ai.md
// (1. Factions, 5. Turret), docs/gregguns-spec/10-entities-npcs/index.md
// Deviation: the Techguns NPCTurret is the player turret's head and carries the TURRET faction; here the hostile camp
// turret is its own mob and carries the camp (HOSTILE) faction, so camp NPCs and the camp turret never fight, and the
// player turret fights it as it fights any camp NPC.
public final class NpcTurretRules {

    /** Maximum health. */
    public static final double MAX_HEALTH = 30.0;
    /** Fixed plate armor value; picks the iron sheet and gives vanilla armor math 5 points. */
    public static final int ARMOR_VALUE = 5;
    /** Registry name of the fixed gun it carries. */
    public static final String GUN = "ak47";
    /** Ticks between target scans while it has none. */
    public static final int SCAN_INTERVAL = 10;
    /** Vertical half-range of the target search, in blocks. */
    public static final double VERTICAL_REACH = 20.0;
    /** The faction it belongs to. */
    public static final NpcFaction FACTION = NpcFaction.HOSTILE;

    private NpcTurretRules() {}

    /**
     * @param isPlayer        the candidate is a player
     * @param playerProtected the player's {@code disableDamage} (creative, spectator); ignored for non-players
     * @param hasFaction      the candidate is an NPC with a faction
     * @param faction         the NPC's faction; ignored when {@code hasFaction} is false
     * @param isPlayerTurret  the candidate is a player's turret entity
     * @return true when the camp turret may shoot the candidate
     */
    public static boolean isTarget(boolean isPlayer, boolean playerProtected, boolean hasFaction, NpcFaction faction,
        boolean isPlayerTurret) {
        if (isPlayer) {
            return NpcTargetRules.canSeekPlayer(playerProtected);
        }
        if (hasFaction) {
            return FactionRules.isHostile(FACTION, faction);
        }
        return isPlayerTurret;
    }
}
