package com.gregguns.projectile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.gregguns.core.config.ExplosionsConfig;

/** The pure rules of the rocket and the 40 mm grenade: blast radii, damage split, fuse and block damage. */
// Spec: docs/techguns-spec/04-projectiles-fx/projectiles.md (3.4, 3.7); docs/gregguns-spec/02-guns/index.md (B5)
class LauncherMathTest {

    private static final double EPS = 1.0e-9;

    @Test
    void radiiAreHalfAndOneAndAHalfTimesTheBlastRadius() {
        assertEquals(0.875, LauncherMath.innerRadius(1.75), EPS);
        assertEquals(2.625, LauncherMath.outerRadius(1.75), EPS);
        assertEquals(0.75, LauncherMath.innerRadius(1.5), EPS);
        assertEquals(2.25, LauncherMath.outerRadius(1.5), EPS);
    }

    @Test
    void splashIsFullInsideHalfTheRadiusRampsDownAndEndsAtOneAndAHalf() {
        assertEquals(50.0, LauncherMath.splashDamage(50.0, 1.75, 0.0), EPS);
        assertEquals(50.0, LauncherMath.splashDamage(50.0, 1.75, 0.875), EPS);
        // Halfway down the ramp: 0.875 + 0.875 = 1.75 blocks.
        assertEquals(25.0, LauncherMath.splashDamage(50.0, 1.75, 1.75), EPS);
        assertEquals(0.0, LauncherMath.splashDamage(50.0, 1.75, 2.625), EPS);
        assertEquals(0.0, LauncherMath.splashDamage(50.0, 1.75, 3.0), EPS);
        assertEquals(15.0, LauncherMath.splashDamage(30.0, 1.5, 1.5), EPS);
    }

    @Test
    void theDirectHitIsTheFullDamageAndTheSplashAtThatSpotIsNeverMore() {
        assertEquals(50.0, LauncherMath.directDamage(50.0), EPS);
        for (double d = 0.0; d <= 3.0; d += 0.25) {
            assertTrue(LauncherMath.splashDamage(50.0, 1.75, d) <= LauncherMath.directDamage(50.0));
        }
        // The split: a target hit directly takes the direct figure once; the same target at 1.75 blocks would only
        // take the ramp, so adding both would overstate by the ramp value.
        assertTrue(LauncherMath.splashDamage(50.0, 1.75, 1.75) < LauncherMath.directDamage(50.0));
    }

    @Test
    void aBounceTakesTwentyTicksOffTheFuseAndNeverBelowOne() {
        assertEquals(80, LauncherMath.bounceFuse(100));
        assertEquals(60, LauncherMath.fuseAfterBounces(100, LauncherMath.GRENADE_BOUNCES));
        assertEquals(1, LauncherMath.bounceFuse(10));
        assertEquals(1, LauncherMath.fuseAfterBounces(30, 5));
        assertEquals(100, LauncherMath.fuseAfterBounces(100, 0));
    }

    @Test
    void aRocketAtSpeedOneFallsAboutThirtySevenBlocksInItsHundredTicks() {
        // Gravity 0.01 against drag 0.99: a nominal 100 block reach falls well short.
        double fall = LauncherMath.fallAfter(0.01, 100);
        assertTrue(fall > 36.0 && fall < 37.5, "fell " + fall);
        assertEquals(0.0, LauncherMath.fallAfter(0.01, 0), EPS);
        assertEquals(0.0, LauncherMath.fallAfter(0.0, 100), EPS);
    }

    @Test
    void fallGrowsEveryTick() {
        double previous = 0.0;
        // Nothing has fallen after one tick (the first step moves by the level launch motion), then it only grows.
        for (int ticks = 2; ticks <= 100; ticks++) {
            double fall = LauncherMath.fallAfter(0.01, ticks);
            assertTrue(fall > previous, "tick " + ticks);
            previous = fall;
        }
    }

    @Test
    void doesNotReproducePFX14_blocksBreakOnlyWithTheConfigOn() {
        assertFalse(LauncherMath.blastMayBreakBlocks(false, true, true));
        assertFalse(LauncherMath.blastMayBreakBlocks(false, false, true));
        assertTrue(LauncherMath.blastMayBreakBlocks(true, true, false));
        // An NPC-fired launcher also needs the world's mobGriefing rule.
        assertTrue(LauncherMath.blastMayBreakBlocks(true, false, true));
        assertFalse(LauncherMath.blastMayBreakBlocks(true, false, false));
        assertFalse(ExplosionsConfig.blockDamage, "block damage defaults to off");
    }

    @Test
    void doesNotReproducePFX25_theShooterIsHurtByDefault() {
        assertTrue(ExplosionsConfig.selfDamage);
    }
}
