package com.gregguns.core.module;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

class ModuleMismatchTest {

    @Test
    void equalSwitchesAgree() {
        int m = ModuleMismatch.mask(Arrays.asList(ModuleId.GUNS, ModuleId.MACHINES));
        assertTrue(
            ModuleMismatch.differences(m, m)
                .isEmpty());
    }

    @Test
    void aClientWithMachinesOffIsReported() {
        int server = ModuleMismatch.mask(Arrays.asList(ModuleId.GUNS, ModuleId.MACHINES));
        int client = ModuleMismatch.mask(Arrays.asList(ModuleId.GUNS));
        List<String> diffs = ModuleMismatch.differences(server, client);
        assertEquals(1, diffs.size());
        assertEquals("modules.machines: server on, client off", diffs.get(0));
    }

    @Test
    void everyMaskFitsTheValidRange() {
        assertTrue(ModuleMismatch.mask(Arrays.asList(ModuleId.values())) <= ModuleMismatch.maxMask());
        assertEquals(ModuleMismatch.mask(Arrays.asList(ModuleId.values())), ModuleMismatch.maxMask());
    }

    @Test
    void summaryNamesEachMismatchedModule() {
        int server = ModuleMismatch.mask(Arrays.asList(ModuleId.GUNS, ModuleId.MACHINES));
        List<String> diffs = ModuleMismatch.differences(server, ModuleMismatch.mask(Arrays.asList(ModuleId.MACHINES)));
        assertEquals("modules.guns: server on, client off", ModuleMismatch.summary(diffs));
        diffs = ModuleMismatch.differences(server, 0);
        assertEquals(
            "modules.guns: server on, client off; modules.machines: server on, client off",
            ModuleMismatch.summary(diffs));
    }
}
