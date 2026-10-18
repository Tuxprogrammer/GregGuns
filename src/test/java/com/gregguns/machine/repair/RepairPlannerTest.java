package com.gregguns.machine.repair;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.gregguns.api.Tier;
import com.gregguns.armor.logic.RepairSpec;
import com.gregguns.machine.repair.RepairPlanner.Candidate;
import com.gregguns.machine.repair.RepairPlanner.Plan;
import com.gregguns.machine.repair.RepairPlanner.Status;

class RepairPlannerTest {

    private static final RepairSpec STEEL_VEST = new RepairSpec("plate", "Steel", "part_heavy_cloth", 0.5, 4);
    private static final RepairSpec CLOTH_ONLY = new RepairSpec(null, null, "part_heavy_cloth", 0.0, 4);
    private static final String STEEL = "plate:Steel";
    private static final String CLOTH = "item:part_heavy_cloth";

    private static Map<String, Integer> pool(int steel, int cloth) {
        Map<String, Integer> pool = new HashMap<>();
        pool.put(STEEL, steel);
        pool.put(CLOTH, cloth);
        return pool;
    }

    private static Candidate vest(int damage) {
        return new Candidate(damage, 826, Tier.LV, STEEL_VEST);
    }

    @Test
    void aFullyWornPieceCostsTheWholeListAndRepairs() {
        Plan plan = RepairPlanner.plan(Collections.singletonList(vest(825)), pool(2, 2), Tier.LV, 1);
        assertEquals(Collections.singletonList(Status.REPAIRED), plan.statuses());
        assertEquals(
            2,
            (int) plan.consumed()
                .get(STEEL));
        assertEquals(
            2,
            (int) plan.consumed()
                .get(CLOTH));
        assertEquals(1.0, plan.maxFraction(), 1e-9);
    }

    @Test
    void repairIsAllOrNothing() {
        // GGS-09-22: one cloth short means nothing at all is taken.
        Plan plan = RepairPlanner.plan(Collections.singletonList(vest(825)), pool(2, 1), Tier.LV, 1);
        assertEquals(Collections.singletonList(Status.MISSING_MATERIALS), plan.statuses());
        assertTrue(
            plan.consumed()
                .isEmpty());
        assertEquals(0, plan.repaired());
    }

    @Test
    void anItemAboveTheMachineTierIsSkipped() {
        // GGS-08-72 and GGS-08-50.
        Candidate mv = new Candidate(100, 826, Tier.MV, STEEL_VEST);
        Plan plan = RepairPlanner.plan(Collections.singletonList(mv), pool(9, 9), Tier.LV, 1);
        assertEquals(Collections.singletonList(Status.TIER_TOO_LOW), plan.statuses());
        assertTrue(
            plan.consumed()
                .isEmpty());
    }

    @Test
    void anUndamagedOrUnrepairableItemIsLeftAlone() {
        Candidate fresh = vest(0);
        Candidate noSpec = new Candidate(50, 826, Tier.LV, null);
        Plan plan = RepairPlanner.plan(Arrays.asList(fresh, noSpec), pool(9, 9), Tier.LV, 4);
        assertEquals(Arrays.asList(Status.UNDAMAGED, Status.NOT_REPAIRABLE), plan.statuses());
    }

    @Test
    void aBatchStopsAtTheBayCount() {
        Plan plan = RepairPlanner.plan(Arrays.asList(vest(825), vest(825), vest(825)), pool(20, 20), Tier.HV, 2);
        assertEquals(Arrays.asList(Status.REPAIRED, Status.REPAIRED, Status.NO_CAPACITY), plan.statuses());
        assertEquals(
            4,
            (int) plan.consumed()
                .get(STEEL));
        assertEquals(2, plan.repaired());
    }

    @Test
    void anItemThatLacksMaterialsDoesNotStopTheOthers() {
        // GGS-08-70: the pool covers the first piece only; the second is skipped, the third is cloth-only and fits.
        Candidate cloth = new Candidate(825, 826, Tier.STEAM, CLOTH_ONLY);
        Plan plan = RepairPlanner.plan(Arrays.asList(vest(825), vest(825), cloth), pool(2, 6), Tier.LV, 4);
        assertEquals(Arrays.asList(Status.REPAIRED, Status.MISSING_MATERIALS, Status.REPAIRED), plan.statuses());
        assertEquals(
            6,
            (int) plan.consumed()
                .get(CLOTH));
    }

    @Test
    void theJobLastsAsLongAsTheMostDamagedItem() {
        Plan plan = RepairPlanner.plan(Arrays.asList(vest(100), vest(500)), pool(20, 20), Tier.LV, 2);
        assertEquals(500.0 / 825.0, plan.maxFraction(), 1e-9);
    }

    @Test
    void thePlanDoesNotChangeThePool() {
        Map<String, Integer> pool = pool(2, 2);
        RepairPlanner.plan(Collections.singletonList(vest(825)), pool, Tier.LV, 1);
        assertEquals(2, (int) pool.get(STEEL));
        assertEquals(2, (int) pool.get(CLOTH));
    }

    @Test
    void materialKeysNameThePrefixAndTheItem() {
        assertEquals("plate:Steel", RepairPlanner.metalKey(STEEL_VEST));
        assertEquals("item:part_heavy_cloth", RepairPlanner.clothKey(STEEL_VEST));
    }
}
