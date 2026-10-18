package com.gregguns.gun;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.gregguns.gun.logic.GunCatalogue;
import com.gregguns.gun.server.GunKeybinds;

/** GGS-01-13: every key, category and gun string has a lang entry. */
class GunKeybindsTest {

    private static Set<String> langKeys() throws IOException {
        Set<String> keys = new HashSet<>();
        try (InputStream in = GunKeybindsTest.class.getResourceAsStream("/assets/gregguns/lang/en_US.lang")) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            for (String line : new String(out.toByteArray(), StandardCharsets.UTF_8).split("\\R")) {
                int eq = line.indexOf('=');
                if (!line.startsWith("#") && eq > 0) {
                    keys.add(line.substring(0, eq));
                }
            }
        }
        return keys;
    }

    @Test
    void everyKeyAndCategoryHasALangEntry() throws IOException {
        Set<String> keys = langKeys();
        assertTrue(keys.contains(GunKeybinds.RELOAD_NAME_KEY));
        assertTrue(keys.contains(GunKeybinds.CATEGORY_KEY));
    }

    @Test
    void everyHudAndTooltipKeyIsDefined() throws IOException {
        Set<String> keys = langKeys();
        for (String key : new String[] { "gregguns.hud.rounds", "gregguns.hud.magazines", "gregguns.hud.reloading",
            "gregguns.gun.tooltip.ammo", "gregguns.gun.tooltip.loaded", "gregguns.gun.tooltip.damage",
            "gregguns.gun.tooltip.shift", "gregguns.gun.tooltip.damage_type", "gregguns.gun.tooltip.range",
            "gregguns.gun.tooltip.spread", "gregguns.gun.tooltip.clipsize", "gregguns.gun.tooltip.reload_time",
            "gregguns.gun.tooltip.armor_pen", "gregguns.gun.tooltip.zoom", "item.gregguns.pistol_9mm.name" }) {
            assertTrue(keys.contains(key), key);
        }
    }

    @Test
    void doesNotReproduceGUN47_thePistolHasNoScopeSoItNeverSendsAZoomIntent() {
        // Techguns sent "zoomed = true" from unscoped guns. ItemGun sends a zoom intent only when hasScope().
        assertFalse(GunCatalogue.PISTOL.hasScope());
    }
}
