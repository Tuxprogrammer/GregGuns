package com.gregguns;

import com.gregguns.core.module.ModuleManager;

public class ClientProxy extends CommonProxy {

    @Override
    protected void registerModules(ModuleManager manager) {
        super.registerModules(manager);
        // Client halves go here as `manager.registerClient(new GunsClientModule())`. Only this class names
        // com.gregguns.client types, so a dedicated server never loads them.
    }
}
