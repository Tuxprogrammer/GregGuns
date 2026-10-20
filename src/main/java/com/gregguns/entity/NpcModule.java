package com.gregguns.entity;

import net.minecraftforge.common.MinecraftForge;

import com.gregguns.core.DevEnvironment;
import com.gregguns.core.module.GregGunsModule;
import com.gregguns.core.module.ModuleId;
import com.gregguns.core.registry.ModuleRegistrar;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.entity.npc.InfernalDropsHandler;
import com.gregguns.entity.npc.InfernalHooks;
import com.gregguns.entity.npc.NpcCampPort;
import com.gregguns.entity.npc.NpcRegistry;
import com.gregguns.entity.npc.spawn.NpcSpawnHandler;
import com.gregguns.utility.camp.CampSpawnPorts;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
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
        // N7: every NPC entity (the dev-only test NPC included) and the spawn egg.
        NpcRegistry.register(registrar);
    }

    @Override
    public void init(FMLInitializationEvent event) {
        // N8: the real camp port replaces CampSpawnPort.NONE (M8 contract: counts never load chunks).
        CampSpawnPorts.install(new NpcCampPort());
        // N9: tiered infernal drops, only when Infernal Mobs is loaded (common code never names its classes).
        if (Loader.isModLoaded(InfernalHooks.MOD_ID)) {
            MinecraftForge.EVENT_BUS.register(new InfernalDropsHandler());
        }
        // N6: the CheckSpawn gate and the SpecialSpawn danger carrier.
        MinecraftForge.EVENT_BUS.register(new NpcSpawnHandler());
        if (DevEnvironment.isActive()) {
            NpcScenarios.registerAll(ScenarioRegistry.SCENARIOS);
        }
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {
        // N6: after every mod registered its biomes, and before EnderZoo's postInit (before:EnderZoo).
        NpcSpawnHandler.registerSpawns();
    }
}
