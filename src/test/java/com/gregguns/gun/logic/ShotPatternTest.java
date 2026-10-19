package com.gregguns.gun.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

class ShotPatternTest {

    private static GunStats gun(ShotPattern pattern, double spread) {
        return GunStats.builder("p")
            .ammoType("a")
            .magazine(10)
            .spread(spread)
            .zoom(0.5, 0.0)
            .pattern(pattern)
            .build();
    }

    @Test
    void defaultIsNoPatternAndKeepsPelletBehavior() {
        GunStats plain = GunStats.builder("x")
            .ammoType("a")
            .magazine(4)
            .build();
        assertFalse(
            plain.pattern()
                .isActive());
        assertEquals(1, ShotPattern.NONE.projectileCount());
        assertEquals(1, SpreadModel.roll(new Random(1), plain, ShotContext.DEFAULT, 0).length);
    }

    @Test
    void burstLineShiftsEachProjectileByAFractionOfATick() {
        GunStats pulse = gun(ShotPattern.burstLine(2), 0.02);
        ShotOffset[] o = SpreadModel.roll(new Random(7), pulse, ShotContext.DEFAULT, 0);
        assertEquals(3, o.length);
        assertEquals(0.0, o[0].forwardTickFraction(), 1e-12);
        assertEquals(0.5, o[1].forwardTickFraction(), 1e-12);
        assertEquals(1.0, o[2].forwardTickFraction(), 1e-12);
        for (ShotOffset s : o) {
            assertEquals(o[0].yawDegrees(), s.yawDegrees(), 1e-12);
            assertEquals(o[0].pitchDegrees(), s.pitchDegrees(), 1e-12);
            assertNull(s.volley());
        }
    }

    @Test
    void pdwBurstLineIsTwoProjectiles() {
        ShotOffset[] o = SpreadModel.roll(new Random(3), gun(ShotPattern.burstLine(1), 0.0), ShotContext.DEFAULT, 0);
        assertEquals(2, o.length);
        assertEquals(1.0, o[1].forwardTickFraction(), 1e-12);
    }

    @Test
    void burstLineSpreadStaysInsideTheMainSpread() {
        GunStats g = gun(ShotPattern.burstLine(1), 0.05);
        Random rnd = new Random(11);
        for (int i = 0; i < 500; i++) {
            ShotOffset s = SpreadModel.roll(rnd, g, ShotContext.DEFAULT, 0)[0];
            assertTrue(Math.abs(s.yawDegrees()) <= 2.0 + 1e-9);
            assertTrue(Math.abs(s.pitchDegrees()) <= 2.0 + 1e-9);
        }
    }

    @Test
    void sonicRingVolleyIsSixteenProjectiles() {
        ShotPattern sonic = ShotPattern.ringVolley(2, 5, 0.075);
        assertEquals(16, sonic.projectileCount());
        ShotOffset[] o = SpreadModel.roll(new Random(5), gun(sonic, 0.3), ShotContext.DEFAULT, 0);
        assertEquals(16, o.length);
        assertEquals(0.0, o[0].yawDegrees(), 0.0);
        assertEquals(0.0, o[0].pitchDegrees(), 0.0);
    }

    @Test
    void ringsSitAtTheConeAngleOfTheirRing() {
        ShotOffset[] o = SpreadModel
            .roll(new Random(5), gun(ShotPattern.ringVolley(2, 5, 0.075), 0.0), ShotContext.DEFAULT, 0);
        for (int i = 1; i <= 5; i++) {
            assertEquals(Math.toDegrees(0.075), coneDegrees(o[i]), 0.02);
        }
        for (int i = 6; i <= 15; i++) {
            assertEquals(Math.toDegrees(0.15), coneDegrees(o[i]), 0.05);
        }
    }

    @Test
    void ringProjectilesAreEvenlySpacedAroundTheAxis() {
        ShotOffset[] o = SpreadModel
            .roll(new Random(5), gun(ShotPattern.ringVolley(1, 4, 0.1), 0.0), ShotContext.DEFAULT, 0);
        // The first ring projectile is at ring angle 0 (sideways, +yaw), then up, then -yaw, then down.
        assertTrue(o[1].yawDegrees() > 0 && Math.abs(o[1].pitchDegrees()) < 1e-9);
        assertTrue(o[2].pitchDegrees() > 0 && Math.abs(o[2].yawDegrees()) < 1e-9);
        assertTrue(o[3].yawDegrees() < 0);
        assertTrue(o[4].pitchDegrees() < 0);
    }

