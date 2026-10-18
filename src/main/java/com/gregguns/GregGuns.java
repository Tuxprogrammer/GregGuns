package com.gregguns;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

@Mod(
    modid = GregGuns.MODID,
    name = GregGuns.NAME,
    version = Tags.VERSION,
    acceptedMinecraftVersions = "[1.7.10]",
    dependencies = GregGuns.DEPENDENCIES)
public class GregGuns {

    public static final String MODID = "gregguns";
    public static final String NAME = "GregGuns";
    /** Hard dependencies: GregGuns is built on GT5-Unofficial and GTNHLib and cannot load without them. */
    public static final String DEPENDENCIES = "required-after:gregtech;required-after:gtnhlib";
    public static final Logger LOG = LogManager.getLogger(MODID);

    @SidedProxy(clientSide = "com.gregguns.ClientProxy", serverSide = "com.gregguns.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }
}
