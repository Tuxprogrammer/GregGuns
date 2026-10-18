package com.gregguns.gun.server;

import com.gregguns.GregGuns;
import com.gtnewhorizon.gtnhlib.keybind.IKeyPressedListener;
import com.gtnewhorizon.gtnhlib.keybind.SyncedKeybind;

/**
 * The gun keybinds, as GTNHLib {@link SyncedKeybind}s. The class is safe to load on a dedicated server: the
 * constructor of {@code SyncedKeybind} only creates a {@code KeyBinding} on the client.
 *
 * <p>
 * Only the reload key exists in M1. Pressing it sends no GregGuns packet: GTNHLib's own {@code PacketKeyDown} tells the
 * server, and the listener below calls {@link ServerGunHandler#onReloadKey}, which decides everything from server
 * state (GGS-01-14). The client also reads the key to predict the animation.
 *
 * <p>
 * Order matters: {@code SyncedKeybind} numbers keys in construction order, and client and server must construct the
 * same keys in the same order. Create them only in {@link #register()}, from the guns module's preInit, on both sides.
 * Append new keys at the end.
 *
 * <p>
 * Spec: docs/gregguns-spec/01-core/keybinds.md (GGS-01-13, GGS-01-14, GGS-01-17).
 * Source: GTNHLib 0.11.52,
 * https://github.com/GTNewHorizons/GTNHLib/blob/0.11.52/src/main/java/com/gtnewhorizon/gtnhlib/keybind/SyncedKeybind.java#L110
 * (createConfigurable) and #L168 (registerGlobalListener; the listener set is weak, so the listener is held in a static
 * field).
 */
public final class GunKeybinds {

    /** Lang key of the reload key's name. */
    public static final String RELOAD_NAME_KEY = "key.gregguns.reload";
    /** Lang key of the GregGuns controls category. */
    public static final String CATEGORY_KEY = "key.categories.gregguns";
    /** LWJGL key code of R (Keyboard.KEY_R); the constant is inlined because LWJGL is absent on a server. */
    public static final int DEFAULT_RELOAD_KEY = 19;

    // GTNHLib keeps global listeners in a weak set, so this strong reference keeps the listener alive.
    private static final IKeyPressedListener RELOAD_LISTENER = (player, key, isDown) -> {
        if (isDown) {
            ServerGunHandler.onReloadKey(player);
        }
    };

    // Set once in register(); a registry, not gameplay state.
    private static SyncedKeybind reload;

    private GunKeybinds() {}

    /** Creates the keys and attaches the server listeners. Call once per side from preInit. */
    public static void register() {
        if (reload != null) {
            throw new IllegalStateException("gun keybinds already registered");
        }
        reload = SyncedKeybind.createConfigurable(RELOAD_NAME_KEY, CATEGORY_KEY, DEFAULT_RELOAD_KEY);
        reload.registerGlobalListener(RELOAD_LISTENER);
        GregGuns.LOG.debug("Registered gun keybinds");
    }

    /** @return the reload key; null when the guns module is off */
    public static SyncedKeybind reload() {
        return reload;
    }
}
