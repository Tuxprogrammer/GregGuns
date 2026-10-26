package com.gregguns.compat;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.gregguns.entity.npc.NpcMob;

/** The pure parts of the in-memory pack-fit hooks. */
class PackFitTest {

    @Test
    void mergeKeepsExistingOrderAndAppendsNewOnce() {
        String[] out = BackhandCompat
            .merge(new String[] { "a:x", "b:y" }, Arrays.asList("b:y", "gregguns:m4", "gregguns:m4"));
        assertArrayEquals(new String[] { "a:x", "b:y", "gregguns:m4" }, out);
    }

    @Test
    void mergeToleratesNullAndIsIdempotent() {
        String[] once = BackhandCompat.merge(null, Arrays.asList("gregguns:ak47"));
        assertArrayEquals(once, BackhandCompat.merge(once, Arrays.asList("gregguns:ak47")));
    }

    @Test
    void applyDoesNotThrowWithoutBackhand() {
        assertFalse(BackhandCompat.apply(Arrays.asList("gregguns:ak47")));
    }

    @Test
    void mobAspectTableNamesExistAndAreEvenPairs() {
        Set<String> npcs = new HashSet<>();
        for (NpcMob m : NpcMob.values()) {
            npcs.add(m.registryName());
        }
        for (java.util.Map.Entry<String, Object[]> e : MobAspects.table()
            .entrySet()) {
            assertTrue(npcs.contains(e.getKey()), e.getKey());
            assertEquals(0, e.getValue().length % 2, e.getKey());
            for (int i = 0; i < e.getValue().length; i += 2) {
                assertTrue(e.getValue()[i] instanceof String && e.getValue()[i + 1] instanceof Integer, e.getKey());
            }
        }
    }

    @Test
    void mobAspectApplyDoesNotThrowWithoutThaumcraft() {
        assertEquals(0, MobAspects.apply());
    }

    @Test
    void listHooksAreInertWithoutTheirMods() {
        assertFalse(PackListHooks.angerModButcherItems());
        assertFalse(PackListHooks.mobsInfoBlacklist());
    }

    /** Stand-in for another mod's config class: a public static String[] that the hook extends. */
    public static class FakeConfig {

        public static String[] list = { "x" };
    }

    @Test
    void extendAppendsToAPublicStaticArray() throws Exception {
        assertTrue(BackhandCompat.extend(FakeConfig.class, "list", Arrays.asList("gregguns.", "x")));
        assertArrayEquals(new String[] { "x", "gregguns." }, FakeConfig.list);
    }
}
