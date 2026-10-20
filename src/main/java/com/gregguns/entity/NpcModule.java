package com.gregguns.entity;

import com.gregguns.core.DevEnvironment;
import com.gregguns.core.module.GregGunsModule;
import com.gregguns.core.module.ModuleId;
import com.gregguns.core.registry.ModuleRegistrar;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.entity.npc.NpcEntity;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

/**
 * The {@code modules.npcs} module: hostile NPCs, their AI, loot and spawning (M7). Each infrastructure piece adds its
 * own registration class and one delegating call per hook here, so the parallel M7 pieces edit different files. Needs
 * the guns module. Both sides; it has no client half yet.
 */
// Spec: docs/ROADMAP.md (M7), docs/gregguns-spec/10-entities-npcs/index.md
public final class NpcModule implements GregGunsModule {

    @Override
    public ModuleId id() {
        return ModuleId.NPCS;
    }

    @Override
    public void preInit(FMLPreInitializationEvent event, ModuleRegistrar registrar) {
        // Pieces N4 onward add their registration calls here.
        if (DevEnvironment.isActive()) {
            // The base NPC, registered for dev scenarios and the test renderer only. N7 registers the real mobs.
            registrar.registerEntity("npc_test", NpcEntity.class, NpcEntity.TEST_ENTITY_ID, 64, 3, true);
        }
    }

    @Override
    public void init(FMLInitializationEvent event) {
        if (DevEnvironment.isActive()) {
            NpcScenarios.registerAll(ScenarioRegistry.SCENARIOS);
        }
    }
}
