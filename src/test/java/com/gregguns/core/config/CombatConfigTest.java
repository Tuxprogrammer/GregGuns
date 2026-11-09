package com.gregguns.core.config;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

/**
 * The combat category's shipped defaults are the owner's 0.4.1 asks (design 0027, D357 to D360), and every key is in
 * the config guide.
 */
class CombatConfigTest {

    @Test
    void knockbackIsOffByDefault() {
        assertFalse(CombatConfig.knockback, "D357: knockback off by default");
        assertEquals(1.0F, CombatConfig.knockbackMultiplier);
    }

    @Test
    void npcTierDamageDefaultsAreTheDesignTable() {
        float[] table = { CombatConfig.npcDamageSteam, CombatConfig.npcDamageLV, CombatConfig.npcDamageMV,
            CombatConfig.npcDamageHV, CombatConfig.npcDamageEV, CombatConfig.npcDamageIV, CombatConfig.npcDamageLuV,
            CombatConfig.npcDamageZPM };
        assertArrayEquals(new float[] { 1.0F, 1.0F, 1.0F, 0.85F, 0.7F, 0.55F, 0.45F, 0.4F }, table);
        assertEquals(0.75F, CombatConfig.npcEliteDamage);
    }

    @Test
    void npcAccuracyDefaultsKeepFiveBlocksFree() {
        assertEquals(5.0F, CombatConfig.npcAccuracyRange);
        assertEquals(0.12F, CombatConfig.npcSpreadPerBlock);
        assertEquals(4.0F, CombatConfig.npcMaxSpread);
    }

    @Test
    void npcPenetrationAgainstPlayersIsHalvedByDefault() {
        assertEquals(0.5F, DamageFactorsConfig.armorPenetrationMultiplierNPCtoPlayer, "D358");
    }

    @Test
    void everyCombatKeyAppearsInTheConfigGuide() throws IOException {
        String doc = new String(Files.readAllBytes(Paths.get("docs", "config.md")), StandardCharsets.UTF_8);
        for (Field f : CombatConfig.class.getDeclaredFields()) {
            if (Modifier.isPublic(f.getModifiers()) && Modifier.isStatic(f.getModifiers())) {
                assertTrue(
                    doc.contains("`combat." + f.getName() + "`"),
                    "combat." + f.getName() + " is not documented");
            }
        }
        assertTrue(doc.contains("Make combat harder"), "the guide has the hardness section");
    }
}
