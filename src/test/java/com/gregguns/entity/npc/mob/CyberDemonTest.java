package com.gregguns.entity.npc.mob;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.gregguns.api.Tier;
import com.gregguns.entity.npc.NpcMob;
import com.gregguns.entity.npc.loot.NpcProfile;
import com.gregguns.entity.npc.loot.NpcProfiles;

/** The Cyberdemon's stats, spawn gate and profile (M7 B4). */
class CyberDemonTest {

    @Test
    void statsMatchTheTechgunsSheet() {
        assertEquals(30.0, CyberDemon.HEALTH);
        assertEquals(7.0, CyberDemon.ATTACK);
        assertEquals(0.35, CyberDemon.SPEED);
        assertEquals(50.0, CyberDemon.FOLLOW_RANGE);
    }

    @Test
    void spawnsOnlyInTheNetherWithTheFlagOn() {
        int[] nether = { -1 };
        assertTrue(CyberDemonRules.canSpawnAt(-1, nether, true));
        assertFalse(CyberDemonRules.canSpawnAt(-1, nether, false));
        assertFalse(CyberDemonRules.canSpawnAt(0, nether, true));
        assertFalse(CyberDemonRules.canSpawnAt(1, nether, true));
        assertTrue(CyberDemonRules.canSpawnAt(7, new int[] { -1, 7 }, true), "a listed Nether-like dimension");
        assertFalse(CyberDemonRules.canSpawnAt(-1, null, true));
    }

    @Test
    void profileCarriesTheNetherBlasterOnly() {
        NpcProfile p = NpcProfiles.get(NpcMob.CYBER_DEMON.registryName());
        assertNotNull(p);
        assertEquals(Tier.LUV, p.ceiling());
        assertEquals(
            1,
            p.equipment()
                .weapons()
                .size());
        assertEquals(
            "nether_blaster",
            p.equipment()
                .weapons()
                .get(0)
                .gun());
        assertTrue(
            p.equipment()
                .armor()
                .isEmpty());
        assertEquals(
            20,
            p.table()
                .slots()
                .size());
    }
}
