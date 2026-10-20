package com.gregguns.gun;

import com.gregguns.combat.CombatModule;
import com.gregguns.core.DevEnvironment;
import com.gregguns.core.module.GregGunsModule;
import com.gregguns.core.module.ModuleId;
import com.gregguns.core.registry.ModuleRegistrar;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.explosion.GrenadeScenarios;
import com.gregguns.explosion.MvGrenadeScenarios;
import com.gregguns.gun.def.GunDefinition;
import com.gregguns.gun.def.GunDefinitions;
import com.gregguns.gun.server.ChargeScenarios;
import com.gregguns.gun.server.EnergyCoreScenarios;
import com.gregguns.gun.server.FrameworkScenarios;
import com.gregguns.gun.server.GeneratedGunLooks;
import com.gregguns.gun.server.GunAccuracyScenarios;
import com.gregguns.gun.server.GunCamoScenarios;
import com.gregguns.gun.server.GunEvents;
import com.gregguns.gun.server.GunKeybinds;
import com.gregguns.gun.server.MpScenarios;
import com.gregguns.item.ammo.AmmoItems;
import com.gregguns.item.grenade.GrenadeItems;
import com.gregguns.item.gun.GunItems;
import com.gregguns.item.melee.MeleeItems;
import com.gregguns.item.melee.MeleeScenarios;
import com.gregguns.item.part.ComponentItems;
import com.gregguns.item.part.PartItems;
import com.gregguns.machine.AmmoPressMachines;
import com.gregguns.projectile.ProjectileModule;
import com.gregguns.recipe.ComponentRecipes;
import com.gregguns.recipe.ComponentScenarios;
import com.gregguns.recipe.GregGunsRecipeMaps;
import com.gregguns.recipe.GrenadeRecipes;
import com.gregguns.recipe.GunRecipes;
import com.gregguns.recipe.MeleeRecipes;
import com.gregguns.recipe.ProductionRecipes;
import com.gregguns.recipe.ProductionScenarios;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

/**
 * The {@code modules.guns} module: the combat core and ballistic arsenal (M1, M2). The damage system, guns,
 * projectiles, ammo and their recipes all register from here, so turning the flag off removes them together.
 *
 * <p>
 * Each feature area keeps its own registration class and adds one delegating call below, so parallel work on
 * different areas touches different lines. Both sides; the client half is {@code com.gregguns.client.GunsClientModule}.
 */
// Spec: docs/ROADMAP.md (M1, M2)
public final class GunsModule implements GregGunsModule {

    @Override
    public ModuleId id() {
        return ModuleId.GUNS;
    }

    @Override
    public void preInit(FMLPreInitializationEvent event, ModuleRegistrar registrar) {
        // Items, entities and other content: one call per area, e.g. AmmoItems.register(registrar).
        AmmoItems.register(registrar);
        PartItems.register(registrar);
        ComponentItems.register(registrar);
        // Built here so GT records GregGuns as the map's owner mod (NEI badge), see GregGunsRecipeMaps.
        GregGunsRecipeMaps.init();
        AmmoPressMachines.registerBlocks(registrar);
        ProjectileModule.preInit(registrar);
        GunItems.register(registrar);
        GunKeybinds.register();
        MeleeItems.register(registrar);
        GrenadeItems.register(registrar);
    }

    @Override
    public void init(FMLInitializationEvent event) {
        // Recipes and event handlers: one call per area.
        CombatModule.init();
        // Pulled into M1 from M4 for the pistol slice: the LV Ammo Press and the 9mm chain's recipes.
        AmmoPressMachines.register();
        ProductionRecipes.register();
        GunRecipes.register();
        ComponentRecipes.register();
        MeleeRecipes.register();
        GrenadeRecipes.register();
        if (DevEnvironment.isActive()) {
            ProductionScenarios.registerAll(ScenarioRegistry.SCENARIOS);
            for (GunDefinition gun : GunDefinitions.ALL) {
                gun.registerScenarios(ScenarioRegistry.SCENARIOS);
            }
            GunCamoScenarios.registerAll(ScenarioRegistry.SCENARIOS);
            GunAccuracyScenarios.registerAll(ScenarioRegistry.SCENARIOS);
            EnergyCoreScenarios.registerAll(ScenarioRegistry.SCENARIOS);
            ChargeScenarios.registerAll(ScenarioRegistry.SCENARIOS);
            com.gregguns.gun.server.AmmoSpendScenarios.registerAll(ScenarioRegistry.SCENARIOS);
            com.gregguns.gun.server.ToolGunScenarios.registerAll(ScenarioRegistry.SCENARIOS);
            com.gregguns.gun.server.EnergyGunScenarios.registerAll(ScenarioRegistry.SCENARIOS);
            ComponentScenarios.registerAll(ScenarioRegistry.SCENARIOS);
            FrameworkScenarios.registerAll(ScenarioRegistry.SCENARIOS);
            MpScenarios.registerAll(ScenarioRegistry.SCENARIOS);
            GeneratedGunLooks.registerAll(ScenarioRegistry.SCENARIOS);
            MeleeScenarios.registerAll(ScenarioRegistry.SCENARIOS);
            GrenadeScenarios.registerAll(ScenarioRegistry.SCENARIOS);
            MvGrenadeScenarios.registerAll(ScenarioRegistry.SCENARIOS);
            com.gregguns.explosion.BlastScenarios.registerAll(ScenarioRegistry.SCENARIOS);
        }
        ProjectileModule.init();
        GunEvents.register();
        cpw.mods.fml.common.FMLCommonHandler.instance()
            .bus()
            .register(com.gregguns.explosion.BlastTicker.instance());
    }
}
