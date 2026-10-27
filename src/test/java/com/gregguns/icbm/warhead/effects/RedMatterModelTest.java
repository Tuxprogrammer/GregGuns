package com.gregguns.icbm.warhead.effects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.Test;

/** The red matter rules (D10, D132): a bounded walk, a bounded pull, a bounded life, a state that survives a save. */
class RedMatterModelTest {

    @Test
    void lifetimeIsBoundedBothWays() {
        assertEquals(100, RedMatterModel.lifetimeTicks(0));
        assertEquals(1200, RedMatterModel.lifetimeTicks(60));
        assertEquals(12000, RedMatterModel.lifetimeTicks(10_000), "clamped to 600 s");
    }

    @Test
    void theMaxRadiusIsTheBlastRadiusAndNeverBelowTheStart() {
        assertEquals(32, RedMatterModel.maxRadius(32.7));
        assertEquals(RedMatterModel.START_RADIUS, RedMatterModel.maxRadius(1.0));
        assertEquals(RedMatterModel.START_RADIUS, RedMatterModel.maxRadius(Double.NaN));
    }

    @Test
    void theShellWalkVisitsEachShellCellOnceAndOnlyShellCells() {
        int radius = 5;
        int inner = 3;
        int total = RedMatterModel.cubeCells(radius);
        int shell = 0;
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (int i = 0; i < total; i++) {
            int[] d = RedMatterModel.decode(i, radius);
            assertTrue(seen.add(d[0] + "," + d[1] + "," + d[2]), "decode is one to one");
            assertTrue(Math.abs(d[0]) <= radius && Math.abs(d[1]) <= radius && Math.abs(d[2]) <= radius);
            if (RedMatterModel.inShell(d[0], d[1], d[2], inner, radius)) {
                shell++;
                double dist = Math.sqrt(d[0] * d[0] + d[1] * d[1] + d[2] * d[2]);
                assertTrue(dist > inner && dist <= radius + 1e-9);
            }
        }
        assertEquals(total, seen.size());
        assertTrue(shell > 0 && shell < total);
        assertFalse(RedMatterModel.inShell(0, 0, 0, 0, 5), "the center is never in a shell");
    }

    @Test
    void shellsOfSuccessivePassesDoNotOverlap() {
        int overlaps = 0;
        int r = 6;
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    boolean a = RedMatterModel.inShell(x, y, z, 3, 4);
                    boolean b = RedMatterModel.inShell(x, y, z, 4, 5);
                    if (a && b) {
                        overlaps++;
                    }
                }
            }
        }
        assertEquals(0, overlaps);
    }

    @Test
    void growthStopsAtTheMaxAndSaysSo() {
        assertEquals(3, RedMatterModel.nextRadius(2, 32));
        assertEquals(-1, RedMatterModel.nextRadius(32, 32));
        assertEquals(-1, RedMatterModel.nextRadius(40, 32));
    }

    @Test
    void thePullIsZeroAtTheCenterOutOfRangeAndForNaN() {
        assertEquals(0.0, RedMatterModel.pullSpeed(0.0, 10), 0.0, "ICBM-02-87: no division by a zero distance");
        assertEquals(0.0, RedMatterModel.pullSpeed(Double.NaN, 10), 0.0);
        assertEquals(0.0, RedMatterModel.pullSpeed(RedMatterModel.pullRange(10), 10), 0.0);
        assertEquals(0.0, RedMatterModel.pullSpeed(500.0, 10), 0.0);
        assertTrue(RedMatterModel.pullSpeed(15.0, 10) > 0.0);
    }

    @Test
    void thePullIsCappedAndFallsWithDistance() {
        for (int r = 2; r <= 96; r += 7) {
            for (double d = 0.01; d < 400.0; d += 3.7) {
                double v = RedMatterModel.pullSpeed(d, r);
                assertTrue(v >= 0.0 && v <= RedMatterModel.MAX_PULL_SPEED);
            }
        }
        assertTrue(RedMatterModel.pullSpeed(8.0, 20) > RedMatterModel.pullSpeed(30.0, 20));
    }

    @Test
    void theSpeedClampBringsAFastEntityDownAndLeavesZeroAlone() {
        assertEquals(1.0, RedMatterModel.speedClamp(0, 0, 0), 0.0, "ICBM-02-86: no normalizing a zero vector");
        assertEquals(1.0, RedMatterModel.speedClamp(Double.NaN, 0, 0), 0.0);
        double k = RedMatterModel.speedClamp(3.0, 0, 4.0);
        assertEquals(RedMatterModel.MAX_PULL_SPEED, 5.0 * k, 1e-9);
    }

    @Test
    void theKillAndPullRangesNest() {
        for (int r = 2; r <= 96; r++) {
            assertTrue(RedMatterModel.killRadius(r) >= 1.5);
            assertTrue(RedMatterModel.pullRange(r) >= RedMatterModel.killRadius(r) + 8.0);
        }
    }

    @Test
    void theWorkPerTickIsBounded() {
        assertTrue(RedMatterModel.EDITS_PER_SLICE <= RedMatterModel.EDITS_PER_TICK);
        assertTrue(RedMatterModel.CELLS_PER_SLICE <= 8192);
        assertTrue(RedMatterModel.EDITS_PER_TICK <= 512);
        assertTrue(RedMatterModel.ENTITY_CAP <= 256);
        assertTrue(RedMatterModel.IDLE_LINGER_TICKS > 0);
    }

    /** ICBM-02-80: a body saved mid-life comes back with its radius, cursor and mode, not at full size. */
    @Test
    void theStateRoundTripsThroughNbt() {
        NBTTagCompound s = new NBTTagCompound();
        s.setBoolean(RedMatterEffect.INIT, true);
        s.setInteger(RedMatterEffect.RADIUS, 11);
        s.setInteger(RedMatterEffect.INNER, 10);
        s.setInteger(RedMatterEffect.CURSOR, 4321);
        NBTTagCompound copy = (NBTTagCompound) s.copy();
        assertEquals(11, copy.getInteger(RedMatterEffect.RADIUS));
        assertEquals(10, copy.getInteger(RedMatterEffect.INNER));
        assertEquals(4321, copy.getInteger(RedMatterEffect.CURSOR));
        assertFalse(RedMatterEffect.isIdle(copy));
    }

    @Test
    void endingAPassGrowsTheBallThenIdlesAtFullSize() {
        NBTTagCompound s = new NBTTagCompound();
        s.setInteger(RedMatterEffect.RADIUS, 4);
        s.setInteger(RedMatterEffect.CURSOR, 999);
        RedMatterEffect.endPass(s, 4, 5);
        assertEquals(5, s.getInteger(RedMatterEffect.RADIUS));
        assertEquals(4, s.getInteger(RedMatterEffect.INNER));
        assertEquals(0, s.getInteger(RedMatterEffect.CURSOR));
        assertFalse(RedMatterEffect.isIdle(s));
        RedMatterEffect.endPass(s, 5, 5);
        assertTrue(RedMatterEffect.isIdle(s), "full size and the last shell done: nothing left to trace");
        assertEquals(5, s.getInteger(RedMatterEffect.RADIUS));
    }
}
