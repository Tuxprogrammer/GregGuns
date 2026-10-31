package com.gregguns.icbm.complex.gen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

/** D308: the metal prop ladder never passes the ceiling, and the low metals are the common ones. */
class MetalPropsTest {

    @Test
    void noCeilingEverYieldsARungAboveIt() {
        for (int ceiling = 4; ceiling <= 12; ceiling++) {
            for (long roll = 0; roll < 5000; roll++) {
                MetalProps.Prop p = MetalProps.pick(ceiling, roll * 0x9E3779B97F4A7C15L);
                assertTrue(p.tier <= ceiling, "ceiling " + ceiling + " gave " + p);
                assertEquals(MetalProps.FALLBACK, p.fallback());
            }
        }
    }

    @Test
    void theOverworldRunsIronToTitaniumAndTheCeilingsMetalIsAFind() {
        Map<Integer, Integer> seen = new HashMap<Integer, Integer>();
        for (long roll = 0; roll < 20000; roll++) {
            MetalProps.Prop p = MetalProps.pick(4, roll * 0x9E3779B97F4A7C15L + 7);
            seen.merge(p.tier, 1, Integer::sum);
        }
        assertEquals(4, seen.size(), "LV, MV, HV and EV all appear: " + seen);
        assertTrue(seen.get(1) > 3 * seen.get(4), "LV is far more common than EV: " + seen);
        assertTrue(seen.get(4) > 0);
    }

    @Test
    void weightsFollowTheLadder() {
        assertEquals(4, MetalProps.weight(1, 9));
        assertEquals(4, MetalProps.weight(2, 9));
        assertEquals(2, MetalProps.weight(5, 9));
        assertEquals(1, MetalProps.weight(8, 9));
        assertEquals(1, MetalProps.weight(9, 9));
        assertEquals(0, MetalProps.weight(10, 9));
        for (int t = 1; t <= 12; t++) assertFalse(
            MetalProps.rung(t)
                .isEmpty(),
            "rung " + t);
    }
}
