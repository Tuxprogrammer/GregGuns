package com.gregguns.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.gregguns.api.Tier;

class AmmoWorksFillTest {

    /** GT's TierEU.RECIPE_*: LV 30 and four times more each tier. */
    private static long eut(Tier tier) {
        return 30L << (2 * (tier.voltageIndex() - 1));
    }

    private static AmmoWorksFill.Recipe rc44() {
        for (AmmoWorksFill.Recipe r : AmmoWorksFill.recipes(AmmoWorksFillTest::eut)) {
            if (r.id.equals("RC-44")) {
                return r;
            }
        }
        throw new AssertionError("RC-44 is not a fill recipe");
    }

    /** A counting store with a cap on how many items it accepts in all, and an optional dishonest take. */
    private static final class Fake implements AmmoWorksFill.Store {

        final Map<String, Long> items = new HashMap<>();
        long capacity = Long.MAX_VALUE;
        boolean stingy;

        Fake with(String item, long n) {
            items.put(item, n);
            return this;
        }

        long total() {
            long t = 0;
            for (long v : items.values()) {
                t += v;
            }
            return t;
        }

        @Override
        public long count(String item) {
            return items.getOrDefault(item, 0L);
        }

        @Override
        public long take(String item, long amount) {
            long give = Math.min(count(item), stingy ? amount - 1 : amount);
            items.merge(item, -give, Long::sum);
            return give;
        }

        @Override
        public boolean fits(String item, long amount) {
            return total() + amount <= capacity;
        }

        @Override
        public void put(String item, long amount) {
            items.merge(item, amount, Long::sum);
        }
    }

    private static AmmoWorksFill.Plan plan(AmmoWorksFill.Recipe r, int parallels, Fake empties, Fake rounds, Fake out) {
        return AmmoWorksFill.plan(r, parallels, Tier.LV.voltageIndex(), 1.0, 1_000_000L, empties, rounds, out);
    }

    @Test
    void theRatiosComeFromTheExistingLoadedMagazineRow() {
        AmmoWorksFill.Recipe r = rc44();
        assertEquals("mag_9mm_pistol", r.loaded);
        assertEquals("mag_9mm_pistol_empty", r.empty);
        assertEquals("ammo_9mm_bullet", r.round);
        assertEquals(4, r.magazinesPerRun);
        assertEquals(12, r.roundsPerRun);
        assertEquals(2 * 20 * 4, r.ticks);
        assertEquals(30, r.eut);
        assertEquals(Tier.LV.voltageIndex(), r.tierIndex);
    }

    @Test
    void everyFillRecipeIsEmptyPlusRoundsPerMagazineIntoLoaded() {
        List<AmmoWorksFill.Recipe> all = AmmoWorksFill.recipes(AmmoWorksFillTest::eut);
        assertTrue(all.size() >= 2, "9mm pistol and SMG at least: " + all.size());
        for (AmmoWorksFill.Recipe r : all) {
            assertTrue(r.magazinesPerRun >= 1 && r.roundsPerRun >= r.magazinesPerRun, r.id);
            assertEquals(0, r.roundsPerRun % r.magazinesPerRun, r.id);
            assertTrue(r.ticks > 0 && r.eut > 0, r.id);
        }
    }

    @Test
    void theParallelCapLimitsTheRuns() {
        AmmoWorksFill.Recipe r = rc44();
        Fake empties = new Fake().with(r.empty, 1000);
        Fake rounds = new Fake().with(r.round, 10000);
        Fake out = new Fake();
        AmmoWorksFill.Plan plan = plan(r, 3, empties, rounds, out);
        assertNotNull(plan);
        assertEquals(3, plan.runs);
        assertEquals(3, AmmoWorksFill.commit(plan, empties, rounds, out));
        assertEquals(1000 - 12, empties.count(r.empty));
        assertEquals(10000 - 36, rounds.count(r.round));
        assertEquals(12, out.count(r.loaded));
    }

    @Test
    void theScarcerInputLimitsTheRuns() {
        AmmoWorksFill.Recipe r = rc44();
        // 25 rounds is two runs of 12; 9 empties is two runs of 4.
        Fake out = new Fake();
        assertEquals(2, plan(r, 48, new Fake().with(r.empty, 1000), new Fake().with(r.round, 25), out).runs);
        assertEquals(2, plan(r, 48, new Fake().with(r.empty, 9), new Fake().with(r.round, 1000), out).runs);
        assertNull(plan(r, 48, new Fake().with(r.empty, 3), new Fake().with(r.round, 1000), out));
        assertNull(plan(r, 48, new Fake().with(r.empty, 1000), new Fake().with(r.round, 11), out));
    }

    @Test
    void aFullOutputStopsTheBenchAndAPartlyFullOneLimitsIt() {
        AmmoWorksFill.Recipe r = rc44();
        Fake empties = new Fake().with(r.empty, 100);
        Fake rounds = new Fake().with(r.round, 1000);
        Fake out = new Fake();
        out.capacity = 0;
        assertNull(plan(r, 48, empties, rounds, out));
        out.capacity = 11; // two runs would make 8, three 12
        AmmoWorksFill.Plan plan = plan(r, 48, empties, rounds, out);
        assertNotNull(plan);
        assertEquals(2, plan.runs);
        assertEquals(100, empties.count(r.empty), "planning takes nothing");
    }

