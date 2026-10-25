package com.gregguns.icbm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import com.gregguns.core.module.ModuleId;
import com.gregguns.core.module.ModuleState;

class IcbmGateTest {

    @Test
    void flagOffIsOffByConfigWhateverGalacticraftDoes() {
        assertEquals(IcbmGate.Verdict.OFF_BY_CONFIG, IcbmGate.decide(false, true));
        assertEquals(IcbmGate.Verdict.OFF_BY_CONFIG, IcbmGate.decide(false, false));
    }

    @Test
    void flagOnWithGalacticraftIsActive() {
        assertEquals(IcbmGate.Verdict.ACTIVE, IcbmGate.decide(true, true));
    }

    @Test
    void flagOnWithoutGalacticraftTurnsItselfOff() {
        assertEquals(IcbmGate.Verdict.OFF_NO_GALACTICRAFT, IcbmGate.decide(true, false));
    }

    @Test
    void onlySelfDisableHasALogLine() {
        assertNull(IcbmGate.logLine(IcbmGate.Verdict.ACTIVE));
        assertNull(IcbmGate.logLine(IcbmGate.Verdict.OFF_BY_CONFIG));
        String line = IcbmGate.logLine(IcbmGate.Verdict.OFF_NO_GALACTICRAFT);
        assertNotNull(line);
        assertTrue(line.contains("Galacticraft"));
        assertFalse(line.contains("\n"), "one log line");
    }

    @Test
    void theModuleIsOffByDefaultAndKeyedIcbm() {
        assertEquals("icbm", ModuleId.ICBM.configKey());
        assertFalse(com.gregguns.core.config.ModuleFlags.isEnabled(ModuleId.ICBM));
    }

    @Test
    void theModuleNeedsGunsAndMachines() {
        Set<ModuleId> need = EnumSet.copyOf(Arrays.asList(ModuleId.ICBM.requires()));
        assertEquals(EnumSet.of(ModuleId.GUNS, ModuleId.MACHINES), need);
        Predicate<ModuleId> onlyIcbmAndGuns = id -> id == ModuleId.ICBM || id == ModuleId.GUNS;
        assertFalse(ModuleState.isActiveLocally(ModuleId.ICBM, onlyIcbmAndGuns));
        assertTrue(ModuleState.isActiveLocally(ModuleId.ICBM, id -> true));
    }
}
