package com.gregguns.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import com.gtnewhorizon.gtnhlib.config.Config;

/** GGS-01-02, GGS-01-03 and GGS-01-04 as reflective rules over every registered config class. */
class ConfigRulesTest {

    /** Words that must never be a whole camelCase or underscore token in a key (GGS-01-02). */
    private static final List<String> BANNED_TOKENS = Arrays
        .asList("ore", "ores", "ingot", "ingots", "nugget", "nuggets", "drill", "drills", "oil", "datawatcher");
    /** Banned fragments, checked against the lower-cased key with separators removed. */
    private static final List<String> BANNED_FRAGMENTS = Arrays
        .asList("oregen", "datawatcher", "tabid", "guitab", "versionchecker", "versioncheck", "oilhole", "orecluster");

    /** Fields expected to carry Config.Sync, as "Class.field". Update this with every synced field you add. */
    private static final Set<String> EXPECTED_SYNCED = new TreeSet<>();

    private static Set<String> tokens(String key) {
        Set<String> result = new HashSet<>();
        for (String part : key.split("(?<=[a-z0-9])(?=[A-Z])|[^A-Za-z0-9]+")) {
            result.add(part.toLowerCase(Locale.ROOT));
        }
        return result;
    }

    private static boolean isBanned(String key) {
        String squashed = key.toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]", "");
        for (String fragment : BANNED_FRAGMENTS) {
            if (squashed.contains(fragment)) {
                return true;
            }
        }
        Set<String> parts = tokens(key);
        for (String banned : BANNED_TOKENS) {
            if (parts.contains(banned)) {
                return true;
            }
        }
        return false;
    }

    @Test
    void bannedDetectorCatchesTheTechgunsKeys() {
        for (String bad : new String[] { "doOreGen", "addSteelIngot", "addIronNuggets", "oreDrillRFMultiplier",
            "spawnOilHoles", "DataWatcherID_FaceSlot", "TechgunsGUI_TabID", "EnableVersionChecker" }) {
            assertTrue(isBanned(bad), bad);
        }
        for (String fine : new String[] { "guns", "energyWeapons", "enableDebugKeybinds", "store", "more" }) {
            assertFalse(isBanned(fine), fine);
        }
    }

    @Test
    void noBannedKeyNames() {
        for (Class<?> clazz : GregGunsConfig.CLASSES) {
            Config config = clazz.getAnnotation(Config.class);
            assertFalse(isBanned(config.category()), "category " + config.category());
            for (Field field : clazz.getDeclaredFields()) {
                assertFalse(isBanned(field.getName()), clazz.getSimpleName() + "." + field.getName());
                Config.Name name = field.getAnnotation(Config.Name.class);
                if (name != null) {
                    assertFalse(isBanned(name.value()), "@Name " + name.value());
                }
            }
        }
    }

    @Test
    void everyFieldHasANonEmptyComment() {
        for (Class<?> clazz : GregGunsConfig.CLASSES) {
            for (Field field : clazz.getDeclaredFields()) {
                Config.Comment comment = field.getAnnotation(Config.Comment.class);
                String where = clazz.getSimpleName() + "." + field.getName();
                assertNotNull(comment, where + " needs @Config.Comment");
                assertTrue(
                    comment.value().length > 0 && !comment.value()[0].trim()
                        .isEmpty(),
                    where);
            }
        }
    }

    @Test
    void everyNumericFieldHasARange() {
        for (Class<?> clazz : GregGunsConfig.CLASSES) {
            for (Field field : clazz.getDeclaredFields()) {
                Class<?> type = field.getType();
                boolean numeric = type == int.class || type == long.class
                    || type == float.class
                    || type == double.class;
                if (!numeric) {
                    continue;
                }
                boolean ranged = field.isAnnotationPresent(Config.RangeInt.class)
                    || field.isAnnotationPresent(Config.RangeFloat.class)
                    || field.isAnnotationPresent(Config.RangeDouble.class);
                assertTrue(ranged, clazz.getSimpleName() + "." + field.getName() + " needs a @Config.Range*");
            }
        }
    }

    @Test
    void commentStatesTheDefaultForEveryField() {
        // CONTRIBUTING.md: every option documents its default; the comment is where players see it.
        for (Class<?> clazz : GregGunsConfig.CLASSES) {
            for (Field field : clazz.getDeclaredFields()) {
                String joined = String.join(
                    " ",
                    field.getAnnotation(Config.Comment.class)
                        .value());
                assertTrue(joined.contains("Default:"), clazz.getSimpleName() + "." + field.getName());
            }
        }
    }

    @Test
    void syncedFieldsAreExactlyTheExpectedOnes() {
        Set<String> actual = new TreeSet<>();
        for (Class<?> clazz : GregGunsConfig.CLASSES) {
            boolean classSynced = clazz.isAnnotationPresent(Config.Sync.class);
            for (Field field : clazz.getDeclaredFields()) {
                Config.Sync sync = field.getAnnotation(Config.Sync.class);
                boolean synced = sync != null ? sync.value() : classSynced;
                if (synced) {
                    actual.add(clazz.getSimpleName() + "." + field.getName());
                }
            }
        }
        assertEquals(EXPECTED_SYNCED, actual);
    }
}
