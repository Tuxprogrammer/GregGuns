package com.gregguns.gun;

import com.gregguns.combat.CombatModule;
import com.gregguns.core.DevEnvironment;
import com.gregguns.core.module.GregGunsModule;
import com.gregguns.core.module.ModuleId;
import com.gregguns.core.registry.ModuleRegistrar;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.item.ammo.AmmoItems;
import com.gregguns.item.part.PartItems;
import com.gregguns.machine.AmmoPressMachines;
import com.gregguns.projectile.ProjectileModule;
import com.gregguns.recipe.GregGunsRecipeMaps;
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
        // Built here so GT records GregGuns as the map's owner mod (NEI badge), see GregGunsRecipeMaps.
        GregGunsRecipeMaps.init();
        ProjectileModule.preInit(registrar);
    }

    @Override
    public void init(FMLInitializationEvent event) {
        // Recipes and event handlers: one call per area.
        CombatModule.init();
        // Pulled into M1 from M4 for the pistol slice: the LV Ammo Press and the 9mm chain's recipes.
        AmmoPressMachines.register();
        ProductionRecipes.register();
        if (DevEnvironment.isActive()) {
            ProductionScenarios.registerAll(ScenarioRegistry.SCENARIOS);
        }
        ProjectileModule.init();
    }
}
