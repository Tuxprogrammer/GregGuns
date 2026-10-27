package com.gregguns.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class GregGunsCasingsTest {

    private static final int[] METAS = { GregGunsCasings.WORKS_CASING, GregGunsCasings.RAM_HOUSING,
        GregGunsCasings.REACTION_CASING, GregGunsCasings.GANTRY_CASING, GregGunsCasings.GANTRY_BEAM,
        GregGunsCasings.GANTRY_HOIST, GregGunsCasings.REPAIR_CRADLE, GregGunsCasings.BASTION_IRON,
        GregGunsCasings.BASTION_STEEL, GregGunsCasings.BASTION_ALUMINUM, GregGunsCasings.BASTION_CARBON,
        GregGunsCasings.RADAR_CASING };

    @Test
    void metaConstantsAreUniqueAndCountedByMetaCount() {
        Set<Integer> seen = new HashSet<>();
        for (int meta : METAS) {
            assertTrue(seen.add(meta), "duplicate meta " + meta);
            assertTrue(meta >= 0 && meta < GregGunsCasings.META_COUNT, "meta " + meta);
        }
        assertEquals(GregGunsCasings.META_COUNT, seen.size());
    }

    @Test
    void everyMetaMapsToADistinctPage50Index() {
        Set<Integer> indices = new HashSet<>();
        for (int meta : METAS) {
            int index = GregGunsCasings.textureIndex(meta);
            assertEquals((50 << 7) | meta, index);
            assertEquals(50, index >> 7);
            assertTrue(indices.add(index), "duplicate texture index " + index);
        }
    }
}
