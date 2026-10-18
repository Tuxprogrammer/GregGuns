package com.gregguns.armor.slots;

import com.gregguns.GregGuns;
import com.gtnewhorizon.gtnhlib.keybind.IKeyPressedListener;
import com.gtnewhorizon.gtnhlib.keybind.SyncedKeybind;

/**
 * The equipment keybinds, as GTNHLib {@link SyncedKeybind}s: toggle night vision and open the worn Ammo Pouch. GTNHLib
 * sends the key state to the server itself, so the listeners below run on the server and decide from server state. Safe
 * on a dedicated server (a {@code SyncedKeybind} only creates a {@code KeyBinding} on the client).
 *
 * <p>
 * Order matters: {@code SyncedKeybind} numbers keys in construction order and client and server must build the same
 * keys
 * in the same order. They are created only in {@link #register()}, from the armor module's preInit, after the guns
 * module's keys. Append new keys at the end.
 */
// Spec: docs/gregguns-spec/05-armor/baubles-slots.md ("toggle buttons become key bindings");
// docs/gregguns-spec/01-core/keybinds.md
// Source: GTNHLib 0.11.52 SyncedKeybind#createConfigurable and #registerGlobalListener (the listener set is weak, so
// the
// listeners are held in static fields):
// https://github.com/GTNewHorizons/GTNHLib/blob/0.11.52/src/main/java/com/gtnewhorizon/gtnhlib/keybind/SyncedKeybind.java#L110
public final class EquipmentKeybinds {

    /** Lang key of the night-vision key's name. */
    public static final String NIGHT_VISION_NAME_KEY = "key.gregguns.night_vision";
    /** Lang key of the ammo pouch key's name. */
    public static final String AMMO_POUCH_NAME_KEY = "key.gregguns.ammo_pouch";
    /** Lang key of the back device key's name (Glider glide, Anti Gravity Device switch). */
    public static final String BACK_DEVICE_NAME_KEY = "key.gregguns.back_device";
    /** LWJGL key code of N (Keyboard.KEY_N); inlined because LWJGL is absent on a server. */
    public static final int DEFAULT_NIGHT_VISION_KEY = 49;
    /** LWJGL key code of P (Keyboard.KEY_P). */
    public static final int DEFAULT_AMMO_POUCH_KEY = 25;

    private static final IKeyPressedListener NIGHT_VISION_LISTENER = (player, key, isDown) -> {
        if (isDown) {
            SlotEquipment.toggleNightVision(player);
        }
    };
    /** LWJGL key code of J (Keyboard.KEY_J), the Techguns jetpack key. */
    public static final int DEFAULT_BACK_DEVICE_KEY = 36;

    private static final IKeyPressedListener BACK_DEVICE_LISTENER = (player, key, isDown) -> SlotEquipment
        .backKey(player, isDown);
    private static final IKeyPressedListener POUCH_LISTENER = (player, key, isDown) -> {
        if (isDown) {
            AmmoPouch.openWorn(player);
        }
    };

    // Set once in register(); a registry, not gameplay state.
    private static SyncedKeybind nightVision;
    private static SyncedKeybind ammoPouch;
    private static SyncedKeybind backDevice;

    private EquipmentKeybinds() {}

    /** Creates the keys and attaches the server listeners. Call once per side from preInit. */
    public static void register() {
        if (nightVision != null) {
            throw new IllegalStateException("equipment keybinds already registered");
        }
        String category = com.gregguns.gun.server.GunKeybinds.CATEGORY_KEY;
        nightVision = SyncedKeybind.createConfigurable(NIGHT_VISION_NAME_KEY, category, DEFAULT_NIGHT_VISION_KEY);
        nightVision.registerGlobalListener(NIGHT_VISION_LISTENER);
        ammoPouch = SyncedKeybind.createConfigurable(AMMO_POUCH_NAME_KEY, category, DEFAULT_AMMO_POUCH_KEY);
        ammoPouch.registerGlobalListener(POUCH_LISTENER);
        // Appended last: key numbering is construction order (see the class comment).
        backDevice = SyncedKeybind.createConfigurable(BACK_DEVICE_NAME_KEY, category, DEFAULT_BACK_DEVICE_KEY);
        backDevice.registerGlobalListener(BACK_DEVICE_LISTENER);
        GregGuns.LOG.debug("Registered equipment keybinds");
    }

    /** @return the back device key (J by default); null before {@link #register()} */
    public static SyncedKeybind backDevice() {
        return backDevice;
    }
}
