package com.gregguns.icbm;

import com.gregguns.GregGuns;
import com.gregguns.core.DevEnvironment;
import com.gregguns.core.module.GregGunsModule;
import com.gregguns.core.module.ModuleId;
import com.gregguns.core.registry.ModuleRegistrar;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.icbm.warhead.WarheadRegistration;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

/**
 * The {@code modules.icbm} module: missiles, warheads and the ICBM program. I1 adds the warheads; the self-disable rule
 * (D9) lives in the module system, and this module carries the gate
 * scenario. Later milestones add their
 * registration classes and one delegating call per hook here. Both sides; no client half yet.
 */
// Spec: docs/ROADMAP-ICBM.md (I0), docs/gregguns-icbm-spec/DECISIONS.md (D9)
public final class IcbmModule implements GregGunsModule {

    @Override
    public ModuleId id() {
        return ModuleId.ICBM;
    }

    /** Warhead blocks, items, tiles and explosives register whatever the flag says (content always registers). */
    @Override
    public void preInit(FMLPreInitializationEvent event, ModuleRegistrar registrar) {
        WarheadRegistration.preInit(registrar);
        com.gregguns.icbm.production.ProductionItems.register(registrar);
        com.gregguns.icbm.missile.MissileRegistration.preInit(registrar);
        com.gregguns.icbm.defense.DefenseMachines.registerItems(registrar);
        com.gregguns.icbm.tools.ToolRegistration.preInit(registrar);
    }

    /**
     * Runs for every module whatever its flag says. The scenarios are registered here, not in {@link #init}, so the
     * gate
     * scenario also runs when the module turned itself off (Galacticraft missing). The self-disable log line comes from
     * the module manager (see {@code ModuleResolution.SkipReason#MISSING_MOD}).
     */
    @Override
    public void initContent(FMLInitializationEvent event) {
        // Content always registers (it is inert until something detonates), so the engine is wired whatever the flag
        // says; the scenarios then also run when the module turned itself off.
        com.gregguns.icbm.blast.BlastEngine.register();
        com.gregguns.icbm.missile.MissileRegistration.initContent();
        // The Launch Control's MTE id must exist on every client and server, so it is built here, in GT's MTE window.
        com.gregguns.icbm.control.ControlMachines.register();
        com.gregguns.icbm.silo.SiloMachines.register();
        com.gregguns.icbm.defense.DefenseMachines.register();
        com.gregguns.icbm.emp.EmpMachines.register();
        if (DevEnvironment.testHooksEnabled()) {
            IcbmScenarios.registerAll(ScenarioRegistry.SCENARIOS);
            WarheadRegistration.registerScenarios(ScenarioRegistry.SCENARIOS);
            com.gregguns.icbm.missile.MissileRegistration.registerScenarios(ScenarioRegistry.SCENARIOS);
            com.gregguns.icbm.control.ControlScenarios.registerAll(ScenarioRegistry.SCENARIOS);
            com.gregguns.icbm.silo.SiloScenarios.registerAll(ScenarioRegistry.SCENARIOS);
            com.gregguns.icbm.missile.DefenseScenarios.registerAll(ScenarioRegistry.SCENARIOS);
            com.gregguns.icbm.production.ProductionScenarios.registerAll(ScenarioRegistry.SCENARIOS);
            com.gregguns.icbm.emp.EmpScenarios.registerAll(ScenarioRegistry.SCENARIOS);
            com.gregguns.icbm.tools.ToolRegistration.registerScenarios(ScenarioRegistry.SCENARIOS);
            // The OpenComputers scenario exists only with the mod; its class names OC types.
            if (cpw.mods.fml.common.Loader.isModLoaded("OpenComputers")) {
                com.gregguns.icbm.compat.oc.OcScenarios.registerAll(ScenarioRegistry.SCENARIOS);
            }
        }
    }

    @Override
    public void init(FMLInitializationEvent event) {
        // Only reached when the module is enabled, which already implies Galacticraft is present.
        GregGuns.LOG.info("ICBM module active (Galacticraft present)");
        WarheadRegistration.init();
        com.gregguns.icbm.missile.MissileRegistration.init();
        com.gregguns.icbm.production.MissileRecipes.init();
        com.gregguns.icbm.control.ControlMachines.registerRecipes();
        com.gregguns.icbm.silo.SiloMachines.registerRecipes();
        com.gregguns.icbm.defense.DefenseMachines.registerRecipes();
        com.gregguns.icbm.emp.EmpMachines.registerRecipes();
        // Nothing of OpenComputers is touched unless the pack has it (GGI-01-01).
        if (cpw.mods.fml.common.Loader.isModLoaded("OpenComputers")) {
            com.gregguns.icbm.compat.oc.OcCompat.register();
        }
        com.gregguns.icbm.production.gc.MissileSchematicPage.registerAll();
        com.gregguns.icbm.tools.ToolRegistration.init();
    }
}
