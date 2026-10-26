package com.gregguns;

import com.gregguns.client.GunsClientModule;
import com.gregguns.client.armor.ArmorClientModule;
import com.gregguns.client.utility.UtilityClientModule;
import com.gregguns.core.module.ModuleManager;

public class ClientProxy extends CommonProxy {

    @Override
    public void init(cpw.mods.fml.common.event.FMLInitializationEvent event) {
        super.init(event);
        com.gregguns.client.ModuleClientHooks hooks = new com.gregguns.client.ModuleClientHooks();
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(hooks);
        cpw.mods.fml.common.FMLCommonHandler.instance()
            .bus()
            .register(hooks);
    }

    @Override
    public void moduleStateChanged() {
        com.gregguns.client.ModuleClientHooks.refreshNei();
    }

    @Override
    protected void registerModules(ModuleManager manager) {
        super.registerModules(manager);
        // Only this class names com.gregguns.client types, so a dedicated server never loads them.
        manager.registerClient(new GunsClientModule());
        manager.registerClient(new ArmorClientModule());
        manager.registerClient(new UtilityClientModule());
        manager.registerClient(new com.gregguns.client.npc.NpcClientModule());
        manager.registerClient(new com.gregguns.client.icbm.IcbmClientModule());
    }
}
