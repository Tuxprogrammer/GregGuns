package com.gregguns.utility;

import com.gregguns.GregGuns;
import com.gregguns.armor.slots.EquipmentGuiHandler;
import com.gregguns.core.DevEnvironment;
import com.gregguns.core.module.GregGunsModule;
import com.gregguns.core.module.ModuleId;
import com.gregguns.core.registry.ModuleRegistrar;
import com.gregguns.core.testing.ScenarioRegistry;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkRegistry;

/**
 * The {@code modules.utility} module: the Turret, Rampart, explosive charges, chests, Bio Blob and Camp Flag (M6). Each
 * content area has its own {@code *Registration} class and this module calls it once per hook, so the parallel M6
 * chunks edit different files. Needs the machines module. Both sides; it has no client half yet.
 */
// Spec: docs/ROADMAP.md (M6), docs/gregguns-spec/09-machines-utility/index.md
public final class UtilityModule implements GregGunsModule {

    @Override
    public ModuleId id() {
        return ModuleId.UTILITY;
    }

    @Override
    public void preInit(FMLPreInitializationEvent event, ModuleRegistrar registrar) {
        UtilityItemOrder.registerAll(registrar);
        TurretRegistration.preInit(registrar);
        RampartRegistration.preInit(registrar);
    }

    @Override
    public void initContent(FMLInitializationEvent event) {
        // FML has one GUI handler per mod. The armor module registers it too; registering the same handler class again
        // is harmless and keeps the utility GUIs working whatever the flags say.
        NetworkRegistry.INSTANCE.registerGuiHandler(GregGuns.instance, new EquipmentGuiHandler());
        TurretRegistration.initContent();
        RampartRegistration.initContent();
        WorldBlockRegistration.initContent();
    }

    @Override
    public void init(FMLInitializationEvent event) {
        UtilityParts.registerRecipes();
        TurretRegistration.init();
        ChargeRegistration.init();
        WorldBlockRegistration.init();
        RampartRegistration.init();
        if (DevEnvironment.testHooksEnabled()) {
            ScenarioRegistry registry = ScenarioRegistry.SCENARIOS;
            UtilityScenarios.registerAll(registry);
            TurretRegistration.registerScenarios(registry);
            ChargeRegistration.registerScenarios(registry);
            WorldBlockRegistration.registerScenarios(registry);
            RampartRegistration.registerScenarios(registry);
        }
    }
}
