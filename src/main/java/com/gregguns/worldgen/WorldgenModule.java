package com.gregguns.worldgen;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.gregguns.GregGuns;
import com.gregguns.core.DevEnvironment;
import com.gregguns.core.module.GregGunsModule;
import com.gregguns.core.module.ModuleId;
import com.gregguns.core.registry.ModuleRegistrar;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.worldgen.camp.PartLayouts;
import com.gregguns.worldgen.layout.LayoutRegistry;
import com.gregguns.worldgen.layout.PaletteKeys;
import com.gregguns.worldgen.loot.ChestScenarios;
import com.gregguns.worldgen.loot.LootCategories;
import com.gregguns.worldgen.loot.LootScenarios;
import com.gregguns.worldgen.nest.NestBlockRegistration;
import com.gregguns.worldgen.spawner.HoleSpawnerRegistration;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.registry.GameRegistry;

/**
 * The {@code modules.worldgen} module: natural structures and their loot (M8). Each infrastructure piece adds its own
 * registration class and one delegating call per hook here. Needs the npcs module. Both sides; no client half yet.
 * The generator is registered in postInit, once, and only when {@link WorldgenSettings#generatorWanted()}.
 */
// Spec: docs/ROADMAP.md (M8), docs/gregguns-spec/11-worldgen/index.md
public final class WorldgenModule implements GregGunsModule {

    @Override
    public ModuleId id() {
        return ModuleId.WORLDGEN;
    }

    @Override
    public void preInit(FMLPreInitializationEvent event, ModuleRegistrar registrar) {
        HoleSpawnerRegistration.preInit(registrar);
        NestBlockRegistration.preInit(registrar);
    }

    @Override
    public void init(FMLInitializationEvent event) {
        // The cascade guard records any chunk loaded while a structure populates (GGS-11-09); safe to call twice.
        PopulateGuard.register();
        if (DevEnvironment.testHooksEnabled()) {
            WorldgenScenarios.registerAll(ScenarioRegistry.SCENARIOS);
            LootScenarios.registerAll(ScenarioRegistry.SCENARIOS);
            ChestScenarios.registerAll(ScenarioRegistry.SCENARIOS);
            GeneratorScenarios.registerAll(ScenarioRegistry.SCENARIOS);
            CascadeScenarios.registerAll(ScenarioRegistry.SCENARIOS);
            Ross128Scenarios.registerAll(ScenarioRegistry.SCENARIOS);
            PlacementScenarios.registerAll(ScenarioRegistry.SCENARIOS);
            SpawnerScenarios.registerAll(ScenarioRegistry.SCENARIOS);
            NestScenarios.registerAll(ScenarioRegistry.SCENARIOS);
        }
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {
        // After every mod's items exist (GT parts resolve through the unificator).
        LootCategories.register();
        registerGenerator();
    }

    /**
     * Loads the layouts of every enabled structure, builds the pools, and registers the one generator at weight 4
     * (GGS-11-07). Skipped entirely when {@link WorldgenSettings#generatorWanted()} is false. A structure whose layout
     * is missing or broken is left out and reported once (GGS-11-28); with no layouts the pools are empty and the
     * generator does nothing.
     */
    /** Structure layouts plus every camp part layout the placer may ask for (most do not exist yet; that is fine). */
    static List<String> layoutIds() {
        List<String> ids = new ArrayList<>(StructureCatalog.idsToLoad(WorldgenSettings::enabled));
        if (WorldgenSettings.enabled(StructureId.MILITARY_BASE)) {
            ids.addAll(PartLayouts.FromRegistry.allIds());
        }
        return ids;
    }

    /** The ids in {@link #layoutIds()} that are allowed to be absent: the camp color variants. */
    static Set<String> optionalLayoutIds() {
        return new HashSet<>(PartLayouts.FromRegistry.variantIds());
    }

    private static void registerGenerator() {
        if (!WorldgenSettings.generatorWanted()) {
            return;
        }
        PaletteKeys keys = PaletteKeys.resolveForPack(GregGuns.LOG::warn);
        LayoutRegistry layouts = LayoutRegistry.load(
            new LayoutRegistry.ClasspathSource(LayoutRegistry.ClasspathSource.STRUCTURES),
            layoutIds(),
            optionalLayoutIds(),
            keys,
            GregGuns.LOG::warn,
            GregGuns.LOG::debug);
        StructureGenerator.configure(layouts, keys);
        GameRegistry.registerWorldGenerator(new StructureGenerator(), StructureGenerator.WEIGHT);
        // Galacticraft drops Forge world generators on Ross128b and Ross128ba; hook their chunk providers (0019).
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new Ross128ProviderHook());
    }
}
