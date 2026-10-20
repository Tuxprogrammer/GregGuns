package com.gregguns.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.gregguns.core.config.WorldgenConfig;

/** GGS-11-07, GGS-11-12 and GGS-11-15: the gates are pure and their defaults are the documented ones. */
class WorldgenGateTest {

    private static final int[] DEFAULT_DIMS = { 0 };
    private static final String[] DEFAULT_TYPES = { "default", "RWG", "largeBiomes", "amplified" };

    @Test
    void defaultDimensionListAllowsOnlyTheOverworld() {
        assertEquals(1, WorldgenConfig.structureDimensions.length);
        assertEquals(0, WorldgenConfig.structureDimensions[0]);
        assertTrue(WorldgenGate.dimensionAllowed(DEFAULT_DIMS, 0));
        for (int dim : new int[] { -1, 1, 7, 180, 27, 28 }) {
            assertFalse(WorldgenGate.dimensionAllowed(DEFAULT_DIMS, dim), "dim " + dim);
        }
    }

    @Test
    void dimensionListIsTheOnlyAuthority() {
        // No literal: a pack that lists the Nether gets the Nether and loses the Overworld.
        int[] nether = { -1 };
        assertTrue(WorldgenGate.dimensionAllowed(nether, -1));
        assertFalse(WorldgenGate.dimensionAllowed(nether, 0));
        assertFalse(WorldgenGate.dimensionAllowed(new int[0], 0));
        assertFalse(WorldgenGate.dimensionAllowed(null, 0));
        assertTrue(WorldgenGate.dimensionAllowed(new int[] { 0, 7, 180 }, 180));
    }

    @Test
    void defaultWorldTypeListMatchesTheDocumentedFour() {
        assertEquals(Arrays.asList(DEFAULT_TYPES), Arrays.asList(WorldgenConfig.structureWorldTypes));
        for (String type : DEFAULT_TYPES) {
            assertTrue(WorldgenGate.worldTypeAllowed(DEFAULT_TYPES, type), type);
        }
    }

    @Test
    void flatAndUnknownWorldTypesAreOut() {
        assertFalse(WorldgenGate.worldTypeAllowed(DEFAULT_TYPES, "flat"));
        assertFalse(WorldgenGate.worldTypeAllowed(DEFAULT_TYPES, "default_1_1"));
        assertFalse(WorldgenGate.worldTypeAllowed(DEFAULT_TYPES, "SomeModdedType"));
        assertFalse(WorldgenGate.worldTypeAllowed(DEFAULT_TYPES, ""));
        assertFalse(WorldgenGate.worldTypeAllowed(DEFAULT_TYPES, "  "));
        assertFalse(WorldgenGate.worldTypeAllowed(DEFAULT_TYPES, null));
        assertFalse(WorldgenGate.worldTypeAllowed(null, "default"));
        assertFalse(WorldgenGate.worldTypeAllowed(new String[0], "default"));
    }

    @Test
    void worldTypeMatchIgnoresCaseAndBlanks() {
        assertTrue(WorldgenGate.worldTypeAllowed(DEFAULT_TYPES, "rwg"));
        assertTrue(WorldgenGate.worldTypeAllowed(DEFAULT_TYPES, " LARGEBIOMES "));
        assertTrue(WorldgenGate.worldTypeAllowed(new String[] { " flat ", null }, "FLAT"));
    }

    @Test
    void placementNeedsBothLists() {
        assertTrue(WorldgenGate.placementAllowed(DEFAULT_DIMS, DEFAULT_TYPES, 0, "RWG"));
        assertFalse(WorldgenGate.placementAllowed(DEFAULT_DIMS, DEFAULT_TYPES, -1, "RWG"));
        assertFalse(WorldgenGate.placementAllowed(DEFAULT_DIMS, DEFAULT_TYPES, 0, "flat"));
        assertFalse(WorldgenGate.placementAllowed(DEFAULT_DIMS, DEFAULT_TYPES, 7, "flat"));
    }

    @Test
    void generatorRegistersOnlyWithModuleAndSpawnStructures() {
        assertTrue(WorldgenGate.shouldRegisterGenerator(true, true));
        assertFalse(WorldgenGate.shouldRegisterGenerator(true, false));
        assertFalse(WorldgenGate.shouldRegisterGenerator(false, true));
        assertFalse(WorldgenGate.shouldRegisterGenerator(false, false));
        assertTrue(WorldgenConfig.spawnStructures);
    }

    @Test
    void spacingDefaultsAreSixteenThirtyTwoSixtyFour() {
        assertEquals(16, WorldgenConfig.spacingSmall);
        assertEquals(32, WorldgenConfig.spacingMedium);
        assertEquals(64, WorldgenConfig.spacingBig);
    }

    @Test
    void onlyTheAlienBugNestIsOffByDefault() {
        for (StructureId s : StructureId.values()) {
            assertEquals(s != StructureId.ALIEN_BUG_NEST, s.enabledByDefault(), s.id());
            assertEquals(s.enabledByDefault(), WorldgenSettings.enabled(s), s.id());
        }
    }

    @Test
    void ceilingsAreLvForSmallAndMvForBunkerBaseAndShips() {
        for (StructureId s : StructureId.values()) {
            assertEquals(s.defaultCeiling(), WorldgenSettings.lootTierCeiling(s), s.id());
            assertEquals(1, WorldgenSettings.lootTier(s), s.id());
            assertTrue(
                WorldgenSettings.biomeBlacklist(s)
                    .isEmpty(),
                s.id());
        }
        assertEquals(2, StructureId.AIRCRAFT_CARRIER.defaultCeiling());
        assertEquals(2, StructureId.SUBMARINE.defaultCeiling());
        assertEquals(2, StructureId.BIG_BUNKER.defaultCeiling());
        assertEquals(2, StructureId.MILITARY_BASE.defaultCeiling());
        assertEquals(1, StructureId.GAS_STATION.defaultCeiling());
    }

    @Test
    void structureIdsAreUniqueLayoutNames() {
        Set<String> ids = new HashSet<>();
        for (StructureId s : StructureId.values()) {
            assertTrue(ids.add(s.id()), s.id());
            assertTrue(
                s.id()
                    .matches("[a-z_]+"),
                s.id());
        }
        assertEquals(9, ids.size());
    }

    @Test
    void blacklistNamesAreNormalizedAndMatchedIgnoringCase() {
        Set<String> list = WorldgenGate.normalizeNames(new String[] { " snowy ", "", null, "Sandy" });
        assertEquals(new HashSet<>(Arrays.asList("SNOWY", "SANDY")), list);
        assertTrue(WorldgenGate.biomeBlocked(list, Arrays.asList("forest", "Snowy")));
        assertFalse(WorldgenGate.biomeBlocked(list, Arrays.asList("FOREST", "HILLS")));
        assertFalse(WorldgenGate.biomeBlocked(list, Collections.<String>emptyList()));
        assertFalse(WorldgenGate.biomeBlocked(Collections.<String>emptySet(), Arrays.asList("SNOWY")));
        assertTrue(
            WorldgenGate.normalizeNames(null)
                .isEmpty());
    }
}
