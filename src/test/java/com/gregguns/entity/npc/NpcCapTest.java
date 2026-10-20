package com.gregguns.entity.npc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

/** Despawn rules, name matching and caps (M7 N8). */
class NpcCapTest {

    private static final List<String> NAMES = Arrays.asList(
        "gregguns.ArmySoldier",
        "gregguns.Commando",
        "gregguns.ArmySoldier",
        "Zombie",
        null,
        "Techguns.ArmySoldier");

    @Test
    void doesNotReproduceNpc17_shortAndFullNamesBothMatch() {
        assertEquals(2, NpcCap.count(NAMES, Collections.singletonList("ArmySoldier")));
        assertEquals(2, NpcCap.count(NAMES, Collections.singletonList("gregguns.ArmySoldier")));
        assertEquals(3, NpcCap.count(NAMES, Arrays.asList("ArmySoldier", "gregguns.Commando")));
    }

    @Test
    void emptyWantedListCountsEveryGregGunsNpcOnly() {
        assertEquals(3, NpcCap.count(NAMES, null));
        assertEquals(3, NpcCap.count(NAMES, Collections.<String>emptyList()));
    }

    @Test
    void byNameResolvesBothSpellingsAndRejectsOthers() {
        assertSame(NpcMob.BANDIT, NpcMob.byName("Bandit"));
        assertSame(NpcMob.BANDIT, NpcMob.byName("gregguns.Bandit"));
        assertNull(NpcMob.byName("Techguns.Bandit"));
        assertNull(NpcMob.byName("bandit"));
        assertNull(NpcMob.byName(null));
    }

    @Test
    void capsAreStrict() {
        assertTrue(NpcCap.allows(3, 7, 4, 8));
        assertFalse(NpcCap.allows(4, 0, 4, 8));
        assertFalse(NpcCap.allows(0, 8, 4, 8));
        assertFalse(NpcCap.allows(0, 0, 0, 8));
    }

    @Test
    void doesNotReproduceNpc28_bossesAndHelicopterPersist() {
        assertTrue(DespawnRules.isPersistent(NpcMob.PSYCHO_STEVE));
        assertTrue(DespawnRules.isPersistent(NpcMob.DICTATOR_DAVE));
        assertTrue(DespawnRules.isPersistent(NpcMob.ATTACK_HELICOPTER));
        for (NpcMob mob : NpcMob.values()) {
            boolean boss = mob == NpcMob.PSYCHO_STEVE || mob == NpcMob.DICTATOR_DAVE || mob == NpcMob.ATTACK_HELICOPTER;
            assertEquals(!boss, DespawnRules.followsVanillaDespawn(mob), mob.name());
        }
        assertFalse(DespawnRules.isPersistent(null));
    }
}
