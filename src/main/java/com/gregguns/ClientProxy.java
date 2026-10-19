package com.gregguns;

import com.gregguns.client.GunsClientModule;
import com.gregguns.client.armor.ArmorClientModule;
import com.gregguns.client.utility.UtilityClientModule;
import com.gregguns.core.module.ModuleManager;

public class ClientProxy extends CommonProxy {

    @Override
    protected void registerModules(ModuleManager manager) {
        super.registerModules(manager);
        // Only this class names com.gregguns.client types, so a dedicated server never loads them.
        manager.registerClient(new GunsClientModule());
        manager.registerClient(new ArmorClientModule());
        manager.registerClient(new UtilityClientModule());
    }
}
