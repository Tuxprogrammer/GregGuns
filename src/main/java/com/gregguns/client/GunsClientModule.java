package com.gregguns.client;

import com.gregguns.client.gun.GunClientSetup;
import com.gregguns.client.sound.ClientGunSounds;
import com.gregguns.core.module.ClientModule;
import com.gregguns.core.module.ModuleId;

/**
 * Client half of the {@code modules.guns} module: renderers, HUD, keybinds and sounds for guns, projectiles and ammo.
 * Runs only when the guns module is enabled. Client only.
 */
// Spec: docs/ROADMAP.md (M1, M2)
public final class GunsClientModule implements ClientModule {

    private static final ClientGunSounds SOUNDS = new ClientGunSounds();

    /** @return the shooter's predicted gun sounds; client only, reached through the proxy */
    public static ClientGunSounds sounds() {
        return SOUNDS;
    }

    @Override
    public ModuleId id() {
        return ModuleId.GUNS;
    }

    @Override
    public void init() {
        // Renderers, HUD overlays and keybinds: one call per area.
        GunClientSetup.init();
    }
}
