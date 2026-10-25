package com.gregguns.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.gregguns.core.module.ModuleId;
import com.gtnewhorizon.gtnhlib.config.Config;

/** GGS-01-01 and GGS-01-06: config classes load with no Forge and expose their defaults. */
class ConfigDefaultsTest {

    @Test
    void everyModuleFlagHasItsExpectedDefault() {
        for (ModuleId id : ModuleId.values()) {
            // icbm ships off until the ICBM beta gate (docs/ROADMAP-ICBM.md)
            assertEquals(id != ModuleId.ICBM, ModuleFlags.isEnabled(id), "modules." + id.configKey());
        }
    }

    @Test
    void debugKeybindsDefaultToOff() {
        assertFalse(DebugConfig.enableDebugKeybinds);
    }

    @Test
    void moduleFlagsAreExactlyTheModuleIds() {
        Set<String> fields = new HashSet<>();
        for (Field field : ModulesConfig.class.getDeclaredFields()) {
            fields.add(field.getName());
        }
        Set<String> ids = new HashSet<>();
        for (ModuleId id : ModuleId.values()) {
            ids.add(id.configKey());
        }
        assertEquals(ids, fields);
    }

    @Test
    void flagsReflectFieldChanges() {
        boolean original = ModulesConfig.guns;
        try {
            ModulesConfig.guns = !original;
            assertEquals(!original, ModuleFlags.isEnabled(ModuleId.GUNS));
        } finally {
            ModulesConfig.guns = original;
        }
    }

    @Test
    void everyConfigClassTargetsTheSharedGregGunsFile() {
        for (Class<?> clazz : GregGunsConfig.CLASSES) {
            Config config = clazz.getAnnotation(Config.class);
            assertEquals("gregguns", config.modid(), clazz.getName());
            assertEquals("", config.filename(), clazz.getName() + " should share config/gregguns.cfg");
        }
    }

    @Test
    void configFieldsArePublicStaticNonFinal() {
        for (Class<?> clazz : GregGunsConfig.CLASSES) {
            for (Field field : clazz.getDeclaredFields()) {
                int mod = field.getModifiers();
                assertTrue(
                    Modifier.isPublic(mod) && Modifier.isStatic(mod) && !Modifier.isFinal(mod),
                    clazz.getSimpleName() + "." + field.getName());
            }
        }
    }
}