    @Test
    void ringVolleyIgnoresSpreadAndZoomAndSharesOneHitList() {
        ShotPattern sonic = ShotPattern.ringVolley(2, 5, 0.075);
        GunStats g = gun(sonic, 0.9);
        ShotOffset[] a = SpreadModel.roll(new Random(1), g, ShotContext.player(true, 0.5), 0);
        ShotOffset[] b = SpreadModel.roll(new Random(99), g, ShotContext.DEFAULT, 0);
        for (int i = 0; i < a.length; i++) {
            assertEquals(a[i].yawDegrees(), b[i].yawDegrees(), 1e-12);
            assertEquals(a[i].pitchDegrees(), b[i].pitchDegrees(), 1e-12);
            assertNotNull(a[i].volley());
            assertSame(a[0].volley(), a[i].volley());
        }
        assertNotSame(a[0].volley(), b[0].volley(), "each volley gets its own list");
    }

    @Test
    void volleyHitsHurtEachEntityOnce() {
        VolleyHits hits = new VolleyHits();
        assertTrue(hits.firstHit(42));
        assertFalse(hits.firstHit(42));
        assertTrue(hits.firstHit(43));
        assertEquals(2, hits.count());
    }

    @Test
    void fanMakesFiveBeamsEachInsideTheSpread() {
        GunStats scatter = gun(ShotPattern.fan(5, 0.15), 0.1);
        Random rnd = new Random(21);
        double sumAbs = 0;
        for (int i = 0; i < 400; i++) {
            ShotOffset[] o = SpreadModel.roll(rnd, scatter, ShotContext.DEFAULT, 0);
            assertEquals(5, o.length);
            for (ShotOffset s : o) {
                assertTrue(Math.abs(s.yawDegrees()) <= 6.0 + 1e-9);
                assertTrue(Math.abs(s.pitchDegrees()) <= 6.0 + 1e-9);
                sumAbs += Math.abs(s.yawDegrees());
            }
        }
        assertTrue(sumAbs / 2000 > 2.0, "beams should actually fan out");
    }

    @Test
    void fanIsTightenedByTheShootersAccuracyAndSeededRollsRepeat() {
        GunStats scatter = gun(ShotPattern.fan(5, 0.15), 0.0);
        ShotOffset[] tight = SpreadModel.roll(new Random(4), scatter, ShotContext.player(false, 1.0), 0);
        for (ShotOffset s : tight) {
            assertEquals(0.0, s.yawDegrees(), 1e-12);
        }
        ShotOffset[] a = SpreadModel.roll(new Random(4), scatter, ShotContext.DEFAULT, 0);
        ShotOffset[] b = SpreadModel.roll(new Random(4), scatter, ShotContext.DEFAULT, 0);
        for (int i = 0; i < a.length; i++) {
            assertEquals(a[i].yawDegrees(), b[i].yawDegrees(), 0.0);
        }
    }

    @Test
    void rejectsInvalidPatterns() {
        assertThrows(IllegalArgumentException.class, () -> ShotPattern.burstLine(0));
        assertThrows(IllegalArgumentException.class, () -> ShotPattern.ringVolley(0, 5, 0.1));
        assertThrows(IllegalArgumentException.class, () -> ShotPattern.ringVolley(2, 5, 0.0));
        assertThrows(IllegalArgumentException.class, () -> ShotPattern.fan(1, 0.1));
    }

    private static double coneDegrees(ShotOffset o) {
        double yaw = Math.toRadians(o.yawDegrees());
        double pitch = Math.toRadians(o.pitchDegrees());
        double x = Math.sin(yaw) * Math.cos(pitch);
        double y = Math.sin(pitch);
        double z = Math.cos(yaw) * Math.cos(pitch);
        return Math.toDegrees(Math.acos(z / Math.sqrt(x * x + y * y + z * z)));
    }
}