    @Test
    void anOutputThatFilledDuringTheCycleTakesNothing() {
        AmmoWorksFill.Recipe r = rc44();
        Fake empties = new Fake().with(r.empty, 100);
        Fake rounds = new Fake().with(r.round, 1000);
        Fake out = new Fake();
        AmmoWorksFill.Plan plan = plan(r, 5, empties, rounds, out);
        assertNotNull(plan);
        out.capacity = 0;
        assertEquals(0, AmmoWorksFill.commit(plan, empties, rounds, out));
        assertEquals(100, empties.count(r.empty));
        assertEquals(1000, rounds.count(r.round));
        assertEquals(0, out.total());
    }

    @Test
    void inputsThatShrankDuringTheCycleShrinkTheCommit() {
        AmmoWorksFill.Recipe r = rc44();
        Fake empties = new Fake().with(r.empty, 100);
        Fake rounds = new Fake().with(r.round, 1000);
        Fake out = new Fake();
        AmmoWorksFill.Plan plan = plan(r, 10, empties, rounds, out);
        assertEquals(10, plan.runs);
        rounds.items.put(r.round, 30L); // a player took rounds out: two runs left
        assertEquals(2, AmmoWorksFill.commit(plan, empties, rounds, out));
        assertEquals(6, rounds.count(r.round));
        assertEquals(92, empties.count(r.empty));
        assertEquals(8, out.count(r.loaded));
    }

    @Test
    void noItemIsLostOrDuplicated() {
        AmmoWorksFill.Recipe r = rc44();
        Fake empties = new Fake().with(r.empty, 37);
        Fake rounds = new Fake().with(r.round, 500);
        Fake out = new Fake();
        long emptiesBefore = empties.count(r.empty);
        long roundsBefore = rounds.count(r.round);
        int totalRuns = 0;
        for (int i = 0; i < 50; i++) {
            AmmoWorksFill.Plan plan = plan(r, 4, empties, rounds, out);
            if (plan == null) {
                break;
            }
            totalRuns += AmmoWorksFill.commit(plan, empties, rounds, out);
        }
        assertEquals(9, totalRuns, "37 empties is nine runs of four");
        assertEquals(emptiesBefore - out.count(r.loaded), empties.count(r.empty), "every loaded mag used one empty");
        assertEquals(roundsBefore - 12L * totalRuns, rounds.count(r.round));
        assertEquals(4L * totalRuns, out.count(r.loaded));
    }

    @Test
    void aStoreThatGivesLessThanItCountedIsPutBack() {
        AmmoWorksFill.Recipe r = rc44();
        Fake empties = new Fake().with(r.empty, 100);
        Fake rounds = new Fake().with(r.round, 1000);
        Fake out = new Fake();
        AmmoWorksFill.Plan plan = plan(r, 2, empties, rounds, out);
        rounds.stingy = true;
        assertEquals(0, AmmoWorksFill.commit(plan, empties, rounds, out));
        assertEquals(100, empties.count(r.empty));
        assertEquals(1000, rounds.count(r.round));
        assertEquals(0, out.total());
    }

    @Test
    void thePowerSupplyCapsTheRuns() {
        AmmoWorksFill.Recipe r = rc44();
        Fake empties = new Fake().with(r.empty, 1000);
        Fake rounds = new Fake().with(r.round, 10000);
        // 30 EU/t per LV run at LV: 100 EU/t carries three runs, 29 carries none.
        assertEquals(3, AmmoWorksFill.plan(r, 48, 1, 1.0, 100, empties, rounds, new Fake()).runs);
        assertNull(AmmoWorksFill.plan(r, 48, 1, 1.0, 29, empties, rounds, new Fake()));
    }

    @Test
    void overclockingHalvesTheTimeAndQuadruplesThePowerPerTier() {
        AmmoWorksFill.Recipe r = rc44();
        Fake empties = new Fake().with(r.empty, 1000);
        Fake rounds = new Fake().with(r.round, 10000);
        AmmoWorksFill.Plan lv = AmmoWorksFill.plan(r, 2, 1, 1.0, 1_000_000L, empties, rounds, new Fake());
        AmmoWorksFill.Plan hv = AmmoWorksFill.plan(r, 2, 3, 1.0, 1_000_000L, empties, rounds, new Fake());
        assertEquals(0, lv.overclocks);
        assertEquals(160, lv.ticks);
        assertEquals(60, lv.eutPerTick);
        assertEquals(2, hv.overclocks);
        assertEquals(40, hv.ticks);
        assertEquals(60 * 16, hv.eutPerTick);
    }

    @Test
    void theBeltShortensTheCycle() {
        AmmoWorksFill.Recipe r = rc44();
        Fake empties = new Fake().with(r.empty, 1000);
        Fake rounds = new Fake().with(r.round, 10000);
        double belt = AmmoWorksShape.speedMultiplier(2);
        AmmoWorksFill.Plan plan = AmmoWorksFill.plan(r, 1, 1, belt, 1_000_000L, empties, rounds, new Fake());
        assertEquals((int) Math.ceil(160 * belt), plan.ticks);
    }

    @Test
    void overclocksNeverShrinkACycleUnderOneTick() {
        assertEquals(0, AmmoWorksFill.overclocks(1, 1, 160));
        assertEquals(3, AmmoWorksFill.overclocks(9, 1, 8));
        assertEquals(1, AmmoWorksFill.cycleTicks(0.2, 3));
        assertEquals(20, AmmoWorksFill.cycleTicks(160, 3));
    }
}
