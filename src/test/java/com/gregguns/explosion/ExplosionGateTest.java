package com.gregguns.explosion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.util.Vec3;

import org.junit.jupiter.api.Test;

/** The M5 explosion exit-gate regressions that need no world. */
class ExplosionGateTest {

    @Test
    void doesNotReproducePFX25_ownerHurtByOwnBlast() {
        String owner = "shooter";
        List<String> inRange = Arrays.asList("shooter", "zombie");
        assertTrue(
            ExplosionRules.victims(inRange, owner, true)
                .contains(owner),
            "selfDamage on keeps the owner in the blast");
        assertFalse(
            ExplosionRules.victims(inRange, owner, false)
                .contains(owner),
            "selfDamage off removes only the owner");
        assertEquals(
            2,
            ExplosionRules.victims(inRange, null, false)
                .size());
    }

    @Test
    void doesNotReproduceCORE19_playerPushPacketCarriesNoBlocks() {
        S27PacketExplosion packet = ProjectileExplosion.pushPacket(1, 2, 3, 4F, Vec3.createVectorHelper(0.5, 0.25, 0));
        assertTrue(
            packet.func_149150_j()
                .isEmpty(),
            "the push packet must not list blocks");
        assertEquals(0.5F, packet.func_149149_c(), 1.0E-6F);
    }

    @Test
    void doesNotReproducePFX14_cancelledBreakEventKeepsBlock() {
        List<int[]> blocks = Arrays.asList(new int[] { 0, 64, 0 }, new int[] { 5, 64, 0 }, new int[] { 9, 64, 0 });
        // A fake protection listener: it cancels every break in x >= 5 (a claimed chunk).
        List<int[]> kept = BlastSlicer.keepAllowed(blocks, p -> p[0] < 5);
        assertEquals(1, kept.size());
        assertEquals(0, kept.get(0)[0]);
        assertEquals(
            3,
            BlastSlicer.keepAllowed(blocks, p -> true)
                .size(),
            "no listener means nothing is vetoed");
    }

    @Test
    void mobGriefingGatesNpcBlastsOnly() {
        assertFalse(ExplosionRules.mayBreakBlocks(true, false, false), "NPC blast, mobGriefing off");
        assertTrue(ExplosionRules.mayBreakBlocks(true, false, true));
        assertTrue(ExplosionRules.mayBreakBlocks(true, true, false), "a player's blast ignores mobGriefing");
        assertFalse(ExplosionRules.mayBreakBlocks(false, true, true), "config off wins");
    }

    @Test
    void auditLineNamesDimensionPositionRadiusShooterWeaponAndBlocks() {
        assertEquals(
            "dim=0 pos=(1.5, 64.0, -2.0) radius=10.0 shooter=Steve weapon=nuke_launcher blocks=4200",
            ExplosionRules.logLine(0, 1.5, 64, -2, 10, "Steve", "nuke_launcher", 4200));
        assertTrue(
            ExplosionRules.logLine(0, 0, 0, 0, 1, null, "x", 1)
                .contains("shooter=none"));
    }

    @Test
    void slicerNeverHandsOutMoreThanTheCapAndGoesNearestFirst() {
        List<int[]> blocks = new ArrayList<>();
        for (int x = -10; x <= 10; x++) {
            for (int y = -10; y <= 10; y++) {
                blocks.add(new int[] { x, y, 0 });
            }
        }
        BlastSlicer.sortNearestFirst(blocks, 0.5, 0.5, 0.5);
        BlastSlicer slicer = new BlastSlicer(blocks, ExplosionRules.BLOCKS_PER_TICK);
        int total = 0;
        int ticks = 0;
        double lastBatchMin = -1;
        while (!slicer.done()) {
            List<int[]> batch = slicer.next();
            assertTrue(batch.size() <= ExplosionRules.BLOCKS_PER_TICK);
            double first = Math.abs(batch.get(0)[0]) + Math.abs(batch.get(0)[1]);
            assertTrue(first >= lastBatchMin - 1, "later batches are farther out");
            lastBatchMin = first;
            total += batch.size();
            ticks++;
        }
        assertEquals(blocks.size(), total, "every block is handed out exactly once");
        assertEquals(1, ticks, "441 blocks fit in one tick");
        assertTrue(
            new BlastSlicer(new ArrayList<int[]>(), 5).next()
                .isEmpty());
    }

    @Test
    void aRadiusTenSphereTakesSeveralTicksUnderTheCap() {
        List<int[]> sphere = new ArrayList<>();
        for (int x = -10; x <= 10; x++) {
            for (int y = -10; y <= 10; y++) {
                for (int z = -10; z <= 10; z++) {
                    if (x * x + y * y + z * z <= 100) {
                        sphere.add(new int[] { x, y, z });
                    }
                }
            }
        }
        BlastSlicer slicer = new BlastSlicer(sphere, ExplosionRules.BLOCKS_PER_TICK);
        int ticks = 0;
        while (!slicer.done()) {
            assertTrue(
                slicer.next()
                    .size() <= ExplosionRules.BLOCKS_PER_TICK);
            ticks++;
        }
        assertTrue(ticks >= 3, "about 4200 blocks need at least three ticks, got " + ticks);
    }

    @Test
    void nukeFxReachesOneHundredFiftyBlocks() {
        assertTrue(ExplosionRules.inNukeFxRange(149.9 * 149.9));
        assertFalse(ExplosionRules.inNukeFxRange(150.1 * 150.1));
    }
}
