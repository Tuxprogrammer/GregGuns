package com.gregguns.entity.npc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

import com.gregguns.entity.npc.HelicopterRules.Action;
import com.gregguns.entity.npc.loot.NpcProfiles;

/** The Attack Helicopter's stats, attack cycle, waypoints and crash clock (M7 B4). */
class HelicopterRulesTest {

    @Test
    void statsMatchTheSpec() {
        assertEquals(100.0, HelicopterRules.MAX_HEALTH);
        assertEquals(10.0F, HelicopterRules.WIDTH);
        assertEquals(4.0F, HelicopterRules.HEIGHT);
        assertEquals(50, HelicopterRules.XP);
        assertTrue(DespawnRules.isPersistent(NpcMob.ATTACK_HELICOPTER));
    }

    @Test
    void oneCycleIsSixteenTicksOfAimingThenSevenBulletsAndARocket() {
        int counter = HelicopterRules.WIND_UP;
        int bursts = 0;
        int bullets = 0;
        int rockets = 0;
        int ticks = 0;
        // Start a cycle at the wind-up and run until the second rocket, counting the actions of one full cycle.
        for (int i = 0; i < 200 && rockets < 2; i++) {
            counter = HelicopterRules.stepCounter(counter, true);
            Action action = HelicopterRules.actionAt(counter);
            if (rockets == 1) {
                ticks++;
            }
            if (action == Action.BURST_SOUND) {
                bursts++;
            } else if (action == Action.MACHINE_GUN) {
                bullets++;
            } else if (action == Action.ROCKET) {
                rockets++;
            }
            counter = HelicopterRules.afterAction(counter);
        }
        assertEquals(2, bursts);
        assertEquals(14, bullets);
        assertEquals(2, rockets);
        assertEquals(70, ticks);
    }

    @Test
    void bulletsFireOnOddCountersSeventeenToTwentyNine() {
        int bullets = 0;
        for (int c = 0; c < 40; c++) {
            if (HelicopterRules.actionAt(c) == Action.MACHINE_GUN) {
                bullets++;
                assertTrue(c % 2 == 1 && c > 16 && c < 30, "counter " + c);
            }
        }
        assertEquals(7, bullets);
        assertEquals(Action.BURST_SOUND, HelicopterRules.actionAt(16));
        assertEquals(Action.ROCKET, HelicopterRules.actionAt(30));
        assertEquals(HelicopterRules.WIND_UP, HelicopterRules.afterAction(30));
    }

    @Test
    void counterDecaysTowardZeroWhenTheTargetIsHidden() {
        assertEquals(9, HelicopterRules.stepCounter(10, false));
        assertEquals(0, HelicopterRules.stepCounter(0, false));
        assertEquals(-39, HelicopterRules.stepCounter(-40, false));
        assertEquals(11, HelicopterRules.stepCounter(10, true));
    }

    @Test
    void doesNotReproduceNpc10_pitchSignKeepsSignInAimAngles() {
        // NPC-10: Techguns took an acos, so it could never aim upward. Our pitch is signed.
        assertTrue(AimAngles.pitch(10, 8, 0) < 0.0F, "a target above gives a negative (upward) pitch");
        assertTrue(AimAngles.pitch(10, -8, 0) > 0.0F, "a target below gives a positive (downward) pitch");
    }

    @Test
    void deathProgressRisesFromZeroToOne() {
        assertEquals(0.0F, HelicopterRules.deathProgress(0, 0.5F));
        assertEquals(0.5F, HelicopterRules.deathProgress(50, 0.0F), 1e-6F);
        assertEquals(1.0F, HelicopterRules.deathProgress(HelicopterRules.CRASH_TICKS, 0.0F));
        assertEquals(1.0F, HelicopterRules.deathProgress(500, 0.9F));
    }

    @Test
    void waypointIsDueWhenReachedOrTooFar() {
        assertTrue(HelicopterRules.needsWaypoint(0.5));
        assertTrue(HelicopterRules.needsWaypoint(3601.0));
        assertTrue(!HelicopterRules.needsWaypoint(100.0));
    }

    @Test
    void waypointsStayInTheAltitudeBandAndWanderWithoutATarget() {
        Random rand = new Random(7);
        for (int i = 0; i < 500; i++) {
            double[] wp = HelicopterRules.pickWaypoint(rand, 0, 70, 0, 64, false, 0, 0);
            assertTrue(wp[1] >= 64 + HelicopterRules.MIN_ALTITUDE && wp[1] <= 64 + HelicopterRules.MAX_ALTITUDE);
            assertTrue(Math.abs(wp[0]) <= HelicopterRules.WANDER && Math.abs(wp[2]) <= HelicopterRules.WANDER);
        }
    }

    @Test
    void waypointClosesInBacksOffAndStrafes() {
        Random rand = new Random(11);
        for (int i = 0; i < 200; i++) {
            double[] far = HelicopterRules.pickWaypoint(rand, 0, 90, 0, 64, true, 0, 80);
            assertTrue(far[2] > 0.0, "a far target pulls the waypoint toward it");
            double[] near = HelicopterRules.pickWaypoint(rand, 0, 90, 0, 64, true, 0, 10);
            assertTrue(near[2] < 0.0, "a near target pushes the waypoint away");
            double[] ring = HelicopterRules.pickWaypoint(rand, 0, 90, 0, 64, true, 0, 40);
            assertTrue(Math.abs(ring[0]) > 8.0, "on the ring it moves sideways");
        }
    }

    @Test
    void profileDropsTheSpecItems() {
        assertNotNull(NpcProfiles.get("AttackHelicopter"));
        assertEquals(
            4,
            NpcProfiles.get("AttackHelicopter")
                .fixedDrops()
                .size());
    }
}
