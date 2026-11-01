package com.gregguns.entity.npc.spawn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.gregguns.entity.npc.NpcMob;

/** The Nether and End spawn tables, their danger rules and their gate rules (D353, GGS-10-41 to GGS-10-43). */
class SpawnRealmTest {

    private static final SpawnGate.Limits LIMITS = new SpawnGate.Limits(4, 8, 256);
    private final SpawnTable table = SpawnTable.build(new SpawnTable.Settings(), m -> true);

    @Test
    void dimensionsMapToRealmsAndUnlistedOnesToNone() {
        SpawnRealm.Dimensions d = SpawnRealm.Dimensions.defaults();
        assertSame(SpawnRealm.OVERWORLD, SpawnRealm.of(0, d));
        assertSame(SpawnRealm.NETHER, SpawnRealm.of(-1, d));
        assertSame(SpawnRealm.END, SpawnRealm.of(1, d));
        for (int dim : new int[] { 7, 28, 180, -100 }) {
            assertNull(SpawnRealm.of(dim, d), "dimension " + dim);
        }
        SpawnRealm.Dimensions both = new SpawnRealm.Dimensions(new int[] { 0, 5 }, new int[] { 5, -1 }, null);
        assertSame(SpawnRealm.OVERWORLD, SpawnRealm.of(5, both), "the Overworld list wins a shared id");
        assertNull(SpawnRealm.of(1, both), "an empty End list switches the End table off");
    }

    @Test
    void netherDangerScalesTheDistanceAndStartsAtOne() {
        SpawnRealm n = SpawnRealm.NETHER;
        assertEquals(1, n.danger(0, 2, 500, 1000, 2500), "the biome part is ignored: the Nether is a fixed 1");
        assertEquals(1, n.danger(62, 0, 500, 1000, 2500));
        assertEquals(2, n.danger(63, 0, 500, 1000, 2500), "63 Nether blocks lead 504 Overworld blocks out");
        assertEquals(3, n.danger(125, 0, 500, 1000, 2500));
        assertEquals(4, n.danger(313, 0, 500, 1000, 2500));
        assertEquals(4, n.danger(100_000, 0, 500, 1000, 2500), "never above distance 3 plus 1");
    }

    @Test
    void endDangerIsHarsh() {
        assertEquals(2, SpawnRealm.END.danger(0, 0, 500, 1000, 2500));
        assertEquals(3, SpawnRealm.END.danger(600, 0, 500, 1000, 2500));
        assertEquals(5, SpawnRealm.END.danger(3000, 0, 500, 1000, 2500));
        assertEquals(4, SpawnRealm.OVERWORLD.danger(1200, 2, 500, 1000, 2500), "the Overworld rule is unchanged");
    }

    @Test
    void netherTableTiersTheMutantsByDanger() {
        assertNotNull(table.entry(NpcMob.ZOMBIE_PIGMAN_SOLDIER, SpawnRealm.NETHER));
        assertTrue(table.allows(NpcMob.ZOMBIE_PIGMAN_SOLDIER, SpawnRealm.NETHER, 1));
        assertFalse(table.allows(NpcMob.SUPER_MUTANT_BASIC, SpawnRealm.NETHER, 1));
        assertTrue(table.allows(NpcMob.SUPER_MUTANT_BASIC, SpawnRealm.NETHER, 2));
        assertFalse(table.allows(NpcMob.SUPER_MUTANT_HEAVY, SpawnRealm.NETHER, 2));
        assertTrue(table.allows(NpcMob.SUPER_MUTANT_HEAVY, SpawnRealm.NETHER, 3));
        assertFalse(table.allows(NpcMob.SUPER_MUTANT_ELITE, SpawnRealm.NETHER, 3));
        assertFalse(table.allows(NpcMob.CYBER_DEMON, SpawnRealm.NETHER, 3));
        assertTrue(table.allows(NpcMob.SUPER_MUTANT_ELITE, SpawnRealm.NETHER, 4));
        assertTrue(table.allows(NpcMob.CYBER_DEMON, SpawnRealm.NETHER, 4));
        assertTrue(
            table.entry(NpcMob.CYBER_DEMON, SpawnRealm.NETHER)
                .boss());
        assertEquals(
            1,
            table.entry(NpcMob.CYBER_DEMON, SpawnRealm.NETHER)
                .groupMax(),
            "the Cyberdemon comes alone");
        assertNull(table.entry(NpcMob.BANDIT, SpawnRealm.NETHER));
        assertNull(table.entry(NpcMob.COMMANDO, SpawnRealm.NETHER));
    }

    @Test
    void endTableIsCommandoSquadsOnly() {
        assertEquals(
            1,
            table.entries(SpawnRealm.END)
                .size());
        SpawnTable.Entry e = table.entry(NpcMob.COMMANDO, SpawnRealm.END);
        assertNotNull(e);
        assertEquals(2, e.groupMin());
        assertEquals(SpawnTable.END_SQUAD_MAX, e.groupMax());
        assertNull(table.entry(NpcMob.COMMANDO, SpawnRealm.OVERWORLD), "Commandos never spawn naturally elsewhere");
    }

