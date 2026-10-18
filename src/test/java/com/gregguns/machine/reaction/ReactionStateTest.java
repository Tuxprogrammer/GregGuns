package com.gregguns.machine.reaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

class ReactionStateTest {

    private static ReactionState started(int preferred, int margin, int required, int reactionTicks) {
        ReactionState state = new ReactionState();
        state.start(preferred, margin, required, reactionTicks * ReactionState.REACTION_TICK, ReactionRisk.BREAK_ITEM);
        return state;
    }

    /** Runs the whole duration with the setting always on the current preferred intensity. */
    private static ReactionState.Outcome runTuned(ReactionState state, Random random, int maxTicks) {
        ReactionState.Outcome outcome = ReactionState.Outcome.RUNNING;
        for (int i = 0; i < maxTicks && outcome == ReactionState.Outcome.RUNNING; i++) {
            outcome = state.tick(state.preferredIntensity(), random);
        }
        return outcome;
    }

    @Test
    void marginZeroNeverDrifts() {
        // GGS-08-20
        ReactionState state = started(5, 0, 100, 50);
        Random random = new Random(1);
        for (int i = 0; i < 50 * ReactionState.REACTION_TICK - 1; i++) {
            state.tick(5, random);
            assertEquals(5, state.preferredIntensity());
        }
    }

    @Test
    void driftStaysInsideThePreferredPlusOrMinusMargin() {
        ReactionState state = started(5, 2, 1000, 200);
        Random random = new Random(42);
        boolean movedUp = false;
        boolean movedDown = false;
        for (int i = 0; i < 200 * ReactionState.REACTION_TICK - 1; i++) {
            state.tick(0, random);
            int preferred = state.preferredIntensity();
            assertTrue(preferred >= 3 && preferred <= 7, "preferred " + preferred);
            movedUp |= preferred > 5;
            movedDown |= preferred < 5;
        }
        assertTrue(movedUp && movedDown, "a long run should visit both sides");
    }

    @Test
    void anNTickRecipeGetsNEvaluationsIncludingTheLast() {
        // Fixes MPRD-18: Techguns excluded the final reaction tick.
        ReactionState state = started(3, 0, 10, 10);
        Random random = new Random(7);
        ReactionState.Outcome outcome = runTuned(state, random, 10 * ReactionState.REACTION_TICK);
        assertEquals(10, state.completion());
        assertEquals(ReactionState.Outcome.SUCCESS, outcome);
        assertEquals(10 * ReactionState.REACTION_TICK, state.elapsed());
    }

    @Test
    void earlyFinishAsSoonAsCompletionReachesTheRequirement() {
        ReactionState state = started(3, 0, 5, 10);
        ReactionState.Outcome outcome = runTuned(state, new Random(3), 10 * ReactionState.REACTION_TICK);
        assertEquals(ReactionState.Outcome.SUCCESS, outcome);
        assertEquals(5 * ReactionState.REACTION_TICK, state.elapsed());
        assertFalse(state.isActive());
    }

    @Test
    void wrongIntensityFailsAtTheEndOfTheDuration() {
        ReactionState state = started(3, 0, 5, 10);
        Random random = new Random(3);
        ReactionState.Outcome outcome = ReactionState.Outcome.RUNNING;
        int ticks = 0;
        while (outcome == ReactionState.Outcome.RUNNING) {
            outcome = state.tick(9, random);
            ticks++;
        }
        assertEquals(ReactionState.Outcome.FAILURE, outcome);
        assertEquals(10 * ReactionState.REACTION_TICK, ticks);
        assertEquals(0, state.completion());
        assertEquals(ReactionRisk.BREAK_ITEM, state.risk());
    }

    @Test
    void theFinalReactionTickCanStillRescueTheRun() {
        // Required 1 of 1: tuned only on the very last tick succeeds, not fails.
        ReactionState state = started(4, 0, 1, 1);
        Random random = new Random(0);
        for (int i = 0; i < ReactionState.REACTION_TICK - 1; i++) {
            assertEquals(ReactionState.Outcome.RUNNING, state.tick(0, random));
        }
        assertEquals(ReactionState.Outcome.SUCCESS, state.tick(4, random));
    }

    @Test
    void seededRunsAreReproducible() {
        ReactionState a = started(5, 2, 1000, 100);
        ReactionState b = started(5, 2, 1000, 100);
        Random ra = new Random(99);
        Random rb = new Random(99);
        for (int i = 0; i < 3000; i++) {
            a.tick(1, ra);
            b.tick(1, rb);
        }
        assertEquals(a.preferredIntensity(), b.preferredIntensity());
    }

    @Test
    void stateSurvivesAnArrayRoundTrip() {
        // GGS-08-22
        ReactionState state = started(8, 1, 7, 10);
        Random random = new Random(5);
        for (int i = 0; i < 200; i++) {
            state.tick(state.preferredIntensity(), random);
        }
        ReactionState copy = new ReactionState();
        copy.fromArray(state.toArray());
        assertTrue(copy.isActive());
        assertEquals(state.preferredIntensity(), copy.preferredIntensity());
        assertEquals(state.completion(), copy.completion());
        assertEquals(state.required(), copy.required());
        assertEquals(state.elapsed(), copy.elapsed());
        assertEquals(state.risk(), copy.risk());
        // Both finish identically from here.
        Random r1 = new Random(11);
        Random r2 = new Random(11);
        assertEquals(runTuned(state, r1, 100000), runTuned(copy, r2, 100000));
        assertEquals(state.elapsed(), copy.elapsed());
    }

    @Test
    void aBadArrayLeavesTheStateInactive() {
        ReactionState state = new ReactionState();
        state.fromArray(new int[] { 1, 2 });
        assertFalse(state.isActive());
        state.fromArray(null);
        assertFalse(state.isActive());
    }

    @Test
    void anInactiveStateIgnoresTicks() {
        ReactionState state = new ReactionState();
        assertEquals(ReactionState.Outcome.RUNNING, state.tick(3, new Random(1)));
        assertEquals(0, state.elapsed());
    }

    @Test
    void intensityIsClampedToZeroToTen() {
        // GGS-08-28
        assertEquals(0, ReactionState.clampIntensity(-5));
        assertEquals(10, ReactionState.clampIntensity(11));
        assertEquals(7, ReactionState.clampIntensity(7));
    }
}
