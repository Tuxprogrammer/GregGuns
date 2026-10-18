package com.gregguns.client;

import com.gregguns.core.module.ClientModule;
import com.gregguns.core.module.ModuleId;

/**
 * Client half of the {@code modules.guns} module: renderers, HUD, keybinds and sounds for guns, projectiles and ammo.
 * Runs only when the guns module is enabled. Client only.
 */
// Spec: docs/ROADMAP.md (M1, M2)
public final class GunsClientModule implements ClientModule {

    @Override
    public ModuleId id() {
        return ModuleId.GUNS;
    }

    @Override
    public void init() {
        // Renderers, HUD overlays and keybinds: one call per area.
    }
}
