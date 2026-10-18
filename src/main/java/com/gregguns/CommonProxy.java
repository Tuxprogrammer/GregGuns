package com.gregguns;

import com.gregguns.core.DevEnvironment;
import com.gregguns.core.GregGunsTab;
import com.gregguns.core.command.GregGunsCommand;
import com.gregguns.core.command.TestSubCommand;
import com.gregguns.core.config.GregGunsConfig;
import com.gregguns.core.config.ModuleFlags;
import com.gregguns.core.module.ModuleManager;
import com.gregguns.core.testing.HarnessScenarios;
import com.gregguns.core.testing.ScenarioRegistry;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

public class CommonProxy {

    private ModuleManager modules;

    public void preInit(FMLPreInitializationEvent event) {
        GregGuns.LOG.info("{} {} loading", GregGuns.NAME, Tags.VERSION);
        // Order per CONTRIBUTING.md: config first, then the modules register items, blocks and entities.
        GregGunsConfig.register();
        modules = new ModuleManager(ModuleFlags::isEnabled, new GregGunsTab());
        registerModules(modules);
        modules.preInit(event);
    }

    public void init(FMLInitializationEvent event) {
        modules.init(event);
    }

    public void postInit(FMLPostInitializationEvent event) {
        modules.postInit(event);
    }

    /**
     * Registers the dev-only {@code /gregguns} command (with its {@code test} subcommand) and the built-in scenarios.
     * Does nothing outside the dev environment.
     */
    public void serverStarting(FMLServerStartingEvent event) {
        if (!DevEnvironment.isActive()) {
            return;
        }
        HarnessScenarios.registerAll(ScenarioRegistry.SCENARIOS);
        GregGunsCommand command = new GregGunsCommand();
        command.register(new TestSubCommand(ScenarioRegistry.SCENARIOS));
        event.registerServerCommand(command);
        GregGuns.LOG.info("Registered /gregguns (dev environment), {} scenarios", ScenarioRegistry.SCENARIOS.size());
    }

    /**
     * Adds every module class. A milestone adds its module here, for example
     * {@code manager.register(new GunsModule())}.
     * {@code ClientProxy} overrides this to add the matching {@code ClientModule}s.
     *
     * @param manager the manager to register with
     */
    protected void registerModules(ModuleManager manager) {}
}
