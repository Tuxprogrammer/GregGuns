package com.gregguns.gun.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.gun.def.GunDefinition;
import com.gregguns.gun.def.GunDefinitions;

/** Every gun in the shared definition list gets exactly one generated visual scenario. */
class GeneratedGunLooksTest {

    @Test
    void oneScenarioPerGunPlusGroup() {
        ScenarioRegistry registry = new ScenarioRegistry();
        GeneratedGunLooks.registerAll(registry);
        for (GunDefinition def : GunDefinitions.ALL) {
            Scenario s = registry.get("mp.look." + def.name());
            assertNotNull(s, def.name());
            assertFalse(s.includedInAll(), def.name() + " needs a client");
        }
        Scenario group = registry.get("mp.look.all");
        assertNotNull(group);
        assertFalse(group.includedInAll());
        assertEquals(GunDefinitions.ALL.size() + 1, registry.size());
        assertTrue(
            registry.forAll()
                .isEmpty());
    }
}
