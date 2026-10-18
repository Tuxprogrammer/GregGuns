package com.gregguns;

import com.gregguns.core.DevEnvironment;
import com.gregguns.core.selfcheck.SelfCheckRunner;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        GregGuns.LOG.info("{} {} loading", GregGuns.NAME, Tags.VERSION);
    }

    public void init(FMLInitializationEvent event) {}

    public void postInit(FMLPostInitializationEvent event) {}

    /**
     * Runs the dev self-check once every mod has registered its recipes.
     *
     * <p>
     * Deviation: CONTRIBUTING.md (Testing) says the self-check runs in postInit. It runs here, in load-complete,
     * because GT buffers its crafting-table recipes and registers the second pass in its own load-complete handler, and
     * other mods add recipes in their postInit; postInit would check an incomplete recipe set. See
     * {@link SelfCheckRunner}.
     */
    public void loadComplete(FMLLoadCompleteEvent event) {
        if (DevEnvironment.isActive()) {
            SelfCheckRunner.run();
        }
    }
}
