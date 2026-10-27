package com.gregguns.explosion.nuclear;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FalloutYieldTest {

    private static final FalloutYield.Out[][] TABLES = { FalloutYield.RUBBLE, FalloutYield.HOT_RUBBLE,
        FalloutYield.CRUSHED_TRINITITE, FalloutYield.DISSOLVED_GOO, FalloutYield.DISSOLVED_HOT_GOO };

    @Test
    void everyChanceIsAProperShare() {
        for (FalloutYield.Out[] table : TABLES) {
            for (FalloutYield.Out o : table) {
                assertTrue(o.chance() > 0 && o.chance() <= 10000, o.element());
            }
        }
    }

    @Test
    void plainRubbleAndGooReturnNoFissileMaterial() {
        for (FalloutYield.Out[] table : new FalloutYield.Out[][] { FalloutYield.RUBBLE, FalloutYield.DISSOLVED_GOO,
            FalloutYield.DISSOLVED_HOT_GOO }) {
            assertEquals(0.0, FalloutYield.expected(table, "Uranium235"), 0.0);
            assertEquals(0.0, FalloutYield.expected(table, "Plutonium"), 0.0);
        }
    }

    @Test
    void aWholeBlastReturnsAtMostAQuarterOfTheCoreAtTheDefaultCap() {
        double share = FalloutYield.maxFissileShare(com.gregguns.core.config.ExplosionsConfig.falloutMaxBlocks);
        assertTrue(share <= FalloutYield.MAX_RETURN, "share " + share);
        assertTrue(share > 0.0);
    }

    @Test
    void dustSizesAreFractionsOfAFullDust() {
        assertEquals(1.0 / 9.0, FalloutYield.Size.TINY.units(), 1e-12);
        assertEquals(0.25, FalloutYield.Size.SMALL.units(), 1e-12);
    }
}
