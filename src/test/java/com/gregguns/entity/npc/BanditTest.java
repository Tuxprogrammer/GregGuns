package com.gregguns.entity.npc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The Bandit's stats against the mob table (M7 B1). */
// Spec: docs/techguns-spec/10-entities-npcs/per-mob.md (Bandit)
class BanditTest {

    @Test
    void statsMatchTheMobTable() {
        assertEquals(20.0, Bandit.HEALTH);
        assertEquals(5.0, Bandit.MELEE_DAMAGE);
        assertEquals(0.30, Bandit.SPEED);
        assertEquals(40.0, Bandit.FOLLOW_RANGE);
    }

    @Test
    void banditHasAnEggAndTheReservedId() {
        assertTrue(NpcMob.BANDIT.hasEgg());
        assertEquals(69, NpcMob.BANDIT.entityId());
        assertNotNull(NpcMob.byName("gregguns.Bandit"));
    }
}
