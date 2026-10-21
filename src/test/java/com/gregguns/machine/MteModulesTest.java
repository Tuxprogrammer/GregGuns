package com.gregguns.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.gregguns.core.module.ModuleId;

class MteModulesTest {

    @Test
    void everyBuiltMachineBelongsToAModule() {
        for (int id : GregGunsMTEIDs.all()) {
            assertNotNull(MteModules.moduleOf(id), "MTE " + id);
        }
    }

    @Test
    void retiredAndForeignIdsBelongToNobody() {
        for (int id : GregGunsMTEIDs.retired()) {
            assertNull(MteModules.moduleOf(id), "retired MTE " + id);
        }
        assertNull(MteModules.moduleOf(1));
    }

    @Test
    void machinesMapToTheModuleThatBuildsThem() {
        assertEquals(ModuleId.GUNS, MteModules.moduleOf(GregGunsMTEIDs.AMMO_WORKS));
        assertEquals(ModuleId.MACHINES, MteModules.moduleOf(GregGunsMTEIDs.CAMO_BENCH));
        assertEquals(ModuleId.UTILITY, MteModules.moduleOf(GregGunsMTEIDs.RAMPART));
    }
}
