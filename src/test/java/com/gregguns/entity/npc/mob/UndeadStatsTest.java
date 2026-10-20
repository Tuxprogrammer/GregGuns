package com.gregguns.entity.npc.mob;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.gregguns.entity.npc.NpcMob;

/** Stats of the Skeleton Soldier and Zombie Pigman Soldier against the mob table (PLAN-M7 section 2). */
class UndeadStatsTest {

    @Test
    void skeletonSoldierStats() {
        assertEquals(25.0, SkeletonSoldier.HEALTH);
        assertEquals(4.0, SkeletonSoldier.ATTACK);
        assertEquals(0.25, SkeletonSoldier.SPEED);
        assertEquals(50.0, SkeletonSoldier.FOLLOW);
    }

    @Test
    void pigmanSoldierStats() {
        assertEquals(25.0, ZombiePigmanSoldier.HEALTH);
        assertEquals(5.0, ZombiePigmanSoldier.ATTACK);
        assertEquals(0.30, ZombiePigmanSoldier.SPEED);
        assertEquals(50.0, ZombiePigmanSoldier.FOLLOW);
    }

    @Test
    void profileKeysMatchRegistryNames() {
        assertEquals(NpcMob.SKELETON_SOLDIER.registryName(), SkeletonSoldier.PROFILE);
        assertEquals(NpcMob.ZOMBIE_PIGMAN_SOLDIER.registryName(), ZombiePigmanSoldier.PROFILE);
        assertTrue(NpcMob.SKELETON_SOLDIER.hasEgg());
        assertTrue(NpcMob.ZOMBIE_PIGMAN_SOLDIER.hasEgg());
    }
}
