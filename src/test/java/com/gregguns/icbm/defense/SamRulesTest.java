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