    @Test
    void drawsStayRareAndInTheirRealm() {
        Map<NpcMob, Integer> counts = new EnumMap<>(NpcMob.class);
        Random rnd = new Random(7);
        for (int i = 0; i < 10_000; i++) {
            counts.merge(table.pick(SpawnRealm.NETHER, 4, rnd), 1, Integer::sum);
        }
        for (NpcMob m : counts.keySet()) {
            assertTrue(SpawnGate.hasRow(m, SpawnRealm.NETHER), "drew " + m + " in the Nether");
        }
        // Weights 15, 6, 3, 2, 1: the Cyberdemon is about one draw in 27 of GregGuns' own Nether rows.
        int demon = counts.getOrDefault(NpcMob.CYBER_DEMON, 0);
        assertTrue(demon > 250 && demon < 500, "Cyberdemon draws " + demon);
        assertTrue(counts.get(NpcMob.ZOMBIE_PIGMAN_SOLDIER) > counts.get(NpcMob.SUPER_MUTANT_BASIC));
        assertEquals(NpcMob.COMMANDO, table.pick(SpawnRealm.END, 5, rnd));
    }

    @Test
    void flagsAndWeightsSwitchRowsOff() {
        SpawnTable.Settings s = new SpawnTable.Settings();
        s.mutants = false;
        s.cyberdemon = false;
        s.military = false;
        SpawnTable t = SpawnTable.build(s, m -> true);
        assertNull(t.entry(NpcMob.SUPER_MUTANT_BASIC, SpawnRealm.NETHER));
        assertNull(t.entry(NpcMob.CYBER_DEMON, SpawnRealm.NETHER));
        assertNull(t.entry(NpcMob.COMMANDO, SpawnRealm.END));
        assertNull(t.entry(NpcMob.SKELETON_SOLDIER), "military covers the Skeleton Soldier too");
        assertNotNull(t.entry(NpcMob.ZOMBIE_PIGMAN_SOLDIER, SpawnRealm.NETHER));
        SpawnTable.Settings w = new SpawnTable.Settings();
        w.weightNetherCyberdemon = 0;
        w.weightEndCommando = 0;
        SpawnTable tw = SpawnTable.build(w, m -> true);
        assertNull(tw.entry(NpcMob.CYBER_DEMON, SpawnRealm.NETHER));
        assertNull(tw.entry(NpcMob.COMMANDO, SpawnRealm.END));
        assertNull(
            SpawnTable.build(new SpawnTable.Settings(), m -> m != NpcMob.COMMANDO)
                .entry(NpcMob.COMMANDO, SpawnRealm.END),
            "an unbound mob has no row");
    }

    @Test
    void gateKeepsTheEndCenterClearAndBossesApart() {
        SpawnRealm end = SpawnRealm.END;
        assertEquals(
            SpawnGate.Decision.DENY_DISTANCE,
            SpawnGate.decide(table, NpcMob.COMMANDO, end, 2, 255.9, 0, 0, 0, LIMITS));
        assertEquals(SpawnGate.Decision.ALLOW, SpawnGate.decide(table, NpcMob.COMMANDO, end, 2, 256, 0, 0, 0, LIMITS));
        assertEquals(
            SpawnGate.Decision.DENY_CAP,
            SpawnGate.decide(table, NpcMob.COMMANDO, end, 2, 800, 4, 0, 0, LIMITS));
        SpawnRealm nether = SpawnRealm.NETHER;
        assertEquals(
            SpawnGate.Decision.DENY_DANGER,
            SpawnGate.decide(table, NpcMob.CYBER_DEMON, nether, 3, 0, 0, 0, 0, LIMITS));
        assertEquals(
            SpawnGate.Decision.ALLOW,
            SpawnGate.decide(table, NpcMob.CYBER_DEMON, nether, 4, 0, 0, 0, 0, LIMITS));
        assertEquals(
            SpawnGate.Decision.DENY_BOSS,
            SpawnGate.decide(table, NpcMob.CYBER_DEMON, nether, 4, 0, 0, 0, 1, LIMITS));
        assertEquals(
            SpawnGate.Decision.ALLOW,
            SpawnGate.decide(table, NpcMob.SUPER_MUTANT_ELITE, nether, 4, 0, 0, 0, 3, LIMITS),
            "only bosses read the boss count");
        assertEquals(
            SpawnGate.Decision.ALLOW,
            SpawnGate.decide(table, NpcMob.ZOMBIE_PIGMAN_SOLDIER, nether, 1, 0, 0, 0, 0, LIMITS),
            "the End keep-out is the End's alone");
        assertEquals(
            SpawnGate.Decision.DENY_REALM,
            SpawnGate.decide(table, NpcMob.COMMANDO, nether, 5, 1000, 0, 0, 0, LIMITS));
        SpawnTable off = SpawnTable.build(new SpawnTable.Settings(), m -> m != NpcMob.CYBER_DEMON);
        assertEquals(
            SpawnGate.Decision.DENY_DISABLED,
            SpawnGate.decide(off, NpcMob.CYBER_DEMON, nether, 4, 0, 0, 0, 0, LIMITS));
    }

    @Test
    void naturalKindsAreTheTableRowsAndNothingElse() {
        for (NpcMob m : NpcMob.values()) {
            boolean row = false;
            for (SpawnRealm r : SpawnRealm.values()) {
                row |= SpawnGate.hasRow(m, r);
                if (SpawnGate.hasRow(m, r)) {
                    assertNotNull(table.entry(m, r), m + " has a gate row but no default table row in " + r);
                }
            }
            assertEquals(row, SpawnGate.isNatural(m), m.name());
        }
        for (SpawnTable.Entry e : table.entries()) {
            assertTrue(SpawnGate.hasRow(e.mob(), e.realm()), e.mob() + " in " + e.realm());
        }
        for (NpcMob m : new NpcMob[] { NpcMob.DICTATOR_DAVE, NpcMob.OUTCAST, NpcMob.STORMTROOPER, NpcMob.ARMY_SOLDIER,
            NpcMob.ALIEN_BUG, NpcMob.ATTACK_HELICOPTER }) {
            assertFalse(SpawnGate.isNatural(m), m + " comes from structures or eggs, not natural spawning");
        }
    }
}
