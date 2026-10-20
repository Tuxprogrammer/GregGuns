package com.gregguns;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

import com.gregguns.core.module.ModuleManager;

/** The client proxy must register each client module id once; the registry throws on a duplicate at boot. */
class ClientProxyModulesTest {

    @Test
    void clientModuleIdsAreUnique() {
        ModuleManager manager = new ModuleManager(id -> true, null);
        assertDoesNotThrow(() -> new ClientProxy().registerModules(manager));
    }
}
