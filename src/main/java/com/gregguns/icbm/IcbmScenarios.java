package com.gregguns.icbm;

import com.gregguns.core.module.ModuleId;
import com.gregguns.core.module.ModuleState;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;

/**
 * In-game scenarios of the ICBM module. Dev environment only (registered behind {@code testHooksEnabled}), server side.
 * Registered only while the module is enabled, so they need {@code modules.icbm} on and Galacticraft present
 * ({@code GG_MODULES=guns,armor,machines,utility,npcs,worldgen,icbm}).
 */
// Spec: docs/ROADMAP-ICBM.md (I0 exit gate)
final class IcbmScenarios {

    /** Galacticraft's main class in the GTNH build; probed by name, without initializing it. */
    private static final String GC_CORE_CLASS = "micdoodle8.mods.galacticraft.core.GalacticraftCore";

    private IcbmScenarios() {}

    static void registerAll(ScenarioRegistry registry) {
        registry.register(new ModuleGate());
    }

    /** With the module on and Galacticraft present, the module reports active and Galacticraft is on the classpath. */
    private static final class ModuleGate implements Scenario {

        @Override
        public String name() {
            return "icbm.module_gate";
        }

        @Override
        public String description() {
            return "With modules.icbm on and Galacticraft loaded, the ICBM module is active, the gate's verdict is"
                + " ACTIVE and Galacticraft's main class is present.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            out.check(IcbmAvailability.galacticraftPresent(), "Galacticraft should be loaded");
            out.checkEquals("gate verdict", IcbmGate.Verdict.ACTIVE, IcbmAvailability.verdict());
            out.check(ModuleState.isActive(ModuleId.ICBM), "the ICBM module should report active");
            boolean classPresent;
            try {
                Class.forName(GC_CORE_CLASS, false, IcbmScenarios.class.getClassLoader());
                classPresent = true;
            } catch (ClassNotFoundException e) {
                classPresent = false;
            }
            out.check(classPresent, "Galacticraft's main class should be on the classpath");
            return out.build();
        }
    }
}
