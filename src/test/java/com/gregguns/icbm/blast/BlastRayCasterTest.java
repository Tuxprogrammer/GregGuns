package com.gregguns.icbm.blast;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * The ported ray caster, with a regression test per bug in its {@code Avoids:} line: ICBM-02-02 (resistance charged
 * once per block), ICBM-02-03 (each direction once), ICBM-02-04 (no duplicate edits), ICBM-02-05 (the radius is
 * reachable) and ICBM-02-60 (a set, not a list scan).
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/blast-engine.md
class BlastRayCasterTest {

    private static final int R = 10;

    private static BlastGrid solid(short cell) {
        BlastGrid g = new BlastGrid(R);
        for (int x = -R; x <= R; x++) {
            for (int y = -R; y <= R; y++) {
                for (int z = -R; z <= R; z++) {
                    g.set(x, y, z, cell);
                }
            }
        }
        return g;
    }

    private static int[] cast(BlastGrid g, double energy) {
        return BlastRayCaster.cast(g, R, 0.5, 0.5, 0.5, energy, new Random(7), () -> false);
    }

    private static double farthest(int[] cells) {
        double far = 0.0;
        for (int p : cells) {
            double x = BlastPacking.x(p);
            double y = BlastPacking.y(p);
            double z = BlastPacking.z(p);
            far = Math.max(far, Math.sqrt(x * x + y * y + z * z));
        }
        return far;
    }

    @Test
    void icbm0205TheConfiguredRadiusIsReachableThroughOpenGlass() {
        // Weak glass (resistance 0.3) everywhere: with the default energy the blast fills the radius. The original's
        // energy of 80 against a radius of 50 stopped at 35 blocks (ICBM-02-05).
        BlastGrid g = solid(BlastGrid.encode(0.3F));
        int[] cells = cast(g, BlastRayCaster.defaultEnergy(R));
        assertTrue(farthest(cells) >= R - 1.5, "reached " + farthest(cells));
    }

    @Test
    void icbm0202AThickBlockIsChargedOncePerRayNotOncePerHalfStep() {
        // A wall of resistance 4 in a column straight above the center, 3 blocks thick: one crossing costs 3 x 4 = 12
        // plus steps. Charged at every half-step (the bug) it would cost twice that and the far side would be safe.
        BlastGrid g = new BlastGrid(R);
        for (int y = 2; y <= 4; y++) {
            g.set(0, y, 0, BlastGrid.encode(4.0F));
        }
        for (int y = 5; y <= 8; y++) {
            g.set(0, y, 0, BlastGrid.encode(0.1F));
        }
        // Energy for exactly the straight-up ray at full strength: steps to y=8 is 16 half-steps (18) + wall 12.
        double energy = 2.0 * (16 * BlastRayCaster.STEP_COST + 12.0 + 3.0);
        boolean farSideHit = false;
        int[] cells = BlastRayCaster.cast(g, R, 0.5, 0.5, 0.5, energy, new Random(1), () -> false);
        for (int p : cells) {
            if (BlastPacking.x(p) == 0 && BlastPacking.z(p) == 0 && BlastPacking.y(p) == 8) {
                farSideHit = true;
            }
        }
        assertTrue(farSideHit, "the ray should cross a 3-thick wall paying its resistance once per block");
    }

    @Test
    void icbm0203EachDirectionIsCastOnce() {
        int steps = BlastRayCaster.steps(R);
        assertEquals(2L * steps * steps + 2L * steps, BlastRayCaster.rayCount(R));
        // The original swept yaw to 4 pi and cast 4 x steps x steps rays.
        assertTrue(BlastRayCaster.rayCount(R) < 4L * steps * steps);
    }

    @Test
    void stepCountIsEvenSoOneRayRunsAlongTheSurface() {
        // With an odd count no pitch slice is horizontal, and the edge of a level crater fell short of its radius
        // (thermobaric r20 on stone missed 18 blocks along the surface about one blast in twenty).
        for (int radius = 3; radius <= 64; radius++) {
            assertEquals(0, BlastRayCaster.steps(radius) % 2, "steps for radius " + radius);
        }
    }

    @Test
    void icbm0204AndIcbm0260EveryEditAppearsOnceAndNearestFirst() {
        BlastGrid g = solid(BlastGrid.encode(0.3F));
        int[] cells = cast(g, BlastRayCaster.defaultEnergy(R));
        Set<Integer> seen = new HashSet<>();
        long previous = -1;
        for (int p : cells) {
            assertTrue(seen.add(p), "duplicate edit " + p);
            long d2 = (long) BlastPacking.x(p) * BlastPacking.x(p) + (long) BlastPacking.y(p) * BlastPacking.y(p)
                + (long) BlastPacking.z(p) * BlastPacking.z(p);
            assertTrue(d2 >= previous, "sorted nearest first");
            previous = d2;
        }
        assertTrue(cells.length > 1000);
    }

