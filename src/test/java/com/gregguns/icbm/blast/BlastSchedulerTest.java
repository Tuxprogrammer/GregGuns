package com.gregguns.icbm.blast;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.gregguns.icbm.explosive.TickBudget;

/**
 * The shared tick budget and the round-robin scheduler. Names ICBM-02-06 (one budget per tick, not per phase) and
 * ICBM-02-90 (a spent budget stops the loop).
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/blast-engine.md (tick budget)
class BlastSchedulerTest {

    /** A task that needs {@code left} slices, each costing one fake time unit. */
    private static final class Work implements BlastTask {

        final long[] clock;
        int left;
        int done;
        boolean waiting;

        Work(long[] clock, int left) {
            this.clock = clock;
            this.left = left;
        }

        @Override
        public boolean wantsWork() {
            return !waiting && left > 0;
        }

        @Override
        public boolean step() {
            clock[0] += 10;
            left--;
            done++;
            return left == 0;
        }
    }

    private static TickBudget budget(long[] clock, long limit) {
        TickBudget b = new TickBudget(() -> clock[0]);
        b.open(limit);
        return b;
    }

    @Test
    void icbm0290TheBudgetIsSpentNotExceededByOne() {
        long[] clock = { 0 };
        TickBudget b = budget(clock, 100);
        Work w = new Work(clock, 1000);
        List<Work> tasks = new ArrayList<>(java.util.Arrays.asList(w));
        new BlastScheduler().runTick(tasks, b);
        assertEquals(10, w.done, "10 slices of 10 units fill a budget of 100; the 11th does not run");
    }

    @Test
    void severalBlastsShareOneBudgetFairly() {
        long[] clock = { 0 };
        TickBudget b = budget(clock, 300);
        Work a = new Work(clock, 1000);
        Work c = new Work(clock, 1000);
        Work d = new Work(clock, 1000);
        List<Work> tasks = new ArrayList<>(java.util.Arrays.asList(a, c, d));
        int slices = new BlastScheduler().runTick(tasks, b);
        assertEquals(30, slices, "the whole budget is used");
        assertEquals(10, a.done);
        assertEquals(10, c.done);
        assertEquals(10, d.done);
    }

    @Test
    void icbm0206AnExhaustedBudgetStillMakesProgressOnOneTaskAndRotates() {
        long[] clock = { 0 };
        Work a = new Work(clock, 100);
        Work c = new Work(clock, 100);
        List<Work> tasks = new ArrayList<>(java.util.Arrays.asList(a, c));
        BlastScheduler scheduler = new BlastScheduler();
        for (int tick = 0; tick < 4; tick++) {
            TickBudget b = budget(clock, 1); // spent after the first slice
            assertEquals(1, scheduler.runTick(tasks, b));
        }
        assertEquals(2, a.done);
        assertEquals(2, c.done, "the starting task rotates, so neither starves");
    }

    @Test
    void waitingTasksAreSkippedAndFinishedOnesRemoved() {
        long[] clock = { 0 };
        Work waiting = new Work(clock, 5);
        waiting.waiting = true;
        Work quick = new Work(clock, 2);
        List<Work> tasks = new ArrayList<>(java.util.Arrays.asList(waiting, quick));
        new BlastScheduler().runTick(tasks, budget(clock, 1000));
        assertEquals(0, waiting.done);
        assertEquals(2, quick.done);
        assertEquals(1, tasks.size());
        assertTrue(tasks.contains(waiting));
    }

    @Test
    void budgetCountsOneNumberPerTickNotPerPhase() {
        long[] clock = { 0 };
        TickBudget b = new TickBudget(() -> clock[0]);
        b.open(50_000_000L);
        assertEquals(50_000_000L, b.limitNanos());
        clock[0] = 50_000_000L;
        assertTrue(b.exhausted(), "reaching the limit counts as spent");
        assertEquals(0L, b.remainingNanos());
        b.open(50_000_000L);
        assertTrue(!b.exhausted(), "open() starts a fresh tick");
    }
}
