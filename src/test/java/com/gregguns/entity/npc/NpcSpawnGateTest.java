package com.gregguns.entity.npc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.gregguns.core.config.NpcConfig;

/** The npcs.* kind flags that refuse eggs, camps and spawners. */
class NpcSpawnGateTest {

    @Test
    void everyKindIsOpenWithAllFlagsOn() {
        for (NpcMob mob : NpcMob.values()) {
            assertTrue(NpcSpawnGate.allowed(mob, true, true, true, true), mob.name());
        }
        assertTrue(NpcSpawnGate.allowed(null, false, false, false, false));
    }

    @Test
    void alienBugFlagClosesOnlyTheBug() {
        for (NpcMob mob : NpcMob.values()) {
            assertTrue(NpcSpawnGate.allowed(mob, true, true, false, true) == (mob != NpcMob.ALIEN_BUG), mob.name());
        }
    }

    @Test
    void mutantFlagClosesTheThreeMutants() {
        for (NpcMob mob : NpcMob.values()) {
            boolean mutant = mob == NpcMob.SUPER_MUTANT_BASIC || mob == NpcMob.SUPER_MUTANT_HEAVY
                || mob == NpcMob.SUPER_MUTANT_ELITE;
            assertTrue(NpcSpawnGate.allowed(mob, true, false, true, true) == !mutant, mob.name());
        }
    }

    @Test
    void miniBossFlagClosesPsychoSteveAndDictatorDave() {
        assertFalse(NpcSpawnGate.allowed(NpcMob.PSYCHO_STEVE, false, true, true, true));
        assertFalse(NpcSpawnGate.allowed(NpcMob.DICTATOR_DAVE, false, true, true, true));
        assertTrue(NpcSpawnGate.allowed(NpcMob.BANDIT, false, true, true, true));
    }

    @Test
    void cyberdemonFlagClosesTheCyberdemon() {
        assertFalse(NpcSpawnGate.allowed(NpcMob.CYBER_DEMON, true, true, true, false));
        assertTrue(NpcSpawnGate.allowed(NpcMob.ALIEN_BUG, true, true, true, false));
    }

    @Test
    void theConfigOverloadReadsTheLiveFlags() {
        boolean was = NpcConfig.alienBug;
        try {
            NpcConfig.alienBug = false;
            assertFalse(NpcSpawnGate.allowed(NpcMob.ALIEN_BUG));
            NpcConfig.alienBug = true;
            assertTrue(NpcSpawnGate.allowed(NpcMob.ALIEN_BUG));
        } finally {
            NpcConfig.alienBug = was;
        }
    }
}
