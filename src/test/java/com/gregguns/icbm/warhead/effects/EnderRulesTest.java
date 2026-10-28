package com.gregguns.icbm.warhead.effects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EnderRulesTest {

    @Test
    void theIcbmPullCurveIsKeptAtRadiusTwenty() {
        // ICBM: motion -= (sign * 20 - delta) * |delta| * 0.0006, peak 0.06 at ten blocks.
        assertEquals(-0.06, EnderRules.pull(10.0, 20.0), 1e-9);
        assertEquals(0.06, EnderRules.pull(-10.0, 20.0), 1e-9);
    }

    @Test
    void thePullIsZeroAtTheCenterAndTheRadiusAndAlwaysInward() {
        assertEquals(0.0, EnderRules.pull(0.0, 24.0), 1e-12);
        assertEquals(0.0, EnderRules.pull(24.0, 24.0), 1e-12);
        assertEquals(0.0, EnderRules.pull(-30.0, 24.0), 1e-12);
        for (double d = 0.5; d < 24.0; d += 0.5) {
            assertTrue(EnderRules.pull(d, 24.0) < 0.0, "toward the center from +" + d);
            assertTrue(EnderRules.pull(-d, 24.0) > 0.0, "toward the center from -" + d);
        }
    }

    @Test
    void thePeakDoesNotGrowWithTheRadius() {
        assertEquals(Math.abs(EnderRules.pull(10.0, 20.0)), Math.abs(EnderRules.pull(15.0, 30.0)), 1e-9);
    }

    @Test
    void theTargetOffsetStaysInItsBand() {
        for (double u = 0.0; u < 1.0; u += 0.001) {
            int off = EnderRules.offset(u);
            assertTrue(
                Math.abs(off) >= EnderRules.TARGET_MIN && Math.abs(off) <= EnderRules.TARGET_SPREAD,
                "offset " + off);
        }
        assertTrue(EnderRules.offset(0.1) < 0 && EnderRules.offset(0.9) > 0);
        // Out-of-range input is clamped, never thrown on.
        assertTrue(Math.abs(EnderRules.offset(5.0)) <= EnderRules.TARGET_SPREAD);
        assertTrue(Math.abs(EnderRules.offset(-5.0)) <= EnderRules.TARGET_SPREAD);
    }

    @Test
    void teleportRangeIsASphere() {
        assertTrue(EnderRules.inTeleportRange(2.0, 2.0, 2.0));
        assertFalse(EnderRules.inTeleportRange(3.0, 3.0, 0.0));
        assertTrue(EnderRules.inTeleportRange(0.0, 0.0, 4.0));
    }

    @Test
    void theNetherScanStartsBelowTheBedrockRoof() {
        // The Nether's height value is its roof (128): a scan from there lands on the bedrock ceiling.
        assertEquals(EnderRules.ROOFED_SCAN_TOP, EnderRules.scanTop(true, 128));
        assertEquals(64, EnderRules.scanTop(true, 64));
        assertEquals(250, EnderRules.scanTop(false, 255));
        assertEquals(128, EnderRules.scanTop(false, 128));
    }
}
