package com.gregguns.entity.npc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The camp turret's stats and target rules (M7 mob 7). */
class NpcTurretRulesTest {

    @Test
    void statsMatchTheSpec() {
        assertEquals(30.0, NpcTurretRules.MAX_HEALTH);
        assertEquals(5, NpcTurretRules.ARMOR_VALUE);
        assertEquals(NpcFaction.HOSTILE, NpcTurretRules.FACTION);
        assertEquals(72, NpcMob.SIMPLE_TURRET.entityId());
    }

    @Test
    void shootsPlayersWhoCanTakeDamage() {
        assertTrue(NpcTurretRules.isTarget(true, false, false, null, false));
        assertFalse(NpcTurretRules.isTarget(true, true, false, null, false));
    }

    @Test
    void spareItsOwnFactionAndShootsEnemyFactions() {
        assertFalse(NpcTurretRules.isTarget(false, false, true, NpcFaction.HOSTILE, false));
        assertTrue(NpcTurretRules.isTarget(false, false, true, NpcFaction.NEUTRAL, false));
        assertTrue(NpcTurretRules.isTarget(false, false, true, NpcFaction.TURRET, false));
    }

    @Test
    void shootsPlayerTurretsButNotAnimalsOrFactionless() {
        assertTrue(NpcTurretRules.isTarget(false, false, false, null, true));
        assertFalse(NpcTurretRules.isTarget(false, false, false, null, false));
    }
}
