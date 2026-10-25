package com.gregguns.icbm;

import com.gregguns.GregGuns;
import com.gregguns.core.DevEnvironment;
import com.gregguns.core.module.GregGunsModule;
import com.gregguns.core.module.ModuleId;
import com.gregguns.core.registry.ModuleRegistrar;
import com.gregguns.core.testing.ScenarioRegistry;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

/**
 * The {@code modules.icbm} module: missiles, warheads and the ICBM program. I0 is the foundation only, so it
 * registers no content yet; it carries the self-disable rule (D9) and the gate scenario. Later milestones add their
 * registration classes and one delegating call per hook here. Both sides; no client half yet.
 */
// Spec: docs/ROADMAP-ICBM.md (I0), docs/gregguns-icbm-spec/DECISIONS.md (D9)
public final class IcbmModule implements GregGunsModule {

    @Override
    public ModuleId id() {
        return ModuleId.ICBM;
    }

    /**
     * Runs for every module whatever its flag says, so it is the place for the one self-disable log line: the manager
     * itself only knows the flag reads as off and would log "off by config".
     */
    @Override
    public void preInit(FMLPreInitializationEvent event, ModuleRegistrar registrar) {
        String line = IcbmGate.logLine(IcbmAvailability.verdict());
        if (line != null) {
            GregGuns.LOG.warn(line);
        }
    }

    @Override
    public void init(FMLInitializationEvent event) {
        // Only reached when the module is enabled, which already implies Galacticraft is present.
        GregGuns.LOG.info("ICBM module active (Galacticraft present)");
        if (DevEnvironment.testHooksEnabled()) {
            IcbmScenarios.registerAll(ScenarioRegistry.SCENARIOS);
        }
    }
}
