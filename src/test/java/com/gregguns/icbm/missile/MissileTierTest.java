package com.gregguns.icbm.missile;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.gregguns.icbm.explosive.ExplosiveTier;

class MissileTierTest {

    @Test
    void theFuelAmountIsFixedPerTier() {
        assertEquals(2000, MissileTier.T1.baseFuelMb());
        assertEquals(4000, MissileTier.T2.baseFuelMb());
    }

    @Test
    void fuelListsFollowD19() {
        // HV: RP-1 or Cetane-Boosted Diesel or better; EV-IV: Dense Hydrazine or better. "Better" includes the rest.
        assertArrayEquals(
            new String[] { "nitrofuel", "rocketfuelmixb", "rocketfuelmixd", "rocketfuelmixc", "rocketfuelmixa" },
            MissileTier.T1.fuels());
        assertArrayEquals(
            new String[] { "rocketfuelmixd", "rocketfuelmixc", "rocketfuelmixa" },
            MissileTier.T2.fuels());
    }

    @Test
    void theFuelArrayIsACopy() {
        String[] a = MissileTier.T2.fuels();
        a[0] = "water";
        assertEquals("rocketfuelmixd", MissileTier.T2.fuels()[0]);
    }

    @Test
    void aBodyCarriesItsOwnWarheadTierAndLighter() {
        assertTrue(MissileTier.T1.carries(ExplosiveTier.T1));
        assertFalse(MissileTier.T1.carries(ExplosiveTier.T2));
        assertTrue(MissileTier.T2.carries(ExplosiveTier.T1));
        assertTrue(MissileTier.T2.carries(ExplosiveTier.T2));
        assertFalse(MissileTier.T2.carries(ExplosiveTier.T3));
    }

    @Test
    void theBodiesAreFullHeightAndTheHealthIsFixedPerTier() {
        assertEquals(3, MissileTier.T1.heightBlocks());
        assertEquals(4, MissileTier.T2.heightBlocks());
        // ICBM-03-01: the baseline gave every tier the tier-1 health.
        assertTrue(MissileTier.T2.health() > MissileTier.T1.health());
    }

    @Test
    void rangeBandsAndSpeedsRiseWithTheTier() {
        assertTrue(MissileTier.T2.baseMaxRange() > MissileTier.T1.baseMaxRange());
        assertTrue(MissileTier.T2.speed() > MissileTier.T1.speed());
        assertTrue(MissileTier.T1.minRange() > 0);
    }

    @Test
    void lookupByNumber() {
        assertEquals(MissileTier.T2, MissileTier.byNumber(2));
        assertNull(MissileTier.byNumber(3));
        assertEquals("missile_body_t1", MissileTier.T1.blockName());
    }
}
