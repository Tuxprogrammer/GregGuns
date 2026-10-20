package com.gregguns.worldgen;

import com.gregguns.core.DevEnvironment;
import com.gregguns.core.module.GregGunsModule;
import com.gregguns.core.module.ModuleId;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.worldgen.loot.LootCategories;
import com.gregguns.worldgen.loot.LootScenarios;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;

/**
 * The {@code modules.worldgen} module: structures and their loot (M8). Each infrastructure piece adds its own
 * registration class and one delegating call per hook here, so parallel pieces edit different files. Needs the npcs
 * module. Both sides; no client half.
 *
 * <p>
 * G7 (loot) created this class only so its categories and scenarios have a hook; G1 owns the config gating and may
 * restructure it. Merge by keeping the loot calls below.
 */
// Spec: docs/ROADMAP.md (M8), docs/gregguns-spec/11-worldgen/index.md
public final class WorldgenModule implements GregGunsModule {

    @Override
    public ModuleId id() {
        return ModuleId.WORLDGEN;
    }

    @Override
    public void init(FMLInitializationEvent event) {
        if (DevEnvironment.isActive()) {
            LootScenarios.registerAll(ScenarioRegistry.SCENARIOS);
        }
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {
        // After every mod's items exist (GT parts resolve through the unificator).
        LootCategories.register();
    }
}
