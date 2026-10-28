package com.gregguns.icbm.complex.gen;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.gregguns.core.config.IcbmConfig;
import com.gregguns.icbm.complex.PlanetTable;

/** The dimension list gates complexes, and a listed dimension outside the planet table gets none (D249). */
class ConfigSettingsTest {

    @Test
    void aListedPlanetOfTheTableGenerates() {
        String[] list = { "Overworld", "moon", "Ross128b" };
        assertEquals(ConfigSettings.Gate.ENABLED, ConfigSettings.gate(list, "Overworld"));
        assertEquals(ConfigSettings.Gate.ENABLED, ConfigSettings.gate(list, "Moon"), "names compare ignoring case");
        assertEquals(ConfigSettings.Gate.ENABLED, ConfigSettings.gate(list, "ross128b"));
        assertEquals(ConfigSettings.Gate.NOT_LISTED, ConfigSettings.gate(list, "mars"));
    }

    /** T Ceti E left the table (D239); a config written before that still lists it, and it must not generate. */
    @Test
    void aListedDimensionOutsideTheTableIsSkipped() {
        assertNull(PlanetTable.tierOf("tcetie"));
        String[] old = { "Overworld", "tcetie", "someModsWorld" };
        assertEquals(ConfigSettings.Gate.NOT_IN_TABLE, ConfigSettings.gate(old, "tcetie"));
        assertEquals(ConfigSettings.Gate.NOT_IN_TABLE, ConfigSettings.gate(old, "someModsWorld"));
    }

    @Test
    void everyDefaultDimensionIsInTheTable() {
        for (String n : IcbmConfig.siloComplexDimensions) {
            assertEquals(ConfigSettings.Gate.ENABLED, ConfigSettings.gate(IcbmConfig.siloComplexDimensions, n), n);
        }
    }

    @Test
    void theSettingsSkipATableMissAndKeepSkippingIt() {
        String[] dims = IcbmConfig.siloComplexDimensions;
        boolean on = IcbmConfig.siloComplexEnabled;
        try {
            IcbmConfig.siloComplexEnabled = true;
            IcbmConfig.siloComplexDimensions = new String[] { "Overworld", "tcetie" };
            ConfigSettings s = new ConfigSettings();
            assertTrue(s.dimensionEnabled("Overworld"));
            assertFalse(s.dimensionEnabled("tcetie"));
            assertFalse(s.dimensionEnabled("tcetie"), "the second call (logged once) is still a skip");
        } finally {
            IcbmConfig.siloComplexDimensions = dims;
            IcbmConfig.siloComplexEnabled = on;
        }
    }
}
