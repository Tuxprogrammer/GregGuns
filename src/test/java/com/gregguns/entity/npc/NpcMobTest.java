package com.gregguns.entity.npc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

/** The reserved entity ID plan and egg data (M7 N7). */
class NpcMobTest {

    /** Projectile IDs in use, and the turret (60), which the NPC range must not touch. */
    private static final int[] TAKEN = { 3, 4, 6, 8, 9, 11, 13, 18, 19, 21, 22, 24, 25, 26, 27, 60 };

    @Test
    void idsAreUniqueAndInsideTheReservedRange() {
        Set<Integer> seen = new HashSet<>();
        for (NpcMob mob : NpcMob.values()) {
            assertTrue(mob.entityId() >= NpcMob.FIRST_ID && mob.entityId() <= NpcMob.LAST_ID, mob.name());
            assertTrue(seen.add(mob.entityId()), "duplicate id " + mob.entityId());
        }
    }

    @Test
    void idsDoNotCollideWithProjectilesOrTheTurret() {
        for (NpcMob mob : NpcMob.values()) {
            for (int taken : TAKEN) {
                assertFalse(mob.entityId() == taken, mob.name() + " collides with " + taken);
            }
        }
    }

    @Test
    void testNpcKeepsItsOriginalId() {
        assertEquals(80, NpcMob.NPC_TEST.entityId());
        assertEquals("gregguns.npc_test", NpcMob.NPC_TEST.fullName());
        assertTrue(NpcMob.NPC_TEST.isDevOnly());
    }

    @Test
    void namesAreUniqueAndFollowTheCatalog() {
        Set<String> names = new HashSet<>();
        for (NpcMob mob : NpcMob.values()) {
            assertTrue(names.add(mob.registryName()), "duplicate name " + mob.registryName());
            assertEquals("gregguns." + mob.registryName(), mob.fullName());
            assertEquals("entity.gregguns." + mob.registryName() + ".name", mob.langKey());
        }
        assertEquals("gregguns.ZombieSoldier", NpcMob.ZOMBIE_SOLDIER.fullName());
        assertEquals(61, NpcMob.ZOMBIE_SOLDIER.entityId());
    }

    @Test
    void eggColorsComeFromTheCatalog() {
        assertEquals(0x757468, NpcMob.ZOMBIE_SOLDIER.eggBase());
        assertEquals(0x38B038, NpcMob.ZOMBIE_SOLDIER.eggSpot());
        assertEquals(0xF0F0F0, NpcMob.STORMTROOPER.eggBase());
        assertTrue(NpcMob.BANDIT.hasEgg());
        assertFalse(NpcMob.SIMPLE_TURRET.hasEgg());
    }

    @Test
    void lookupByIdRoundTrips() {
        for (NpcMob mob : NpcMob.values()) {
            assertSame(mob, NpcMob.byId(mob.entityId()));
        }
        assertNull(NpcMob.byId(60));
        assertNull(NpcMob.byId(0));
    }
}
