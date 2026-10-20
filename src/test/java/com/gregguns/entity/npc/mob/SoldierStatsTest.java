package com.gregguns.entity.npc.mob;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.gregguns.entity.npc.NpcMob;

/** The Army Soldier's, Commando's, Outcast's and Stormtrooper's stats against the mob table (M7 B2, B5). */
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

    @Test
    void outcastMatchesTheMobTable() {
        assertEquals(30.0, Outcast.HEALTH);
        assertEquals(8.0, Outcast.MELEE_DAMAGE);
        assertEquals(0.20, Outcast.SPEED);
        assertEquals(60.0, Outcast.FOLLOW_RANGE);
        assertTrue(NpcMob.OUTCAST.hasEgg());
        assertEquals(70, NpcMob.OUTCAST.entityId());
    }

    @Test
    void stormTrooperMatchesTheMobTable() {
        assertEquals(35.0, StormTrooper.HEALTH);
        assertEquals(4.0, StormTrooper.MELEE_DAMAGE);
        assertEquals(0.30, StormTrooper.SPEED);
        assertEquals(75.0, StormTrooper.FOLLOW_RANGE);
        assertTrue(NpcMob.STORMTROOPER.hasEgg());
        assertEquals(76, NpcMob.STORMTROOPER.entityId());
    }
}
