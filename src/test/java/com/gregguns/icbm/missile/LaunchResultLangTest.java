package com.gregguns.icbm.missile;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Properties;

import org.junit.jupiter.api.Test;

/** Every launch outcome has its lang line (ICBM-04-16, 04-17), including the clearance and ticket refusals. */
class LaunchResultLangTest {

    @Test
    void everyResultHasALaunchKey() throws IOException {
        Properties lang = new Properties();
        try (InputStream in = LaunchResultLangTest.class.getResourceAsStream("/assets/gregguns/lang/en_US.lang")) {
            assertNotNull(in);
            lang.load(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8));
        }
        for (LaunchResult r : LaunchResult.values()) {
            String key = "gregguns.icbm.launch." + r.name()
                .toLowerCase(Locale.ROOT);
            assertTrue(lang.containsKey(key), "missing " + key);
        }
        assertNotNull(LaunchResult.valueOf("BLOCKED"));
        assertNotNull(LaunchResult.valueOf("NO_TICKET"));
    }
}
