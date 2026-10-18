package com.gregguns.client;

import com.gregguns.client.autotest.ClientAutotest;
import com.gregguns.client.gun.GunClientSetup;
import com.gregguns.client.projectile.ProjectileRenderers;
import com.gregguns.client.render.GunAnimations;
import com.gregguns.client.render.GunRenderClient;
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
    private static final GunAnimations ANIMATIONS = new GunAnimations();

    /** @return the shooter's predicted gun sounds; client only, reached through the proxy */
    public static ClientGunSounds sounds() {
        return SOUNDS;
    }

    /**
     * @return the gun animation hooks: {@code onShotFired}, {@code onShotEcho}, {@code onReloadStarted},
     *         {@code onReloadCanceled}, {@code setZoom}; client only, reached through the proxy
     */
    public static GunAnimations animations() {
        return ANIMATIONS;
    }

    @Override
    public ModuleId id() {
        return ModuleId.GUNS;
    }

    @Override
    public void init() {
        // Renderers, HUD overlays and keybinds: one call per area.
        ProjectileRenderers.register();
        GunRenderClient.init(ANIMATIONS);
        // Dev-only and inert unless -Dgregguns.autotest is set: see docs/testing/README.md.
        ClientAutotest.init(GunClientSetup.init());
    }
}
