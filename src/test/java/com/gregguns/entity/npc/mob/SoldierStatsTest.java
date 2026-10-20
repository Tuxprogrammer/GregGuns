package com.gregguns.entity.npc.mob;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.gregguns.entity.npc.NpcMob;

/** The Army Soldier's and Commando's stats against the mob table (M7 B2). */
// Spec: docs/techguns-spec/10-entities-npcs/per-mob.md (Army Soldier, Commando)
class SoldierStatsTest {

    @Test
    void armySoldierMatchesTheMobTable() {
        assertEquals(30.0, ArmySoldier.HEALTH);
        assertEquals(4.0, ArmySoldier.MELEE_DAMAGE);
        assertEquals(0.30, ArmySoldier.SPEED);
        assertEquals(75.0, ArmySoldier.FOLLOW_RANGE);
        assertTrue(NpcMob.ARMY_SOLDIER.hasEgg());
    }

    @Test
    void commandoMatchesTheMobTable() {
        assertEquals(30.0, Commando.HEALTH);
        assertEquals(5.0, Commando.MELEE_DAMAGE);
        assertEquals(0.35, Commando.SPEED);
        assertEquals(75.0, Commando.FOLLOW_RANGE);
        assertTrue(NpcMob.COMMANDO.hasEgg());
    }
}
