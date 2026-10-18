package com.gregguns.gun.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

class SpreadAndZoomTest {

    private static final GunStats SCOPED = GunStats.builder("scoped")
        .ammoType("a")
        .magazine(10)
        .spread(0.1)
        .zoom(0.35, 0.5)
        .build();

    private static final GunStats SHOTGUN = GunStats.builder("shotgun")
        .ammoType("a")
        .magazine(8)
        .spread(0.05)
        .pellets(4, 0.2, false)
        .build();

    private static final GunStats LINE = GunStats.builder("line")
        .ammoType("a")
        .magazine(8)
        .spread(0.0)
        .pellets(2, 0.0, true)
        .build();

    @Test
    void pistolSpreadIsPlusMinusOneDegree() {
        assertEquals(1.0, SpreadModel.maxDegrees(GunCatalogue.PISTOL.spread()), 1e-9);
        Random rnd = new Random(42);
        for (int i = 0; i < 2000; i++) {
            ShotOffset o = SpreadModel.roll(rnd, GunCatalogue.PISTOL, ShotContext.DEFAULT, 0)[0];
            assertTrue(Math.abs(o.yawDegrees()) <= 1.0);
            assertTrue(Math.abs(o.pitchDegrees()) <= 1.0);
        }
    }

    @Test
    void sameSeedGivesTheSameOffsets() {
        ShotOffset[] a = SpreadModel.roll(new Random(7), SHOTGUN, ShotContext.DEFAULT, 0);
        ShotOffset[] b = SpreadModel.roll(new Random(7), SHOTGUN, ShotContext.DEFAULT, 0);
        for (int i = 0; i < a.length; i++) {
            assertEquals(a[i].yawDegrees(), b[i].yawDegrees());
            assertEquals(a[i].pitchDegrees(), b[i].pitchDegrees());
        }
    }

    @Test
    void offsetsFillTheRangeAndAreCentred() {
        Random rnd = new Random(1);
        double max = 0;
        double sum = 0;
        int n = 20000;
        for (int i = 0; i < n; i++) {
            double yaw = SpreadModel.roll(rnd, SCOPED, ShotContext.DEFAULT, 0)[0].yawDegrees();
            max = Math.max(max, Math.abs(yaw));
            sum += yaw;
        }
        assertTrue(max > 3.9 && max <= 4.0, "max " + max);
        assertTrue(Math.abs(sum / n) < 0.05);
    }

    @Test
    void zoomTightensSpreadOnlyForAScopedGun() {
        assertEquals(0.1, SpreadModel.mainSpread(SCOPED, ShotContext.player(false, 0)), 1e-12);
        assertEquals(0.05, SpreadModel.mainSpread(SCOPED, ShotContext.player(true, 0)), 1e-12);
        GunStats unscoped = GunStats.builder("u")
            .ammoType("a")
            .magazine(10)
            .spread(0.1)
            .zoom(1.0, 0.5)
            .build();
        assertEquals(0.1, SpreadModel.mainSpread(unscoped, ShotContext.player(true, 0)), 1e-12);
    }

    @Test
    void armourBonusReducesSpreadAndStacksWithZoom() {
        assertEquals(0.09, SpreadModel.mainSpread(SCOPED, ShotContext.player(false, 0.1)), 1e-12);
        assertEquals(0.045, SpreadModel.mainSpread(SCOPED, ShotContext.player(true, 0.1)), 1e-12);
        assertEquals(0.0, SpreadModel.mainSpread(SCOPED, ShotContext.player(false, 5.0)), 1e-12);
    }

    @Test
    void pelletsUseTheirOwnSpread() {
        ShotOffset[] shot = SpreadModel.roll(new Random(3), SHOTGUN, ShotContext.DEFAULT, 0);
        assertEquals(5, shot.length);
        Random rnd = new Random(99);
        for (int i = 0; i < 500; i++) {
            ShotOffset[] s = SpreadModel.roll(rnd, SHOTGUN, ShotContext.DEFAULT, 0);
            assertTrue(Math.abs(s[0].yawDegrees()) <= 2.0);
            for (int p = 1; p < s.length; p++) {
                assertTrue(Math.abs(s[p].yawDegrees()) <= 8.0);
            }
        }
    }

    @Test
    void inLinePelletsAreSpacedAlongTheFlightPath() {
        ShotOffset[] s = SpreadModel.roll(new Random(0), LINE, ShotContext.DEFAULT, 0);
        assertEquals(3, s.length);
        assertEquals(0.0, s[0].forwardTickFraction());
        assertEquals(0.5, s[1].forwardTickFraction(), 1e-12);
        assertEquals(1.0, s[2].forwardTickFraction(), 1e-12);
    }

    @Test
    void handsFollowTheAkimboRules() {
        GunStats alternating = GunStats.builder("a")
            .ammoType("a")
            .magazine(10)
            .akimbo(false)
            .build();
        GunStats dual = GunStats.builder("d")
            .ammoType("a")
            .magazine(10)
            .akimbo(true)
            .build();
        assertFalse(SpreadModel.roll(new Random(0), alternating, ShotContext.DEFAULT, 0)[0].leftHand());
        assertTrue(SpreadModel.roll(new Random(0), alternating, ShotContext.DEFAULT, 1)[0].leftHand());
        ShotOffset[] both = SpreadModel.roll(new Random(0), dual, ShotContext.DEFAULT, 0);
        assertEquals(2, both.length);
        assertFalse(both[0].leftHand());
        assertTrue(both[1].leftHand());
    }

    @Test
    void zoomToggleAndFovFactor() {
        ZoomState z = new ZoomState();
        assertEquals(1.0, z.fovMultiplier(SCOPED));
        assertTrue(z.toggle(SCOPED));
        assertTrue(z.isZoomed(SCOPED));
        assertEquals(0.35, z.fovMultiplier(SCOPED));
        assertTrue(z.toggle(SCOPED));
        assertFalse(z.isZoomed(SCOPED));
        z.toggle(SCOPED);
        z.reset();
        assertEquals(1.0, z.fovMultiplier(SCOPED));
    }

    @Test
    void doesNotReproduceGUN47_anUnscopedGunIsNeverZooming() {
        ZoomState z = new ZoomState();
        assertFalse(z.toggle(GunCatalogue.PISTOL));
        assertFalse(z.isZoomed(GunCatalogue.PISTOL));
        assertEquals(1.0, z.fovMultiplier(GunCatalogue.PISTOL));
    }

    @Test
    void doesNotReproduceGUN12_zoomedSpreadComesFromServerStateNotAFlag() {
        // The only input to spread is the context built from ZoomState; there is no flag parameter to forge.
        ZoomState server = new ZoomState();
        ShotContext ctx = ShotContext.player(server.isZoomed(SCOPED), 0);
        assertEquals(0.1, SpreadModel.mainSpread(SCOPED, ctx), 1e-12);
        server.toggle(SCOPED);
        ctx = ShotContext.player(server.isZoomed(SCOPED), 0);
        assertEquals(0.05, SpreadModel.mainSpread(SCOPED, ctx), 1e-12);
    }

    @Test
    void rollsAreStableAcrossRuns() {
        ShotOffset o = SpreadModel.roll(new Random(12345), GunCatalogue.PISTOL, ShotContext.DEFAULT, 0)[0];
        Random r = new Random(12345);
        double yaw = (r.nextDouble() * 2 - 1) * 1.0;
        double pitch = (r.nextDouble() * 2 - 1) * 1.0;
        assertArrayEquals(new double[] { yaw, pitch }, new double[] { o.yawDegrees(), o.pitchDegrees() }, 0.0);
    }
}
