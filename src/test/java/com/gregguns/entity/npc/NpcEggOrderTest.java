package com.gregguns.entity.npc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.Test;

class NpcEggOrderTest {

    @Test
    void everyMobIsPlacedInTheRoster() {
        assertEquals(new HashSet<>(java.util.Arrays.asList(NpcMob.values())), new HashSet<>(NpcEggOrder.ROSTER));
        assertEquals(NpcMob.values().length, NpcEggOrder.ROSTER.size(), "a mob is listed twice");
    }

    @Test
    void allListsEveryMobOnce() {
        List<NpcMob> all = NpcEggOrder.all();
        assertEquals(NpcMob.values().length, all.size());
        assertEquals(NpcMob.values().length, new HashSet<>(all).size());
    }

    @Test
    void familiesStayTogetherAndClimb() {
        List<NpcMob> all = NpcEggOrder.all();
        assertTrue(all.indexOf(NpcMob.ZOMBIE_SOLDIER) < all.indexOf(NpcMob.SKELETON_SOLDIER));
        assertTrue(all.indexOf(NpcMob.BANDIT) < all.indexOf(NpcMob.ARMY_SOLDIER));
        assertEquals(all.indexOf(NpcMob.SUPER_MUTANT_BASIC) + 1, all.indexOf(NpcMob.SUPER_MUTANT_HEAVY));
        assertEquals(all.indexOf(NpcMob.SUPER_MUTANT_HEAVY) + 1, all.indexOf(NpcMob.SUPER_MUTANT_ELITE));
        assertEquals(NpcMob.NPC_TEST, all.get(all.size() - 1));
    }

    @Test
    void idsAreUntouched() {
        assertEquals(61, NpcMob.ZOMBIE_SOLDIER.entityId());
        assertEquals(66, NpcMob.ALIEN_BUG.entityId());
    }
}
