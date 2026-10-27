package com.gregguns.explosion.nuclear;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FalloutGooMathTest {

    @Test
    void gooIsRareAndFallsToZeroAtTheInnerRim() {
        assertEquals(FalloutMath.GOO_PEAK, FalloutMath.gooChance(0.0, 30.0), 1e-12);
        assertEquals(0.0, FalloutMath.gooChance(30.0, 30.0), 0.0);
        assertEquals(0.0, FalloutMath.gooChance(45.0, 30.0), 0.0);
        assertEquals(0.0, FalloutMath.gooChance(5.0, 0.0), 0.0);
        assertTrue(FalloutMath.gooChance(10.0, 30.0) > FalloutMath.gooChance(20.0, 30.0));
        assertTrue(FalloutMath.GOO_PEAK <= 0.05, "about one percent on average");
    }
}
