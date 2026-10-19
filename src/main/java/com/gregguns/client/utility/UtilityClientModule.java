package com.gregguns.client.utility;

import com.gregguns.core.module.ClientModule;
import com.gregguns.core.module.ModuleId;
import com.gregguns.utility.turret.entity.TurretEntity;

import cpw.mods.fml.client.registry.RenderingRegistry;

/**
 * Client half of the {@code modules.utility} module: the placeholder renderers of the M6 content. Each M6 chunk adds
 * one line to {@link #init()} for its own renderers. Client only, reached through {@code ClientProxy}.
 */
// Spec: docs/ROADMAP.md (M6)
public final class UtilityClientModule implements ClientModule {

    @Override
    public ModuleId id() {
        return ModuleId.UTILITY;
    }

    @Override
    public void init() {
        RenderingRegistry.registerEntityRenderingHandler(TurretEntity.class, new TurretRenderer());
    }
}
