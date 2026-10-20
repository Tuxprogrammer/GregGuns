package com.gregguns.worldgen;

import com.gregguns.core.DevEnvironment;
import com.gregguns.core.module.GregGunsModule;
import com.gregguns.core.module.ModuleId;
import com.gregguns.core.registry.ModuleRegistrar;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.worldgen.loot.LootCategories;
import com.gregguns.worldgen.loot.LootScenarios;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
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
        // The cascade guard records any chunk loaded while a structure populates (GGS-11-09); safe to call twice.
        PopulateGuard.register();
        if (DevEnvironment.isActive()) {
            WorldgenScenarios.registerAll(ScenarioRegistry.SCENARIOS);
            LootScenarios.registerAll(ScenarioRegistry.SCENARIOS);
        }
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {
        // After every mod's items exist (GT parts resolve through the unificator).
        LootCategories.register();
    }
}
