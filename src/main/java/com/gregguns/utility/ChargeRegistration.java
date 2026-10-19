package com.gregguns.utility;

import com.gregguns.core.registry.ModuleRegistrar;
import com.gregguns.core.testing.ScenarioRegistry;

/**
 * Registration stub for the explosive charges (chunk m6/charges). {@link UtilityModule} already calls all three hooks;
 * that chunk fills this class and nothing else in the module wiring. Common side.
 */
// Spec: docs/ROADMAP.md (M6)
public final class ChargeRegistration {

    private ChargeRegistration() {}

    /**
     * Config-dependent registration: items, blocks, entities and tile entities.
     *
     * @param registrar the utility module's registrar
     */
    public static void preInit(ModuleRegistrar registrar) {}

    /** Recipes and event handlers. */
    public static void init() {}

    /**
     * Dev-only scenarios.
     *
     * @param registry where to add them
     */
    public static void registerScenarios(ScenarioRegistry registry) {}
}
