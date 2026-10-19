package com.gregguns.entity.npc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

class RangedAttackTimerTest {

    /** Runs {@code ticks} ticks at a fixed distance and returns the (1-based) ticks on which it fired. */
    private static List<Integer> run(RangedAttackTimer t, int ticks, double distance, boolean seen) {
        List<Integer> shots = new ArrayList<>();
        for (int i = 1; i <= ticks; i++) {
            if (t.tick(distance, seen)) {
                shots.add(i);
            }
        }
        return shots;
    }

    @Test
    void singleShotPointBlankWaitsVariance() {
        // attack 60, variance 20: distance 0 -> factor 0, timer 20; tick 1 sets it, fires 20 ticks later
        RangedAttackTimer t = new RangedAttackTimer(new NpcGunProfile(12, 60, 0, 0));
        assertEquals(Arrays.asList(21, 41, 61), run(t, 61, 0.0, true));
    }

    @Test
    void singleShotAtEdgeOfRangeWaitsFullAttackTime() {
        RangedAttackTimer t = new RangedAttackTimer(new NpcGunProfile(12, 60, 0, 0));
        // first tick computes timer = 60; fires on tick 61, then waits 60 again
        assertEquals(Arrays.asList(61, 121), run(t, 121, 12.0, true));
    }

    @Test
    void timerScalesLinearlyWithDistance() {
        // attack 30, variance 10, range 24: at 12 blocks f=0.5 -> floor(0.5*20+10)=20
        RangedAttackTimer t = new RangedAttackTimer(new NpcGunProfile(24, 30, 0, 0));
        t.tick(12.0, true);
        assertEquals(20, t.timer());
    }

    @Test
    void burstFiresShotDelayApart() {
        // AK: range 24, attack 30, burst 3, delay 3; distance 24 -> gap 30
        RangedAttackTimer t = new RangedAttackTimer(new NpcGunProfile(24, 30, 3, 3));
        // tick 1 -> timer 30; shot on tick 31; 3 ticks to the next shots (34, 37); then 30 more (67)
        assertEquals(Arrays.asList(31, 34, 37, 67, 70, 73), run(t, 73, 24.0, true));
    }

    @Test
    void burstCountRefillsAfterBurst() {
        RangedAttackTimer t = new RangedAttackTimer(new NpcGunProfile(24, 30, 3, 3));
        run(t, 37, 24.0, true);
        assertEquals(3, t.burstLeft());
    }

    @Test
    void revolverBurstOfSix() {
        // revolver: range 18, attack 90, burst 6, delay 20, at the edge: 90, then 5 more shots 20 apart
        RangedAttackTimer t = new RangedAttackTimer(new NpcGunProfile(18, 90, 6, 20));
        assertEquals(Arrays.asList(91, 111, 131, 151, 171, 191), run(t, 191, 18.0, true));
    }

    @Test
    void outOfRangeDoesNotFireAndTimerRecomputes() {
        RangedAttackTimer t = new RangedAttackTimer(new NpcGunProfile(12, 60, 0, 0));
        assertTrue(run(t, 200, 30.0, true).isEmpty());
        assertTrue(t.timer() > 0);
    }

    @Test
    void unseenTargetDoesNotFire() {
        RangedAttackTimer t = new RangedAttackTimer(new NpcGunProfile(12, 60, 0, 0));
        assertTrue(run(t, 200, 5.0, false).isEmpty());
    }

    @Test
    void farTimerIsCappedAtAttackTime() {
        RangedAttackTimer t = new RangedAttackTimer(new NpcGunProfile(12, 60, 0, 0));
        t.tick(70.0, true);
        assertEquals(60, t.timer());
    }

    @Test
    void factorIsClampedToTenthAndOne() {
        RangedAttackTimer near = new RangedAttackTimer(new NpcGunProfile(20, 30, 0, 0));
        run(near, 40, 0.0, true);
        assertEquals(0.1, near.lastFactor(), 1e-9);
        RangedAttackTimer edge = new RangedAttackTimer(new NpcGunProfile(20, 30, 0, 0));
        run(edge, 40, 20.0, true);
        assertEquals(1.0, edge.lastFactor(), 1e-9);
        RangedAttackTimer mid = new RangedAttackTimer(new NpcGunProfile(20, 30, 0, 0));
        run(mid, 40, 10.0, true);
        assertEquals(0.5, mid.lastFactor(), 1e-9);
    }

    @Test
    void holdsPositionOnlyAfterTwentySightTicksInRange() {
        RangedAttackTimer t = new RangedAttackTimer(new NpcGunProfile(24, 30, 0, 0));
        for (int i = 0; i < 19; i++) {
            t.tick(10.0, true);
            assertFalse(t.holdPosition(10.0));
        }
        t.tick(10.0, true);
        assertTrue(t.holdPosition(10.0));
        assertFalse(t.holdPosition(30.0), "out of range keeps pathing");
        t.tick(10.0, false);
        assertFalse(t.holdPosition(10.0), "losing sight resets the count");
        assertEquals(0, t.ticksSeen());
    }

    @Test
    void partialBurstCarriesOverLossOfSight() {
        RangedAttackTimer t = new RangedAttackTimer(new NpcGunProfile(24, 30, 3, 3));
        run(t, 31, 24.0, true); // first shot of the burst
        assertEquals(2, t.burstLeft());
        run(t, 20, 24.0, false); // sight lost; the due shot is skipped
        assertEquals(2, t.burstLeft());
    }

    @Test
    void resetDropsStaleTargetState() {
        RangedAttackTimer t = new RangedAttackTimer(new NpcGunProfile(24, 30, 3, 3));
        run(t, 25, 10.0, true);
        t.reset();
        assertEquals(-1, t.timer());
        assertEquals(0, t.ticksSeen());
        // after a reset nothing fires until the timer is recomputed and has run out again
        assertFalse(t.tick(10.0, true));
        assertTrue(t.timer() > 0);
    }

    @Test
    void continuesOnlyWithATarget() {
        // NPC-31: a leftover navigator path must not keep the task alive
        assertTrue(RangedAttackTimer.shouldContinue(true));
        assertFalse(RangedAttackTimer.shouldContinue(false));
        assertTrue(RangedAttackTimer.shouldStart(true));
        assertFalse(RangedAttackTimer.shouldStart(false));
    }
}
