package com.gregguns.entity.npc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.entity.boss.IBossDisplayData;

import org.junit.jupiter.api.Test;

/** Stats and persistence of the two mini-bosses (M7 B3), pinned to the Techguns mob catalog. */
class BossStatsTest {

    @Test
    void psychoSteveStatsMatchTheCatalog() {
        assertEquals(75.0, PsychoSteve.MAX_HEALTH);
        assertEquals(7.0, PsychoSteve.ATTACK_DAMAGE);
        assertEquals(0.60, PsychoSteve.SPEED);
        assertEquals(60.0, PsychoSteve.FOLLOW_RANGE);
        assertEquals(25, PsychoSteve.XP);
    }

    @Test
    void dictatorDaveStatsMatchTheCatalog() {
        assertEquals(100.0, DictatorDave.MAX_HEALTH);
        assertEquals(7.0, DictatorDave.ATTACK_DAMAGE);
        assertEquals(0.40, DictatorDave.SPEED);
        assertEquals(60.0, DictatorDave.FOLLOW_RANGE);
        assertEquals(35, DictatorDave.XP);
    }

    @Test
    void doesNotReproduceNpc28_bossesPersist() {
        assertTrue(DespawnRules.isPersistent(NpcMob.PSYCHO_STEVE));
        assertTrue(DespawnRules.isPersistent(NpcMob.DICTATOR_DAVE));
        assertFalse(DespawnRules.isPersistent(NpcMob.ZOMBIE_SOLDIER));
    }

    @Test
    void doesNotReproduceNpc22_onlySteveIsABossBarEntity() {
        assertTrue(IBossDisplayData.class.isAssignableFrom(PsychoSteve.class));
        assertFalse(IBossDisplayData.class.isAssignableFrom(DictatorDave.class));
    }
}