    @Test
    void airIsNeverAnEditAndUnbreakableBlocksShieldWhatIsBehindThem() {
        BlastGrid g = new BlastGrid(R);
        for (int x = -R; x <= R; x++) {
            for (int y = -R; y <= R; y++) {
                for (int z = -R; z <= R; z++) {
                    g.set(x, y, z, y >= 3 ? BlastGrid.encode(1.0F) : BlastGrid.AIR);
                }
            }
        }
        for (int x = -R; x <= R; x++) {
            for (int z = -R; z <= R; z++) {
                g.set(x, 2, z, BlastGrid.UNBREAKABLE);
            }
        }
        int[] cells = cast(g, BlastRayCaster.defaultEnergy(R));
        assertEquals(0, cells.length, "a bedrock ceiling keeps every ray from the stone above it");
    }

    @Test
    void unloadedCellsEndTheRayInsteadOfLoadingTheChunk() {
        BlastGrid g = solid(BlastGrid.UNLOADED);
        assertEquals(0, cast(g, 1000.0).length);
    }

    @Test
    void aCancelledCastStopsEarly() {
        BlastGrid g = solid(BlastGrid.encode(0.3F));
        int[] cells = BlastRayCaster
            .cast(g, R, 0.5, 0.5, 0.5, BlastRayCaster.defaultEnergy(R), new Random(7), () -> true);
        assertEquals(0, cells.length);
    }

    @Test
    void theSmallCasterCratersAndStopsAtUnbreakableBlocks() {
        BlastGrid g = solid(BlastGrid.encode(0.5F));
        int[] cells = BlastRayCaster
            .castGrid(g, R, 0.5, 0.5, 0.5, BlastRayCaster.gridEnergy(R), new Random(3), () -> false);
        assertTrue(cells.length > 100);
        assertTrue(farthest(cells) <= R + 1.0);
        assertEquals(
            0,
            BlastRayCaster.castGrid(
                solid(BlastGrid.UNBREAKABLE),
                R,
                0.5,
                0.5,
                0.5,
                BlastRayCaster.gridEnergy(R),
                new Random(3),
                () -> false).length);
    }

    /** Vanilla stone: setResistance(10) stores 30, and the engine reads it divided by 5. */
    private static final float STONE = 6.0F;

    /** Stone below the center cell's floor, air above: the center sits on the surface. */
    private static BlastGrid halfSpace(int radius) {
        BlastGrid g = new BlastGrid(radius);
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= -1; y++) {
                for (int z = -radius; z <= radius; z++) {
                    g.set(x, y, z, BlastGrid.encode(STONE));
                }
            }
        }
        return g;
    }

    private static int depth(int[] cells) {
        int d = 0;
        for (int p : cells) {
            d = Math.max(d, -BlastPacking.y(p));
        }
        return d;
    }

    private static double hemisphere(int radius) {
        return 2.0 / 3.0 * Math.PI * radius * radius * radius;
    }

    @Test
    void d59TheSmallCasterDigsTheRadiusIntoStone() {
        // Vanilla stone (resistance 6): a radius-9 grid blast digs about 9 deep and fills over half a hemisphere.
        int radius = 9;
        for (long seed = 1; seed <= 3; seed++) {
            int[] cells = BlastRayCaster.castGrid(
                halfSpace(radius),
                radius,
                0.5,
                0.5,
                0.5,
                BlastRayCaster.gridEnergy(radius),
                new Random(seed),
                () -> false);
            assertTrue(depth(cells) >= radius - 2, "depth " + depth(cells));
            assertTrue(cells.length >= 0.5 * hemisphere(radius), "cells " + cells.length);
            assertTrue(farthest(cells) <= radius + 1.0);
        }
    }

    @Test
    void d59TheSphereCasterDigsTheRadiusIntoStone() {
        for (int radius : new int[] { 9, 20 }) {
            int[] cells = BlastRayCaster.cast(
                halfSpace(radius),
                radius,
                0.5,
                0.5,
                0.5,
                BlastRayCaster.defaultEnergy(radius),
                new Random(1),
                () -> false);
            assertTrue(depth(cells) >= radius - 2, "r" + radius + " depth " + depth(cells));
            // No spire of stone under the center: the column straight down is dug too.
            java.util.Set<Integer> set = new HashSet<>();
            for (int p : cells) {
                set.add(p);
            }
            int column = 0;
            while (set.contains(BlastPacking.pack(0, -1 - column, 0))) {
                column++;
            }
            assertTrue(column >= radius - 2, "r" + radius + " column " + column);
            assertTrue(cells.length >= 0.5 * hemisphere(radius), "r" + radius + " cells " + cells.length);
            assertTrue(farthest(cells) <= radius + 1.0);
        }
    }

    @Test
    void d59TheOldEnergyBarelyScratchedStone() {
        // The radius itself as the energy (the original) breaks a few dozen blocks, a few deep: the bug D59 fixes.
        int radius = 9;
        int[] cells = BlastRayCaster
            .castGrid(halfSpace(radius), radius, 0.5, 0.5, 0.5, radius, new Random(1), () -> false);
        assertTrue(cells.length < 0.1 * hemisphere(radius), "cells " + cells.length);
    }

    @Test
    void packingRoundTripsOffsets() {
        for (int v : new int[] { -255, -1, 0, 1, 255 }) {
            int p = BlastPacking.pack(v, -v, v);
            assertEquals(v, BlastPacking.x(p));
            assertEquals(-v, BlastPacking.y(p));
            assertEquals(v, BlastPacking.z(p));
        }
    }
}
