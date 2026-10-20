package com.gregguns.machine;

import com.gregguns.core.DevEnvironment;
import com.gregguns.core.module.GregGunsModule;
import com.gregguns.core.module.ModuleId;
import com.gregguns.core.registry.ModuleRegistrar;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.machine.reaction.ReactionCraftingRecipes;
import com.gregguns.machine.reaction.ReactionMachines;
import com.gregguns.machine.reaction.ReactionParts;
import com.gregguns.machine.reaction.ReactionRecipeMaps;
import com.gregguns.machine.reaction.ReactionRecipes;
import com.gregguns.machine.reaction.ReactionScenarios;
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
        // Reaction Chamber and Retort. The map is built here so GT records GregGuns as its owner mod (NEI badge).
        ReactionRecipeMaps.init();
        ReactionParts.register(registrar);
        // The Reaction Chamber Casing is meta 2 of the shared casings block, registered by the guns module.
    }

    @Override
    public void init(FMLInitializationEvent event) {
        // Inside GT's MTE registration window (after its preload, before its postload).
        ReactionMachines.register();
        ReactionRecipes.register();
        ReactionCraftingRecipes.register();
        // Repair Bench, Armory Gantry and Camo Bench.
        RepairMachines.register();
        RepairMachines.registerRecipes();
        if (DevEnvironment.isActive()) {
            ReactionScenarios.registerAll(ScenarioRegistry.SCENARIOS);
            RepairScenarios.registerAll(ScenarioRegistry.SCENARIOS);
            ScenarioRegistry.SCENARIOS.register(new com.gregguns.core.text.AddedByScenario());
        }
    }
}
