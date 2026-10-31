package com.gregguns.core.module;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.gregguns.compat.ModIds;

class ModPresenceTest {

    @Test
    void onlyIcbmNeedsAModAndItIsGalacticraft() {
        for (ModuleId id : ModuleId.values()) {
            String[] expected = id == ModuleId.ICBM ? new String[] { ModIds.GALACTICRAFT_CORE } : new String[0];
            assertEquals(Arrays.asList(expected), Arrays.asList(id.requiredMods()), id.name());
        }
    }

    @Test
    void aProbeThatThrowsAnswersFalseAndIsNotCached() {
        // D344 (R2-16): a failed probe must not stick, or a real game would keep the module off for good.
        String mod = "gregguns_test_probe_" + System.nanoTime();
        assertFalse(ModPresence.isLoaded(mod, id -> { throw new IllegalStateException("no mod list"); }));
        assertTrue(ModPresence.isLoaded(mod, id -> true));
        assertTrue(ModPresence.isLoaded(mod, id -> false), "a real answer is cached");
    }

    @Test
    void missingModsListsTheUnloadedOnes() {
        assertEquals(Arrays.asList("GalacticraftCore"), ModPresence.missingMods(ModuleId.ICBM, mod -> false));
        assertTrue(
            ModPresence.missingMods(ModuleId.ICBM, mod -> true)
                .isEmpty());
        assertEquals(Collections.emptyList(), ModPresence.missingMods(ModuleId.GUNS, mod -> false));
    }

    @Test
    void stateAndMaskHonorTheModProbe() {
        assertTrue(ModuleState.isActiveLocally(ModuleId.ICBM, id -> true, mod -> true));
        assertEquals(false, ModuleState.isActiveLocally(ModuleId.ICBM, id -> true, mod -> false));
        int without = ModuleState.maskOf(id -> true, mod -> false);
        assertEquals(
            false,
            ModuleState.idsOf(without)
                .contains(ModuleId.ICBM));
        assertTrue(
            ModuleState.idsOf(without)
                .contains(ModuleId.MACHINES));
        assertTrue(
            ModuleState.idsOf(ModuleState.maskOf(id -> true, mod -> true))
                .contains(ModuleId.ICBM));
    }
}
