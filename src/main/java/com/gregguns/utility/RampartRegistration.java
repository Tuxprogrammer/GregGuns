package com.gregguns.utility;

import com.gregguns.core.registry.ModuleRegistrar;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.utility.rampart.RampartMachines;
import com.gregguns.utility.rampart.RampartScenarios;

/**
 * Registration of the Rampart and the Gun Mount Hatches (chunk m6/rampart): the machines, their recipes and the
 * scenarios. The Bastion Plating metas belong to the casing block, which the guns module creates. {@link UtilityModule}
 * calls the three hooks. Common side.
 */
// Spec: docs/ROADMAP.md (M6)
public final class RampartRegistration {

    private RampartRegistration() {}

    /**
     * Config-dependent registration: items, blocks, entities and tile entities.
     *
     * @param registrar the utility module's registrar
     */
    public static void preInit(ModuleRegistrar registrar) {
        // Nothing to register here: the entity is the Turret's, and the machines wait for GT's MTE window.
    }

    /** Machines (inside GT's MTE window), recipes and event handlers. */
    public static void init() {
        if (RampartMachines.register()) {
            RampartMachines.registerRecipes();
        }
    }

    /**
     * Dev-only scenarios.
     *
     * @param registry where to add them
     */
    public static void registerScenarios(ScenarioRegistry registry) {
        RampartScenarios.registerAll(registry);
    }
}
