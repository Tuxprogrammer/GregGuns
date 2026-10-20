package com.gregguns.entity.npc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.gregguns.entity.npc.loot.LootDrop;
import com.gregguns.entity.npc.loot.NpcProfile;
import com.gregguns.entity.npc.loot.NpcProfiles;

/** The Alien Bug's catalog stats, registration data and loot profile (M7 B4). */
class AlienBugTest {

    @Test
    void statsMatchTheCatalog() {
        assertEquals(20.0, AlienBug.MAX_HEALTH);
        assertEquals(4.0, AlienBug.ATTACK_DAMAGE);
        assertEquals(1.0, AlienBug.SPEED);
        assertEquals(16.0, AlienBug.FOLLOW_RANGE);
        assertEquals(1.1F, AlienBug.WIDTH);
        assertEquals(1.2F, AlienBug.HEIGHT);
        assertEquals(10, AlienBug.BITE_TICKS);
    }

    @Test
    void registrationDataMatchesTheCatalog() {
        assertEquals("gregguns.AlienBug", NpcMob.ALIEN_BUG.fullName());
        assertEquals(66, NpcMob.ALIEN_BUG.entityId());
        assertEquals(0xC6A96B, NpcMob.ALIEN_BUG.eggBase());
        assertEquals(0x71552E, NpcMob.ALIEN_BUG.eggSpot());
    }

    @Test
    void bugDoesNotPersist() {
        assertFalse(DespawnRules.isPersistent(NpcMob.ALIEN_BUG));
    }

    @Test
    void profileIsSpiderLikeAndCarriesNothing() {
        NpcProfile p = NpcProfiles.get("AlienBug");
        assertNotNull(p);
        assertNull(p.equipment());
        assertFalse(p.rareDrop());
        assertTrue(
            p.fixedDrops()
                .isEmpty());
        assertNotNull(p.table());
        for (LootDrop d : p.table()
            .slots()) {
            assertTrue(
                d.ref()
                    .equals("minecraft:string")
                    || d.ref()
                        .equals("minecraft:spider_eye"),
                d.ref());
        }
    }
}
