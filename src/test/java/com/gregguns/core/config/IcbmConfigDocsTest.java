package com.gregguns.core.config;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

/** Every ICBM config key is documented in docs/config.md (a miss in I2, I3 and I4 reviews). */
class IcbmConfigDocsTest {

    @Test
    void everyIcbmKeyAppearsInTheConfigGuide() throws IOException {
        String doc = new String(Files.readAllBytes(Paths.get("docs", "config.md")), StandardCharsets.UTF_8);
        for (Field f : IcbmConfig.class.getDeclaredFields()) {
            if (!Modifier.isPublic(f.getModifiers()) || !Modifier.isStatic(f.getModifiers())) {
                continue;
            }
            String key = "icbm." + f.getName();
            // The four terrain keys share one row: "icbm.terrainDamageT1` to `icbm.terrainDamageT4".
            boolean documented = doc.contains(key) || f.getName()
                .matches("terrainDamageT[1-4]") && doc.contains("icbm.terrainDamageT1` to `icbm.terrainDamageT4");
            assertTrue(documented, key + " is missing from docs/config.md");
        }
    }
}
