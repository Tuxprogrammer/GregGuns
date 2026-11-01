package com.gregguns.compat.carpenters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The primer sets an unknown cached light value once, from the tile's own method, and leaves a known one alone. */
class CarpentersLightPrimerTest {

    /** Stands for Carpenter's TEBase: a private cache, unknown at first, and the protected method that works it out. */
    static class FakeBase {

        private int lightValue = -1;
        int asked;

        protected int getDynamicLightValue() {
            asked++;
            return 7;
        }

        int cached() {
            return lightValue;
        }
    }

    @Test
    void anUnknownValueIsSetOnceFromTheTilesOwnMethod() throws Exception {
        CarpentersLightPrimer p = new CarpentersLightPrimer(FakeBase.class);
        FakeBase te = new FakeBase();
        assertTrue(p.unknown(te));
        assertTrue(p.prime(te));
        assertEquals(7, te.cached());
        assertFalse(p.unknown(te));
        assertFalse(p.prime(te), "a known value is left alone");
        assertEquals(1, te.asked);
        assertFalse(p.unknown("not a tile"));
    }
}
