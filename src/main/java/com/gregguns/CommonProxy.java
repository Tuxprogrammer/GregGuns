package com.gregguns;

import com.gregguns.core.GregGunsTab;
import com.gregguns.core.config.GregGunsConfig;
import com.gregguns.core.config.ModuleFlags;
import com.gregguns.core.module.ModuleManager;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

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
     * Adds every module class. A milestone adds its module here, for example
     * {@code manager.register(new GunsModule())}.
     * {@code ClientProxy} overrides this to add the matching {@code ClientModule}s.
     *
     * @param manager the manager to register with
     */
    protected void registerModules(ModuleManager manager) {}
}
