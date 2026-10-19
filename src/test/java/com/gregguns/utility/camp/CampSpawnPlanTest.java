package com.gregguns.utility.camp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class CampSpawnPlanTest {

    @Test
    void capFormula() {
        assertEquals(24, CampSpawnPlan.defaultCap(32, 32, 0.75F));
        assertEquals(15, CampSpawnPlan.defaultCap(20, 21, 0.75F));
        assertEquals(0, CampSpawnPlan.defaultCap(0, 0, 0.75F));
        assertEquals(0, CampSpawnPlan.defaultCap(-10, -10, 0.75F));
    }

    @Test
    void spawnsEveryIntervalBelowTheCap() {
        CampSpawnPlan plan = new CampSpawnPlan(200);
        AtomicInteger counts = new AtomicInteger();
        int first = -1;
        int second = -1;
        for (int t = 1; t <= 450; t++) {
            boolean spawn = plan.tick(true, 5, 2, 1, () -> {
                counts.incrementAndGet();
                return 0;
            });
            if (spawn) {
                if (first < 0) {
                    first = t;
                } else if (second < 0) {
                    second = t;
                }
            }
        }
        assertEquals(200, first);
        assertEquals(400, second);
        // One count on the first tick, then every 60 ticks.
        assertEquals(1 + 450 / 60, counts.get());
    }

    @Test
    void atTheCapNothingSpawnsAndTheTimerResets() {
        CampSpawnPlan plan = new CampSpawnPlan(200);
        for (int t = 1; t <= 600; t++) {
            assertFalse(plan.tick(true, 3, 1, 1, () -> 3));
        }
        assertFalse(plan.enabled());
        // Space opens up: the next count enables it and most of an interval has to pass again.
        int spawnedAt = -1;
        for (int t = 1; t <= 400 && spawnedAt < 0; t++) {
            if (plan.tick(true, 3, 1, 1, () -> 0)) {
                spawnedAt = t;
            }
        }
        assertTrue(spawnedAt > 140, "needs most of an interval after the reset, got " + spawnedAt);
    }

    @Test
    void emptyListsNeverSpawn() {
        CampSpawnPlan plan = new CampSpawnPlan(10);
        for (int t = 0; t < 500; t++) {
            assertFalse(plan.tick(true, 5, 0, 1, () -> 0));
        }
        CampSpawnPlan noNames = new CampSpawnPlan(10);
        for (int t = 0; t < 500; t++) {
            assertFalse(noNames.tick(true, 5, 1, 0, () -> 0));
        }
    }

    /** MUTL-39: an uninitialized flag neither counts nor spawns nor throws. */
    @Test
    void doesNotReproduceMUTL39_uninitializedFlagIsSafe() {
        CampSpawnPlan plan = new CampSpawnPlan(10);
        for (int t = 0; t < 500; t++) {
            assertFalse(plan.tick(false, 5, 1, 1, () -> { throw new AssertionError("must not count"); }));
        }
    }
}
