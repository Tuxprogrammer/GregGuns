package com.gregguns.icbm.defense;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.Test;

class SamRulesTest {

    @Test
    void oneSamKillsT1AndT2AndTwoKillT3() {
        assertEquals(1, SamRules.stillNeeded(20.0F, 0));
        assertEquals(1, SamRules.stillNeeded(40.0F, 0));
        assertEquals(2, SamRules.stillNeeded(60.0F, 0));
        assertEquals(1, SamRules.stillNeeded(60.0F, 1));
        assertEquals(0, SamRules.stillNeeded(60.0F, 2));
        assertEquals(0, SamRules.stillNeeded(20.0F, 1));
    }

    @Test
    void aSamIsFasterThanEveryMissile() {
        assertTrue(SamRules.SPEED > 1.6);
    }

    /** D126: the SAM beats the steepest dive any tier makes on its shortest flight, from the arc itself. */
    @Test
    void aSamOutrunsTheSteepestDiveOfEveryTierAtItsShortestRange() {
        for (com.gregguns.icbm.missile.MissileTier t : com.gregguns.icbm.missile.MissileTier.values()) {
            double worst = 0.0;
            for (int flat = t.minRange(); flat <= Math.min(t.baseMaxRange(), 3000); flat++) {
                worst = Math.max(worst, com.gregguns.icbm.missile.ArcPath.terminalSpeed(t.speed(), flat));
            }
            assertTrue(SamRules.SPEED > worst, t + " dives at " + worst + ", SAM flies " + SamRules.SPEED);
        }
    }

    /** A 4-blocks-per-tick dive (the review's case) is caught: the lead aims below, and the sweep never skips it. */
    @Test
    void aSamCatchesAMissileDivingAtFourBlocksPerTick() {
        double sx = 0;
        double sy = 0;
        double sz = 0;
        double mx = 60;
        double my = 300;
        double mz = 0;
        double[] v = { -1.5, -3.7, 0 };
        boolean hit = false;
        for (int tick = 0; tick < SamRules.LIFETIME && !hit; tick++) {
            double[] aim = SamRules.intercept(sx, sy, sz, mx, my, mz, v[0], v[1], v[2]);
            double[] p = SamRules.step(sx, sy, sz, aim[0], aim[1], aim[2]);
            double nx = mx + v[0];
            double ny = my + v[1];
            double nz = mz + v[2];
            hit = SamRules.bursts(SamRules.closestApproach(sx - mx, sy - my, sz - mz, p[0] - nx, p[1] - ny, p[2] - nz));
            sx = p[0];
            sy = p[1];
            sz = p[2];
            mx = nx;
            my = ny;
            mz = nz;
        }
        assertTrue(hit, "caught");
    }

    @Test
    void theInterceptLeadsAMovingTargetAndAimsAtAStationaryOne() {
        double[] still = SamRules.intercept(0, 0, 0, 30, 10, 0, 0, 0, 0);
        assertEquals(30.0, still[0], 1e-9);
        double[] lead = SamRules.intercept(0, 0, 0, 90, 0, 0, 0, 0, 3.0);
        // 3 sideways per tick, SAM at 9: the meeting is where |(90, 3t)| = 9t, so t = 90 / sqrt(72) ~ 10.6.
        assertEquals(3.0 * 90.0 / Math.sqrt(72.0), lead[2], 1e-6);
        // A target faster than the SAM has no solution: aim at it.
        double[] fast = SamRules.intercept(0, 0, 0, 10, 0, 0, 20, 0, 0);
        assertEquals(10.0, fast[0], 1e-9);
    }

    @Test
    void theSweepSeesAPassThatSkipsTheSphereBetweenSamples() {
        // Head-on at a closing speed of 14 per tick: 7 blocks before, 7 beyond. Neither end is inside 3.5.
        assertFalse(SamRules.bursts(7.0));
        assertTrue(SamRules.bursts(SamRules.closestApproach(7, 0, 0, -7, 0, 0)));
        assertFalse(SamRules.bursts(SamRules.closestApproach(7, 5, 0, -7, 5, 0)));
    }

    @Test
    void theStepMovesOneSpeedTowardTheTargetAndNeverPastIt() {
        double[] p = SamRules.step(0, 0, 0, 30, 0, 0);
        assertEquals(SamRules.SPEED, p[0], 1e-9);
        double[] q = SamRules.step(0, 0, 0, 1, 0, 0);
        assertEquals(1.0, q[0], 1e-9);
    }

    @Test
    void aSamCatchesAMissileFlyingAway() {
        // A T3 missile (1.6 per tick) 100 blocks off, fleeing along X: the SAM closes 1.4 per tick.
        double sx = 0;
        double mx = 100;
        int ticks = 0;
        while (!SamRules.bursts(Math.abs(mx - sx)) && ticks < SamRules.LIFETIME) {
            mx += 1.6;
            sx = SamRules.step(sx, 0, 0, mx, 0, 0)[0];
            ticks++;
        }
        assertTrue(ticks < SamRules.LIFETIME, "caught after " + ticks);
    }

    @Test
    void burstsOnlyInsideTheKillRadius() {
        assertTrue(SamRules.bursts(SamRules.KILL_RADIUS));
        assertFalse(SamRules.bursts(SamRules.KILL_RADIUS + 0.1));
    }

    @Test
    void theLinkRoundTripsAndAnEmptyOrDamagedTagIsNoLink() {
        NBTTagCompound tag = new NBTTagCompound();
        assertNull(RadarLink.read(tag));
        assertNull(RadarLink.read(null));
        new RadarLink(-1, 10, 64, -20).write(tag);
        RadarLink l = RadarLink.read(tag);
        assertNotNull(l);
        assertEquals(new RadarLink(-1, 10, 64, -20), l);
        tag.getCompoundTag(RadarLink.TAG)
            .removeTag("y");
        assertNull(RadarLink.read(tag));
    }

    @Test
    void fireRefusalNamesTheFirstFailedCheck() {
        assertNull(SamRules.fireRefusal(true, true, true, 2, true, 0, true, true));
        assertEquals("module_off", SamRules.fireRefusal(false, true, true, 2, true, 0, true, true));
        assertEquals("no_link", SamRules.fireRefusal(true, false, true, 2, true, 0, true, true));
        assertEquals("no_link", SamRules.fireRefusal(true, true, false, 2, true, 0, true, true));
        assertEquals("no_sams", SamRules.fireRefusal(true, true, true, 0, true, 0, true, true));
        assertEquals("no_contact", SamRules.fireRefusal(true, true, true, 2, true, 0, false, true));
        assertEquals("out_of_range", SamRules.fireRefusal(true, true, true, 2, true, 0, true, false));
        assertEquals("no_power", SamRules.fireRefusal(true, true, true, 2, false, 0, true, true));
        assertEquals("cooling", SamRules.fireRefusal(true, true, true, 2, true, 5, true, true));
    }
}
