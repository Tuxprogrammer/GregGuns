package com.gregguns.icbm.complex.gen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.gregguns.core.config.IcbmConfig;
import com.gregguns.icbm.complex.PlanetTable;
import com.gregguns.icbm.worldgen.SiloCeiling;

/**
 * The D38 table keyed by what {@link DimensionKey} returns: a Galacticraft body's own unlocalized name, not the
 * localized provider name ("Io", "Ganymede", "Barnarda C"), which is why eight planets once never got a complex.
 */
class PlanetNamesTest {

    /** Every dimension of the table with its rocket tier, as GalacticGreg names them (its DimNames). */
    private static final String[][] BODIES = { { "Overworld", "0" }, { "moon", "1" }, { "mars", "2" },
        { "phobos", "2" }, { "deimos", "2" }, { "ceres", "3" }, { "callisto", "3" }, { "europa", "3" },
        { "ganymed", "3" }, { "iojupiter", "4" }, { "mercury", "4" }, { "venus", "4" }, { "enceladus", "5" },
        { "titan", "5" }, { "miranda", "5" }, { "oberon", "5" }, { "proteus", "6" }, { "triton", "6" },
        { "pluto", "7" }, { "haumea", "7" }, { "makemake", "7" }, { "barnarda2", "8" }, { "barnarda4", "8" },
        { "barnarda5", "8" }, { "centauribb", "8" }, { "tcetie", "8" }, { "vega1", "8" }, { "anubis", "9" },
        { "horus", "9" }, { "maahes", "9" }, { "neper", "9" }, { "seth", "9" }, { "ross128b", "-1" },
        { "ross128ba", "-1" } };

    @Test
    void allThirtyFourNamesAreInTheTableWithTheirTier() {
        assertEquals(34, BODIES.length);
        for (String[] b : BODIES) {
            Integer tier = PlanetTable.tierOf(b[0]);
            assertEquals(Integer.valueOf(b[1]), tier, b[0]);
            int ceiling = ComplexPlacer.ceilingFor(b[0], 12345L, SiloCeiling.DEFAULT_ROSS_WEIGHTS);
            assertTrue(ceiling >= SiloCeiling.EV && ceiling <= SiloCeiling.UMV, b[0] + " " + ceiling);
            if (!b[1].equals("-1")) {
                assertEquals(SiloCeiling.EV + Math.max(0, Integer.parseInt(b[1]) - 1), ceiling, b[0]);
            }
        }
        assertEquals(
            34,
            PlanetTable.names()
                .size());
    }

    @Test
    void theDefaultConfigListIsExactlyTheTable() {
        Set<String> cfg = new HashSet<>();
        for (String n : IcbmConfig.siloComplexDimensions) cfg.add(n.toLowerCase(java.util.Locale.ROOT));
        Set<String> table = new HashSet<>();
        for (String[] b : BODIES) table.add(b[0].toLowerCase(java.util.Locale.ROOT));
        assertEquals(table, cfg);
    }

    @Test
    void localizedNamesAreNotKeys() {
        // What WorldProviderSpace.getDimensionName() answers in English for the planets that failed.
        for (String localized : Arrays
            .asList("Io", "Ganymede", "Barnarda C", "Barnarda E", "Barnarda F", "α Centauri Bb", "Vega B", "Mars")) {
            if (localized.equals("Mars")) continue; // matches "mars" by case, the one that always worked
            assertNull(PlanetTable.tierOf(localized), localized);
        }
    }

    @Test
    void ross128RollsFromItsOwnNineWeightsAndReachesUmv() {
        assertEquals(SiloCeiling.STEPS, IcbmConfig.siloComplexRoss128Weights.length);
        Set<Integer> seen = new HashSet<>();
        for (long s = 0; s < 40000; s++) {
            seen.add(
                ComplexPlacer
                    .ceilingFor("ross128b", s * 0x9E3779B97F4A7C15L + 7, IcbmConfig.siloComplexRoss128Weights));
        }
        assertTrue(seen.contains(SiloCeiling.UMV), "UMV rolls: " + seen);
        assertTrue(seen.contains(SiloCeiling.EV));
        assertEquals(SiloCeiling.STEPS, seen.size());
    }

    @Test
    void aDimensionTheTableLacksGetsTheLowestCeiling() {
        assertEquals(SiloCeiling.EV, ComplexPlacer.ceilingFor("someaddedworld", 1L, SiloCeiling.DEFAULT_ROSS_WEIGHTS));
    }
}
