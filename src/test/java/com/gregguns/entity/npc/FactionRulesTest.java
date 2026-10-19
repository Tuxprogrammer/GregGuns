package com.gregguns.entity.npc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FactionRulesTest {

    @Test
    void tableMatchesSpecAndIsSymmetric() {
        boolean[][] expected = { { false, true, true }, { true, false, false }, { true, false, false } };
        NpcFaction[] f = NpcFaction.values();
        assertEquals(3, f.length);
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                assertEquals(expected[i][j], FactionRules.isHostile(f[i], f[j]), f[i] + " vs " + f[j]);
                assertEquals(FactionRules.isHostile(f[i], f[j]), FactionRules.isHostile(f[j], f[i]));
            }
        }
    }

    @Test
    void noFactionIsNeverHostile() {
        assertFalse(FactionRules.isHostile(null, NpcFaction.HOSTILE));
        assertFalse(FactionRules.isHostile(NpcFaction.TURRET, null));
        assertFalse(FactionRules.isHostile(null, null));
    }

    @Test
    void sameFactionNeverFights() {
        for (NpcFaction f : NpcFaction.values()) {
            assertFalse(FactionRules.isHostile(f, f));
        }
    }

    @Test
    void crossfireDoesNotCauseInfighting() {
        assertFalse(NpcTargetRules.shouldRetaliate(NpcFaction.HOSTILE, false, true, NpcFaction.HOSTILE));
    }

    @Test
    void turretShotHostileNpcRetaliates() {
        assertTrue(NpcTargetRules.shouldRetaliate(NpcFaction.HOSTILE, false, true, NpcFaction.TURRET));
    }

    @Test
    void playersAndNonNpcAttackersAreRetaliatedAgainst() {
        assertTrue(NpcTargetRules.shouldRetaliate(NpcFaction.HOSTILE, true, false, null));
        assertTrue(NpcTargetRules.shouldRetaliate(NpcFaction.HOSTILE, false, false, null));
    }

    @Test
    void doesNotReproduceNpc24_meleeHitsNonPlayerRevengeTarget() {
        assertTrue(NpcTargetRules.canMelee(false, false, true));
        assertFalse(NpcTargetRules.canMelee(false, false, false));
    }

    @Test
    void protectedPlayersAreIgnored_GGS_10_28() {
        assertFalse(NpcTargetRules.canSeekPlayer(true));
        assertTrue(NpcTargetRules.canSeekPlayer(false));
        assertFalse(NpcTargetRules.canMelee(true, true, true));
        assertTrue(NpcTargetRules.canMelee(true, false, false));
    }
}
