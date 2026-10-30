package com.gregguns.icbm.control;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Properties;

import org.junit.jupiter.api.Test;

/**
 * Owner decision 2026-10-30 (D284 in the ICBM decisions): the Radar Gun is the way to get a target, and data sticks
 * stay a silent feature. Nothing the Launch Control shows a player (GUI text, tooltips, slot hint, quests) names a data
 * stick, and the empty-target text points at the Radar Gun.
 */
class LaunchControlNudgeTextTest {

    private static final String[] PLAYER_KEYS = { "gregguns.icbm.control.gui.target_none",
        "gregguns.icbm.control.gui.slot.hint", "gregguns.icbm.control.gui.slot.tip",
        "gregguns.icbm.control.gui.load.tip", "gregguns.icbm.control.gui.save.tip",
        "gregguns.icbm.control.gui.stick_missing", "gregguns.icbm.control.tooltip.target",
        "gregguns.icbm.control.stick.no_stick", "gregguns.icbm.control.stick.empty_stick",
        "gregguns.icbm.control.stick.loaded", "gregguns.icbm.control.stick.saved",
        "gregguns.icbm.control.stick.wrong_dimension" };

    private static Properties lang() throws IOException {
        Properties p = new Properties();
        try (
            InputStream in = LaunchControlNudgeTextTest.class.getResourceAsStream("/assets/gregguns/lang/en_US.lang")) {
            assertNotNull(in);
            p.load(new java.io.InputStreamReader(in, StandardCharsets.UTF_8));
        }
        return p;
    }

    @Test
    void launchControlTextNeverNamesADataStick() throws IOException {
        Properties p = lang();
        for (String key : PLAYER_KEYS) {
            String v = p.getProperty(key);
            assertNotNull(v, key);
            assertFalse(
                v.toLowerCase(Locale.ROOT)
                    .contains("stick"),
                key + " advertises data sticks");
        }
        for (String key : p.stringPropertyNames()) {
            if (key.startsWith("gregguns.icbm.control.tooltip") || key.startsWith("gregguns.icbm.control.gui.")) {
                assertFalse(
                    p.getProperty(key)
                        .toLowerCase(Locale.ROOT)
                        .contains("data stick"),
                    key);
            }
        }
    }

    @Test
    void theEmptyTargetStateAndTheTooltipPointAtTheRadarGun() throws IOException {
        Properties p = lang();
        for (String key : new String[] { "gregguns.icbm.control.gui.target_none", "gregguns.icbm.control.gui.slot.hint",
            "gregguns.icbm.control.gui.slot.tip", "gregguns.icbm.control.tooltip.target" }) {
            assertTrue(
                p.getProperty(key)
                    .contains("Radar Gun"),
                key);
        }
    }

    @Test
    void theEmptyTargetLineFitsTheScreenRow() throws IOException {
        String text = lang().getProperty("gregguns.icbm.control.gui.target_none");
        int w = 0;
        for (char c : text.toCharArray()) {
            w += ControlStatusTextTest.px(c);
        }
        assertTrue(w <= 176 - 14 - 8, "the line is " + w + " px");
    }
}
