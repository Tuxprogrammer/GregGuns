package com.gregguns.icbm;

import java.util.Arrays;

import com.gregguns.compat.ModIds;
import com.gregguns.core.config.ModulesConfig;
import com.gregguns.core.module.ModuleId;
import com.gregguns.core.module.ModuleState;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;

/**
 * In-game scenarios of the ICBM module. Dev environment only (registered behind {@code testHooksEnabled}), server side.
 * Registered whatever the module's flag says, and they check the branch the run is on, so they pass with Galacticraft
 * present (module active when its flag and requirements are on) and with it absent (module off).
 */
// Spec: docs/ROADMAP-ICBM.md (I0 exit gate)
final class IcbmScenarios {

    /** Galacticraft's main class in the GTNH build; probed by name, without initializing it. */
    private static final String GC_CORE_CLASS = "micdoodle8.mods.galacticraft.core.GalacticraftCore";

    private IcbmScenarios() {}

    static void registerAll(ScenarioRegistry registry) {
        registry.register(new ModuleGate());
        IcbmBlastScenarios.registerAll(registry);
    }

    /**
     * The module is active exactly when its flag is on, Galacticraft is loaded and the modules it needs are on; and
     * Galacticraft's main class is on the classpath exactly when the mod is loaded.
     */
    private static final class ModuleGate implements Scenario {

        @Override
        public String name() {
            return "icbm.module_gate";
        }

        @Override
        public String description() {
            return "The ICBM module is active exactly when modules.icbm is on, Galacticraft is loaded and guns and"
                + " machines are on; Galacticraft's main class is present exactly when the mod is loaded.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            boolean gc = IcbmAvailability.galacticraftPresent();
            boolean expected = ModulesConfig.icbm && gc
                && ModuleState.isActive(ModuleId.GUNS)
                && ModuleState.isActive(ModuleId.MACHINES);
            out.checkEquals("module active", expected, ModuleState.isActive(ModuleId.ICBM));
            out.checkEquals(
                "requiredMods",
                Arrays.asList(ModIds.GALACTICRAFT_CORE),
                Arrays.asList(ModuleId.ICBM.requiredMods()));
            boolean classPresent;
            try {
                Class.forName(GC_CORE_CLASS, false, IcbmScenarios.class.getClassLoader());
                classPresent = true;
            } catch (ClassNotFoundException e) {
                classPresent = false;
            }
            out.checkEquals("Galacticraft main class present", gc, classPresent);
            return out.build();
        }
    }
}
