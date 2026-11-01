package com.gregguns.recipe.recycling;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * The recycling exclusion list: every entry has a reason, whole-item entries cover every variant, the warhead items are
 * left to their own code, and ordinary items are never excluded by accident. The live check that every GregGuns stack
 * is covered is the {@code recycling.coverage} scenario.
 */
class RecyclingExclusionsTest {

    @Test
    void everyExclusionHasAReason() {
        for (Map.Entry<String, String> entry : RecyclingExclusions.excluded()
            .entrySet()) {
            assertNotNull(entry.getValue(), entry.getKey());
            assertTrue(
                entry.getValue()
                    .trim()
                    .length() >= 20,
                entry.getKey() + ": the reason should say why, not just name a category");
        }
    }

    @Test
    void keysAreCatalogKeys() {
        for (String key : RecyclingExclusions.excluded()
            .keySet()) {
            assertTrue(key.matches("[a-z0-9_]+(@[0-9]+)?"), key + " is not a catalog key");
        }
    }

    @Test
    void aWholeItemEntryCoversEveryVariant() {
        assertNotNull(RecyclingExclusions.reason("coin"));
        assertNotNull(RecyclingExclusions.reason("coin@3"));
        assertNotNull(RecyclingExclusions.reason("irradiated_earth@4"));
    }

    @Test
    void aVariantEntryCoversOnlyThatVariant() {
        assertNotNull(RecyclingExclusions.reason("casings@2"));
        assertNull(RecyclingExclusions.reason("casings"));
        assertNull(RecyclingExclusions.reason("casings@11"));
    }

    @Test
    void familiesAreExcludedByPrefix() {
        assertNotNull(RecyclingExclusions.reason("launcher_warhead_he"));
        assertNotNull(RecyclingExclusions.reason("launcher_warhead_grenade_cluster"));
        assertNotNull(RecyclingExclusions.reason("missile_chip_t1"));
        assertNotNull(RecyclingExclusions.reason("missile_schematic_t3"));
        for (String reason : RecyclingExclusions.excludedPrefixes()
            .values()) {
            assertTrue(reason.length() >= 20, reason);
        }
    }

    @Test
    void warheadItemsAreDelegatedNotExcluded() {
        for (String key : new String[] { "warhead_t1", "warhead_t5", "inert_warhead_t1", "inert_warhead_t3" }) {
            assertNull(RecyclingExclusions.reason(key), key);
            assertNotNull(RecyclingExclusions.delegatedReason(key), key);
        }
        // The technical filler block shares the prefix but is excluded, not delegated.
        assertNull(RecyclingExclusions.delegatedReason("warhead_filler"));
        assertNotNull(RecyclingExclusions.reason("warhead_filler"));
    }

    @Test
    void ordinaryItemsAreNeitherExcludedNorDelegated() {
        for (String key : new String[] { "pistol_9mm", "part_barrel_iron", "sam", "mte:20109", "casings@11" }) {
            assertNull(RecyclingExclusions.reason(key), key);
            assertNull(RecyclingExclusions.delegatedReason(key), key);
        }
    }

    @Test
    void noEntryIsDelegatedToo() {
        for (String key : RecyclingExclusions.excluded()
            .keySet()) {
            assertFalse(RecyclingExclusions.delegatedReason(key) != null, key + " is both excluded and delegated");
        }
    }
}
