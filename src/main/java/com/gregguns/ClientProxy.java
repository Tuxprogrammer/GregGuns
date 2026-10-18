package com.gregguns;

import com.gregguns.client.GunsClientModule;
import com.gregguns.core.module.ModuleManager;

public class ClientProxy extends CommonProxy {

    @Override
    protected void registerModules(ModuleManager manager) {
        super.registerModules(manager);
        // Only this class names com.gregguns.client types, so a dedicated server never loads them.
        manager.registerClient(new GunsClientModule());
    }
}
