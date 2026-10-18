package com.gregguns;

import com.gregguns.core.DevEnvironment;
import com.gregguns.core.command.GregGunsCommand;
import com.gregguns.core.command.TestSubCommand;
import com.gregguns.core.testing.HarnessScenarios;
import com.gregguns.core.testing.ScenarioRegistry;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        GregGuns.LOG.info("{} {} loading", GregGuns.NAME, Tags.VERSION);
    }

    public void init(FMLInitializationEvent event) {}

    public void postInit(FMLPostInitializationEvent event) {}

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
}
