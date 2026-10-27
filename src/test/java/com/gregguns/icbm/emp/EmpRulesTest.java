package com.gregguns.icbm.emp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

/** The EMP numbers and gates (D16). */
class EmpRulesTest {

    @Test
    void costGrowsContinuouslyWithTheRadius() {
        // D16: 1.6.4 and 1.7 divided the radius as integers, so every radius under the maximum cost the minimum.
        long previous = -1;
        Set<Long> distinct = new HashSet<>();
        for (int r = EmpRules.MIN_RADIUS; r <= 100; r++) {
            long cost = EmpRules.euCost(r);
            assertTrue(cost > previous, "cost must rise at every radius, r=" + r);
            previous = cost;
            distinct.add(cost);
        }
        assertEquals(97, distinct.size());
    }

    @Test
    void costOfAFractionalRadiusFallsBetweenItsNeighbors() {
        assertTrue(EmpRules.euCost(60.5) > EmpRules.euCost(60));
        assertTrue(EmpRules.euCost(60.5) < EmpRules.euCost(61));
    }

    @Test
    void costIsTwentyFiveEuPerSquareBlock() {
        assertEquals(90_000L, EmpRules.euCost(60));
        assertEquals(250_000L, EmpRules.euCost(100));
        assertEquals(0L, EmpRules.euCost(-5));
    }

    @Test
    void doesNotReproduceICBM0511_radiusIsClampedFromBelowToo() {
        assertEquals(EmpRules.MIN_RADIUS, EmpRules.clampRadius(-40, 100));
        assertEquals(EmpRules.MIN_RADIUS, EmpRules.clampRadius(0, 100));
        assertEquals(100, EmpRules.clampRadius(5000, 100));
        assertEquals(60, EmpRules.clampRadius(60, 100));
        assertEquals(EmpRules.MIN_RADIUS, EmpRules.clampRadius(60, 1), "a silly maximum still leaves a legal radius");
    }

    @Test
    void theGateReportsTheFirstFailure() {
        assertEquals(EmpRules.Gate.MODULE_OFF, EmpRules.gate(false, true, 0, 100));
        assertEquals(EmpRules.Gate.COOLING, EmpRules.gate(true, true, 0, 100));
        assertEquals(EmpRules.Gate.NO_POWER, EmpRules.gate(true, false, 99, 100));
        assertEquals(EmpRules.Gate.READY, EmpRules.gate(true, false, 100, 100));
        assertTrue(EmpRules.Gate.READY.isGood());
        assertTrue(!EmpRules.Gate.NO_POWER.isGood());
    }

    @Test
    void anIssueIsAlwaysAddedWhileOneIsLeft() {
        for (int mask = 1; mask < 64; mask++) {
            for (int roll = -7; roll < 20; roll++) {
                int pick = EmpRules.pickIssue(mask, roll);
                assertTrue(pick >= 0 && (mask & (1 << pick)) != 0, "mask " + mask + " roll " + roll);
            }
        }
    }

    @Test
    void nothingToBreakGivesMinusOne() {
        assertEquals(-1, EmpRules.pickIssue(0, 3));
        assertEquals(-1, EmpRules.pickIssue(0b1000000, 3), "bits above the six parts are ignored");
    }

    @Test
    void everyIssueCanBePicked() {
        Set<Integer> seen = new HashSet<>();
        for (int roll = 0; roll < 6; roll++) {
            seen.add(EmpRules.pickIssue(0b111111, roll));
        }
        assertEquals(6, seen.size());
    }

    @Test
    void modesCycleAndParse() {
        assertEquals(EmpMode.MISSILES, EmpMode.ALL.next());
        assertEquals(EmpMode.ALL, EmpMode.MACHINES.next());
        assertEquals(EmpMode.MACHINES, EmpMode.byId(" Machines "));
        assertEquals(null, EmpMode.byId("bogus"));
        assertEquals(null, EmpMode.byId(null));
        assertTrue(EmpMode.ALL.hitsMachines() && EmpMode.ALL.hitsMissiles());
        assertTrue(!EmpMode.MISSILES.hitsMachines() && EmpMode.MISSILES.hitsMissiles());
        assertTrue(EmpMode.MACHINES.hitsMachines() && !EmpMode.MACHINES.hitsMissiles());
    }
}
