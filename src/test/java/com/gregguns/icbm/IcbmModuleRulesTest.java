package com.gregguns.icbm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import com.gregguns.core.config.ModuleFlags;
import com.gregguns.core.module.ModuleId;
import com.gregguns.core.module.ModuleState;

class IcbmModuleRulesTest {

    @Test
    void theModuleIsOffByDefaultAndKeyedIcbm() {
        assertEquals("icbm", ModuleId.ICBM.configKey());
        assertFalse(ModuleFlags.isEnabled(ModuleId.ICBM));
    }

    @Test
    void theModuleNeedsGunsMachinesAndGalacticraft() {
        Set<ModuleId> need = EnumSet.copyOf(Arrays.asList(ModuleId.ICBM.requires()));
        assertEquals(EnumSet.of(ModuleId.GUNS, ModuleId.MACHINES), need);
        assertEquals(Arrays.asList("GalacticraftCore"), Arrays.asList(ModuleId.ICBM.requiredMods()));
        Predicate<ModuleId> onlyIcbmAndGuns = id -> id == ModuleId.ICBM || id == ModuleId.GUNS;
        assertFalse(ModuleState.isActiveLocally(ModuleId.ICBM, onlyIcbmAndGuns, mod -> true));
        assertTrue(ModuleState.isActiveLocally(ModuleId.ICBM, id -> true, mod -> true));
        assertFalse(ModuleState.isActiveLocally(ModuleId.ICBM, id -> true, mod -> false));
    }
}
