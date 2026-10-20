package com.gregguns.worldgen.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

/** GGS-11-28, WGEN-23: a bad structure is disabled and logged; the others still load and nothing throws. */
class LayoutRegistryTest {

    @Test
    void goodStructureLoadsAndEachBadOneIsDisabledWithAnError() {
        List<String> errors = new ArrayList<>();
        LayoutRegistry reg = LayoutRegistry.load(
            new LayoutRegistry.ClasspathSource(LayoutRegistry.ClasspathSource.STRUCTURES),
            Arrays.asList("test_hut", "test_bad", "test_missing", "test_nokey"),
            TestKeys.chiselAbsent(new TestKeys.Blocks()),
            errors::add);

        assertTrue(reg.isEnabled("test_hut"));
        assertNotNull(reg.get("test_hut"));

        assertNull(reg.get("test_bad"), "truncated file");
        assertTrue(reg.disabledReason("test_bad")
            .contains("1 row(s), expected 3"));
        assertNull(reg.get("test_missing"), "missing file");
        assertTrue(reg.disabledReason("test_missing")
            .contains("not found"));
        assertNull(reg.get("test_nokey"), "unknown palette key");
        assertTrue(reg.disabledReason("test_nokey")
            .contains("no_such_key"));

        assertEquals(3, errors.size());
        assertFalse(reg.isEnabled("never_asked"));
        assertNull(reg.disabledReason("test_hut"));
    }

    @Test
    void headerIdMustMatchTheRequestedName() {
        List<String> errors = new ArrayList<>();
        LayoutRegistry.Source src = id -> "gregguns-layout 1\nid: other\nsize: 1 1 1\npalette:\nA = air\nlayers:\nlayer 0\nA\n";
        LayoutRegistry reg = LayoutRegistry
            .load(src, Arrays.asList("wanted"), TestKeys.chiselAbsent(new TestKeys.Blocks()), errors::add);
        assertFalse(reg.isEnabled("wanted"));
        assertEquals(1, errors.size());
        assertTrue(errors.get(0)
            .contains("does not match"));
    }

    @Test
    void anUnexpectedRuntimeFailureStillOnlyDisablesThatStructure() {
        List<String> errors = new ArrayList<>();
        LayoutRegistry.Source src = id -> {
            if (id.equals("boom")) {
                throw new IllegalStateException("source exploded");
            }
            throw new IOException("nope");
        };
        LayoutRegistry reg = LayoutRegistry
            .load(src, Arrays.asList("boom", "io"), TestKeys.chiselAbsent(new TestKeys.Blocks()), errors::add);
        assertEquals(2, errors.size());
        assertNull(reg.get("boom"));
    }
}
