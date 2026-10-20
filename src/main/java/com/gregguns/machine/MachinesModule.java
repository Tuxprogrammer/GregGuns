package com.gregguns.machine;

import com.gregguns.core.DevEnvironment;
import com.gregguns.core.module.GregGunsModule;
import com.gregguns.core.module.ModuleId;
import com.gregguns.core.registry.ModuleRegistrar;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.machine.repair.RepairMachines;
import com.gregguns.machine.repair.RepairScenarios;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

/**
 * The {@code modules.machines} module: GregGuns' production and utility machines (M4) other than the Ammo Press
 * family, which stays in the guns module because gun ammo needs it. Each machine area keeps its own registration class
 * and adds one delegating call to {@link #preInit} or {@link #init}, so parallel branches touch different lines.
 * Both sides; it has no client half yet.
 */
// Spec: docs/ROADMAP.md (M4), docs/gregguns-spec/08-machines-production/machine-roster.md
public final class MachinesModule implements GregGunsModule {

    @Override
    public ModuleId id() {
        return ModuleId.MACHINES;
    }

    @Override
    public void preInit(FMLPreInitializationEvent event, ModuleRegistrar registrar) {
        // Nothing to register before GT's MTE window. The Reaction Chamber and Retort were retired by design record
        // 0012;
        // their MTE ids (20106 to 20108) stay reserved, and Reaction Casing meta 2 stays as an inert block.
    }

    @Override
    public void init(FMLInitializationEvent event) {
        // Repair Bench, Armory Gantry and Camo Bench.
        RepairMachines.register();
        RepairMachines.registerRecipes();
        if (DevEnvironment.testHooksEnabled()) {
            RepairScenarios.registerAll(ScenarioRegistry.SCENARIOS);
            ScenarioRegistry.SCENARIOS.register(new com.gregguns.core.text.AddedByScenario());
        }
    }
}
