package com.gregguns.entity.npc.spawn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

import com.gregguns.entity.npc.NpcMob;

class SpawnCoreTest {

    private static final int[] OVERWORLD = { 0 };

    @Test
    void distanceDangerFollowsThresholds() {
        assertEquals(0, SpawnDanger.distance(0, 500, 1000, 2500));
        assertEquals(0, SpawnDanger.distance(499.9, 500, 1000, 2500));
        assertEquals(1, SpawnDanger.distance(500, 500, 1000, 2500));
        assertEquals(2, SpawnDanger.distance(1000, 500, 1000, 2500));
        assertEquals(3, SpawnDanger.distance(2500, 500, 1000, 2500));
    }

    @Test
    void totalIsClamped() {
        assertEquals(5, SpawnDanger.total(3, 2));
        assertEquals(5, SpawnDanger.total(9, 9));
        assertEquals(0, SpawnDanger.total(0, 0));
    }

    @Test
    void doesNotReproduceNPC14_zeroWeightNeverPicked() {
        int[] weights = { 0, 100, 0, 50, 0 };
        Random rnd = new Random(1);
        int[] counts = new int[weights.length];
        for (int i = 0; i < 10_000; i++) {
            counts[WeightedPick.pick(weights, rnd)]++;
        }
        assertEquals(0, counts[0]);
        assertEquals(0, counts[2]);
        assertEquals(0, counts[4]);
        double ratio = counts[1] / (double) counts[3];
        assertTrue(ratio > 1.8 && ratio < 2.2, "ratio " + ratio);
    }

    @Test
    void pickOfNothingIsMinusOne() {
        assertEquals(-1, WeightedPick.pick(new int[0], new Random(1)));
        assertEquals(-1, WeightedPick.pick(new int[] { 0, -5 }, new Random(1)));
    }

    @Test
    void tableOpensByDangerAndSkipsUnbound() {
        SpawnTable all = SpawnTable.build(new SpawnTable.Settings(), m -> true);
        assertEquals(
            2,
            all.open(0)
                .size());
        assertEquals(
            5,
            all.open(1)
                .size());
        assertEquals(
            6,
            all.open(2)
                .size());
        assertTrue(all.allows(NpcMob.BANDIT, 2));
        assertFalse(all.allows(NpcMob.BANDIT, 1));
        SpawnTable some = SpawnTable.build(new SpawnTable.Settings(), m -> m == NpcMob.ZOMBIE_FARMER);
        assertEquals(
            1,
            some.entries()
                .size());
        assertNull(some.entry(NpcMob.BANDIT));
        assertNull(
            SpawnTable.build(new SpawnTable.Settings(), m -> false)
                .pick(5, new Random(1)));
    }

    @Test
    void netherKindsAndDisabledHaveNoOverworldEntry() {
        SpawnTable.Settings s = new SpawnTable.Settings();
        s.zombies = false;
        s.weightBandit = 0;
        SpawnTable t = SpawnTable.build(s, m -> true);
        assertNull(t.entry(NpcMob.ZOMBIE_FARMER));
        assertNull(t.entry(NpcMob.BANDIT));
        assertNull(t.entry(NpcMob.ZOMBIE_PIGMAN_SOLDIER));
        assertNull(t.entry(NpcMob.CYBER_DEMON));
        assertNull(t.entry(NpcMob.ZOMBIE_PIGMAN_SOLDIER, SpawnRealm.NETHER), "the zombies flag covers the Pigman");
        Random rnd = new Random(2);
        for (int i = 0; i < 1000; i++) {
            NpcMob m = t.pick(5, rnd);
            assertTrue(m == NpcMob.SKELETON_SOLDIER || m == NpcMob.PSYCHO_STEVE, "drew " + m);
        }
    }

    @Test
    void gateAppliesRulesInOrder() {
        SpawnTable t = SpawnTable.build(new SpawnTable.Settings(), m -> true);
        SpawnRealm.Dimensions dims = SpawnRealm.Dimensions.defaults();
        SpawnGate.Limits limits = new SpawnGate.Limits(4, 8, 256);
        for (int dim : new int[] { 7, 28, 180 }) {
            assertEquals(
                SpawnGate.Decision.DENY_DIMENSION,
                SpawnGate.decide(t, NpcMob.ZOMBIE_FARMER, SpawnRealm.of(dim, dims), 5, 0, 0, 0, 0, limits));
        }
        SpawnRealm ow = SpawnRealm.OVERWORLD;
        assertEquals(SpawnGate.Decision.ALLOW, SpawnGate.decide(t, NpcMob.ZOMBIE_FARMER, ow, 0, 0, 0, 0, 0, limits));
        assertEquals(SpawnGate.Decision.DENY_DANGER, SpawnGate.decide(t, NpcMob.BANDIT, ow, 1, 0, 0, 0, 0, limits));
        assertEquals(SpawnGate.Decision.DENY_CAP, SpawnGate.decide(t, NpcMob.ZOMBIE_FARMER, ow, 0, 0, 4, 0, 0, limits));
        assertEquals(SpawnGate.Decision.DENY_CAP, SpawnGate.decide(t, NpcMob.ZOMBIE_FARMER, ow, 0, 0, 0, 8, 0, limits));
        assertEquals(SpawnGate.Decision.NOT_MANAGED, SpawnGate.decide(t, NpcMob.NPC_TEST, ow, 0, 0, 0, 0, 0, limits));
        assertEquals(SpawnGate.Decision.NOT_MANAGED, SpawnGate.decide(t, NpcMob.OUTCAST, ow, 5, 0, 0, 0, 0, limits));
        assertEquals(SpawnGate.Decision.DENY_REALM, SpawnGate.decide(t, NpcMob.CYBER_DEMON, ow, 5, 0, 0, 0, 0, limits));
    }
}
