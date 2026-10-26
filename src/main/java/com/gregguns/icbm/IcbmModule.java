package com.gregguns.icbm;

import com.gregguns.GregGuns;
import com.gregguns.core.DevEnvironment;
import com.gregguns.core.module.GregGunsModule;
import com.gregguns.core.module.ModuleId;
import com.gregguns.core.testing.ScenarioRegistry;

import cpw.mods.fml.common.event.FMLInitializationEvent;

/**
 * The {@code modules.icbm} module: missiles, warheads and the ICBM program. I0 is the foundation only, so it
 * registers no content yet; the self-disable rule (D9) lives in the module system, and this module carries the gate
 * scenario. Later milestones add their
 * registration classes and one delegating call per hook here. Both sides; no client half yet.
 */
// Spec: docs/ROADMAP-ICBM.md (I0), docs/gregguns-icbm-spec/DECISIONS.md (D9)
public final class IcbmModule implements GregGunsModule {

    @Override
    public ModuleId id() {
        return ModuleId.ICBM;
    }

    /**
     * Runs for every module whatever its flag says. The scenarios are registered here, not in {@link #init}, so the
     * gate
     * scenario also runs when the module turned itself off (Galacticraft missing). The self-disable log line comes from
     * the module manager (see {@code ModuleResolution.SkipReason#MISSING_MOD}).
     */
    @Override
    public void initContent(FMLInitializationEvent event) {
        if (DevEnvironment.testHooksEnabled()) {
            IcbmScenarios.registerAll(ScenarioRegistry.SCENARIOS);
        }
    }

    @Override
    public void init(FMLInitializationEvent event) {
        // Only reached when the module is enabled, which already implies Galacticraft is present.
        GregGuns.LOG.info("ICBM module active (Galacticraft present)");
    }
}
