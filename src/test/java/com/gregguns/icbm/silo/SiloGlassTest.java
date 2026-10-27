package com.gregguns.icbm.silo;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The silo's accepted-glass list (D115), by registry name, so no Tinkers' or EnderIO class is needed. */
class SiloGlassTest {

    @Test
    void tinkersClearGlassIsAccepted() {
        assertTrue(SiloGlass.accepts("TConstruct:GlassBlock", 0, false));
    }

    @Test
    void enderIoQuiteClearAndEnlightenedClearAreAccepted() {
        assertTrue(SiloGlass.accepts("EnderIO:blockFusedQuartz", 1, false));
        assertTrue(SiloGlass.accepts("EnderIO:blockFusedQuartz", 3, false));
    }

    @Test
    void enderIoQuartzAndDarkGlassAreRefused() {
        for (int meta : new int[] { 0, 2, 4, 5, 6 }) {
            assertFalse(SiloGlass.accepts("EnderIO:blockFusedQuartz", meta, false), "meta " + meta);
        }
    }

    @Test
    void otherGlassIsRefused() {
        assertFalse(SiloGlass.accepts("minecraft:glass", 0, false));
        assertFalse(SiloGlass.accepts("minecraft:stained_glass", 0, false));
        assertFalse(SiloGlass.accepts("bartworks:BoronSilicateGlassBlock", 0, false));
        assertFalse(SiloGlass.accepts("gregtech:gt.blockglass1", 0, false));
        assertFalse(SiloGlass.accepts(null, 0, false));
    }

    @Test
    void withoutEitherModOnlyVanillaGlassStandsIn() {
        assertTrue(SiloGlass.accepts("minecraft:glass", 0, true));
        assertFalse(SiloGlass.accepts("TConstruct:GlassBlock", 0, true));
        assertFalse(SiloGlass.accepts("minecraft:stained_glass", 0, true));
    }
}
