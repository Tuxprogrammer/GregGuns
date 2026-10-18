package com.gregguns;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.gregguns.core.DevEnvironment;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.network.NetworkCheckHandler;
import cpw.mods.fml.relauncher.Side;

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

    /** The mod instance, needed by Forge's entity registry. */
    @Mod.Instance(MODID)
    public static GregGuns instance;

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

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        proxy.serverStarting(event);
    }

    @Mod.EventHandler
    public void loadComplete(FMLLoadCompleteEvent event) {
        proxy.loadComplete(event);
    }

    /**
     * Decides whether a remote side may connect, replacing FML's default exact-version rule for this mod only. A
     * remote without GregGuns is refused; in the dev workspace any GregGuns version is accepted (the buildscript puts
     * the git state in the version, so separate worktrees never match); in a released jar the version must be equal.
     * See {@link RemoteVersionCheck}. Both sides call it about the other.
     *
     * @param remoteVersions the remote side's mod ids and versions
     * @param side           the side the remote is on
     * @return true to allow the connection
     */
    // Source: FML 1.7.10 cpw.mods.fml.common.network.NetworkCheckHandler: "a method taking (Map<String,String>, Side)
    // and
    // returning boolean; it replaces the default version check for the owning mod".
    @NetworkCheckHandler
    public boolean checkRemote(Map<String, String> remoteVersions, Side side) {
        return RemoteVersionCheck.accepts(DevEnvironment.isActive(), remoteVersions, Tags.VERSION);
    }
}
