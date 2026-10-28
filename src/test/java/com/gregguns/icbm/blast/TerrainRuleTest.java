package com.gregguns.icbm.blast;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.gregguns.core.config.ExplosionsConfig;
import com.gregguns.core.config.IcbmConfig;
import com.gregguns.icbm.explosive.BlastShape;

/** D52: only the per-tier switch decides terrain damage. */
// Spec: DECISIONS.md D52
class TerrainRuleTest {

    @AfterEach
    void restore() {
        IcbmConfig.terrainDamageT1 = true;
        IcbmConfig.terrainDamageT2 = true;
    }

    @Test
    void explosionsBlockDamageIsNotConsulted() {
        boolean saved = ExplosionsConfig.blockDamage;
        try {
            ExplosionsConfig.blockDamage = false;
            assertTrue(TerrainRule.breaks(firstSolid(), 1, true, true));
        } finally {
            ExplosionsConfig.blockDamage = saved;
        }
    }

    @Test
    void aLauncherBlastNeedsBothSwitches() {
        // D52, D154: a hand-held launcher warhead follows explosions.blockDamage as well as the tier switch.
        assertFalse(TerrainRule.breaks(firstSolid(), 1, true, true, true, false));
        assertTrue(TerrainRule.breaks(firstSolid(), 1, true, true, true, true));
        assertTrue(TerrainRule.breaks(firstSolid(), 1, true, true, false, false));
        IcbmConfig.terrainDamageT1 = false;
        assertFalse(TerrainRule.breaks(firstSolid(), 1, true, true, true, true));
    }

    @Test
    void onByDefault() {
        assertTrue(BlastShape.values().length > 1);
        for (BlastShape s : BlastShape.values()) {
            assertTrue(TerrainRule.breaks(s, 1, true, true) == (s != BlastShape.NONE));
        }
    }

    @Test
    void theTierSwitchDecidesAndOnlyThatTier() {
        IcbmConfig.terrainDamageT1 = false;
        assertFalse(TerrainRule.breaks(firstSolid(), 1, true, true));
        assertTrue(TerrainRule.breaks(firstSolid(), 2, true, true));
    }

    @Test
    void anNpcBlastNeedsMobGriefing() {
        assertFalse(TerrainRule.breaks(firstSolid(), 1, false, false));
        assertTrue(TerrainRule.breaks(firstSolid(), 1, false, true));
    }

    private static BlastShape firstSolid() {
        for (BlastShape s : BlastShape.values()) {
            if (s != BlastShape.NONE) {
                return s;
            }
        }
        throw new AssertionError();
    }
}
