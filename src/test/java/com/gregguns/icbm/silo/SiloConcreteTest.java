package com.gregguns.icbm.silo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The silo's accepted concrete metadata (D116). */
class SiloConcreteTest {

    @Test
    void everyFinishedVariantOfBothColorsIsAccepted() {
        // 0 dark stone, 3-5 dark bricks, 6 chiseled, 7 smooth; and the same +8 for light.
        int[] finished = { 0, 3, 4, 5, 6, 7 };
        for (int meta : finished) {
            assertTrue(SiloConcrete.acceptsMeta(meta), "dark " + meta);
            assertTrue(SiloConcrete.acceptsMeta(meta + 8), "light " + (meta + 8));
        }
    }

    @Test
    void cobblestoneFormsAreRefused() {
        for (int meta : new int[] { 1, 2, 9, 10 }) {
            assertFalse(SiloConcrete.acceptsMeta(meta), "meta " + meta);
        }
    }

    @Test
    void outOfRangeMetaIsRefused() {
        assertFalse(SiloConcrete.acceptsMeta(-1));
        assertFalse(SiloConcrete.acceptsMeta(16));
    }

    @Test
    void theHologramBlockIsAccepted() {
        assertTrue(SiloConcrete.acceptsMeta(SiloConcrete.LIGHT));
        assertEquals(8, SiloConcrete.LIGHT);
    }
}
