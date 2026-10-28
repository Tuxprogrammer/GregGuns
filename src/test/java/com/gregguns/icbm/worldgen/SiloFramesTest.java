package com.gregguns.icbm.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

/** Armory frames draw guns from the weapon tables at or below the ceiling, the same way every time. */
class SiloFramesTest {

    /** Stand-in for the item test: the weapon tables name guns as bare names; anything with a part word is not one. */
    private static final Predicate<String> GUN = ref -> !ref.contains("ammo") && !ref.startsWith("mag_")
        && !ref.contains("tank_")
        && !ref.contains("grenade")
        && !ref.contains("stielgranate");

    @Test
    void poolsExistUpToTheCeilingTableOnly() {
        assertFalse(
            SiloFrames.pool(SiloCeiling.EV, 1, GUN)
                .isEmpty());
        assertTrue(
            SiloFrames.pool(SiloCeiling.EV, SiloCeiling.tableTier(SiloCeiling.EV) + 1, GUN)
                .isEmpty());
    }

    @Test
    void drawsAreStableAndNeverAboveTheCeilingTable() {
        for (int ceiling = SiloCeiling.EV; ceiling <= SiloCeiling.UMV; ceiling++) {
            Set<String> allowed = new HashSet<>();
            for (int t = 1; t <= SiloCeiling.tableTier(ceiling); t++) allowed.addAll(SiloFrames.pool(ceiling, t, GUN));
            for (int i = 0; i < 200; i++) {
                String a = SiloFrames.gunFor(ceiling, 77L, i, 2 + i % 2, i * 3, GUN);
                assertNotNull(a);
                assertEquals(a, SiloFrames.gunFor(ceiling, 77L, i, 2 + i % 2, i * 3, GUN));
                assertTrue(allowed.contains(a), a + " is above the ceiling " + ceiling);
            }
        }
    }

    @Test
    void anArmoryShowsMoreThanOneGun() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 60; i++) seen.add(SiloFrames.gunFor(SiloCeiling.EV, 3L, i, 2, i, GUN));
        assertTrue(seen.size() >= 4, "variety: " + seen);
        List<String> top = SiloFrames.pool(SiloCeiling.EV, 4, GUN);
        assertFalse(top.isEmpty());
    }
}
