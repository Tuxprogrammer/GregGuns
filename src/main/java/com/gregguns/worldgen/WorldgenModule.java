package com.gregguns.worldgen;

import com.gregguns.core.DevEnvironment;
import com.gregguns.core.module.GregGunsModule;
import com.gregguns.core.module.ModuleId;
import com.gregguns.core.registry.ModuleRegistrar;
import com.gregguns.core.testing.ScenarioRegistry;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

/**
 * The {@code modules.worldgen} module: natural structures and their loot (M8). Each infrastructure piece adds its own
 * registration class and one delegating call per hook here. Needs the npcs module. Both sides; no client half yet.
 * The generator itself is registered by a later piece, and only when {@link WorldgenSettings#generatorWanted()}.
 */
// Spec: docs/ROADMAP.md (M8), docs/gregguns-spec/11-worldgen/index.md
public final class WorldgenModule implements GregGunsModule {

    @Override
    public ModuleId id() {
        return ModuleId.WORLDGEN;
    }

    @Override
    public void preInit(FMLPreInitializationEvent event, ModuleRegistrar registrar) {
        // Nothing to register yet; later pieces add their calls here.
    }

    @Override
    public void init(FMLInitializationEvent event) {
        if (DevEnvironment.isActive()) {
            WorldgenScenarios.registerAll(ScenarioRegistry.SCENARIOS);
        }
    }
}
