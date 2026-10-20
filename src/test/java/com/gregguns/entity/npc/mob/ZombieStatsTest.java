package com.gregguns.entity.npc.mob;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.gregguns.entity.npc.NpcMob;

/** Stats of the three zombie NPCs against the mob table (PLAN-M7 section 2, mob-catalog.md). */
class ZombieStatsTest {

    @Test
    void soldierStats() {
        assertEquals(25.0, ZombieSoldier.HEALTH);
        assertEquals(4.0, ZombieSoldier.ATTACK);
        assertEquals(0.25, ZombieSoldier.SPEED);
        assertEquals(50.0, ZombieSoldier.FOLLOW);
    }

    @Test
    void farmerStats() {
        assertEquals(18.0, ZombieFarmer.HEALTH);
        assertEquals(3.0, ZombieFarmer.ATTACK);
        assertEquals(0.20, ZombieFarmer.SPEED);
        assertEquals(50.0, ZombieFarmer.FOLLOW);
    }

    @Test
    void minerStats() {
        assertEquals(20.0, ZombieMiner.HEALTH);
        assertEquals(4.0, ZombieMiner.ATTACK);
        assertEquals(0.20, ZombieMiner.SPEED);
        assertEquals(50.0, ZombieMiner.FOLLOW);
    }

    @Test
    void profileKeysMatchRegistryNames() {
        assertEquals(NpcMob.ZOMBIE_SOLDIER.registryName(), ZombieSoldier.PROFILE);
        assertEquals(NpcMob.ZOMBIE_FARMER.registryName(), ZombieFarmer.PROFILE);
        assertEquals(NpcMob.ZOMBIE_MINER.registryName(), ZombieMiner.PROFILE);
        assertTrue(NpcMob.ZOMBIE_MINER.hasEgg());
    }
}
