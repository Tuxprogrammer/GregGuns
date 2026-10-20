package com.gregguns.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.gregguns.armor.slots.EquipmentKeybinds;
import com.gregguns.gun.server.GunKeybinds;

/**
 * Keeps the GregGuns default key map clear of the GTNH pack's keys (design record 0013, M0 audit D-3). The pack list in
 * {@code pack-keys.csv} is codes and names only, taken from a real GTNH 2.9.0-RC-2 client's options.txt.
 */
// Spec: docs/gregguns-spec/01-core/keybinds.md; docs/design/0013-default-key-map.md
class DefaultKeyMapTest {

    /** The GregGuns defaults, name to LWJGL key code. Add every new key here. */
    private static final int[] DEFAULTS = { GunKeybinds.DEFAULT_RELOAD_KEY, EquipmentKeybinds.DEFAULT_NIGHT_VISION_KEY,
        EquipmentKeybinds.DEFAULT_AMMO_POUCH_KEY, EquipmentKeybinds.DEFAULT_BACK_DEVICE_KEY };

    /** Codes where a known, accepted clash exists (reload R: the only same-key in-world binding ships unbound). */
    private static final Set<Integer> ACCEPTED_NON_WORLD = new HashSet<>();
    static {
        ACCEPTED_NON_WORLD.add(GunKeybinds.DEFAULT_RELOAD_KEY);
    }

    @Test
    void defaultsAreDistinctFromEachOther() {
        Set<Integer> seen = new HashSet<>();
        for (int code : DEFAULTS) {
            assertTrue(seen.add(code), "duplicate GregGuns default key code " + code);
        }
    }

    @Test
    void defaultsAvoidPackInWorldKeys() throws IOException {
        for (String[] row : packKeys()) {
            int code = Integer.parseInt(row[0]);
            for (int mine : DEFAULTS) {
                if (mine == code && "world".equals(row[2])) {
                    throw new AssertionError("GregGuns default " + code + " clashes with in-world pack key " + row[1]);
                }
            }
        }
    }

    @Test
    void onlyAcceptedKeysSharePackKeysOutsideTheWorld() throws IOException {
        for (String[] row : packKeys()) {
            int code = Integer.parseInt(row[0]);
            for (int mine : DEFAULTS) {
                if (mine == code && !"world".equals(row[2])) {
                    assertTrue(
                        ACCEPTED_NON_WORLD.contains(code),
                        "undocumented shared key " + code + " with " + row[1]);
                }
            }
        }
    }

    @Test
    void packListIsLoaded() throws IOException {
        assertTrue(packKeys().size() > 50, "pack key list looks empty");
        assertEquals(36, Integer.parseInt(find("key.journeymap.map_toggle")[0]));
    }

    private static String[] find(String name) throws IOException {
        for (String[] row : packKeys()) {
            if (row[1].equals(name)) {
                return row;
            }
        }
        throw new AssertionError("missing " + name);
    }

    private static List<String[]> packKeys() throws IOException {
        List<String[]> rows = new ArrayList<>();
        try (InputStream in = DefaultKeyMapTest.class.getResourceAsStream("/pack-keys.csv")) {
            if (in == null) {
                throw new AssertionError("pack-keys.csv missing from test resources");
            }
            BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while ((line = r.readLine()) != null) {
                if (line.isEmpty() || line.charAt(0) == '#') {
                    continue;
                }
                String[] parts = line.split(",", 3);
                rows.add(parts);
            }
        }
        return rows;
    }
}
