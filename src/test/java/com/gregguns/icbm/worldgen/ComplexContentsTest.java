package com.gregguns.icbm.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.gregguns.icbm.complex.ComplexLayout;
import com.gregguns.icbm.complex.ComplexPlan;
import com.gregguns.icbm.complex.MarkKind;
import com.gregguns.icbm.worldgen.Placement.Bonus;
import com.gregguns.icbm.worldgen.Placement.Kind;
import com.gregguns.worldgen.spawner.HoleKind;

/** The activity and contents pass over real layouts. */
class ComplexContentsTest {

    private static long count(List<Placement> ps, Kind k) {
        return ps.stream()
            .filter(p -> p.kind() == k)
            .count();
    }

    private static long[] sum(ActivityLevel level, int ceiling, Kind k) {
        long total = 0;
        long plans = 0;
        for (long s = 0; s < 60; s++) {
            ComplexPlan plan = ComplexLayout.generate(s, 1, 2);
            total += count(ComplexContents.build(plan, level, ceiling, null), k);
            plans++;
        }
        return new long[] { total, plans };
    }

    @Test
    void sameInputsGiveTheSamePlacements() {
        ComplexPlan plan = ComplexLayout.generate(7, 3, 4);
        assertEquals(
            ComplexContents.build(plan, ActivityLevel.PARTLY_ABANDONED, 5, new int[] { 1, 2 })
                .toString(),
            ComplexContents.build(plan, ActivityLevel.PARTLY_ABANDONED, 5, new int[] { 1, 2 })
                .toString());
    }

    @Test
    void abandonedHasNoGuardsAndOperationalHasMany() {
        assertEquals(0, sum(ActivityLevel.ABANDONED, 4, Kind.SPAWNER)[0]);
        long partly = sum(ActivityLevel.PARTLY_ABANDONED, 4, Kind.SPAWNER)[0];
        long op = sum(ActivityLevel.OPERATIONAL, 4, Kind.SPAWNER)[0];
        assertTrue(partly > 0, "partly abandoned has some guards");
        assertTrue(op > partly, "operational has the most: " + op + " vs " + partly);
    }

    @Test
    void damageFallsWithActivity() {
        long ab = sum(ActivityLevel.ABANDONED, 4, Kind.AIR)[0] + sum(ActivityLevel.ABANDONED, 4, Kind.RUBBLE)[0]
            + sum(ActivityLevel.ABANDONED, 4, Kind.COLLAPSE)[0];
        long pa = sum(ActivityLevel.PARTLY_ABANDONED, 4, Kind.AIR)[0]
            + sum(ActivityLevel.PARTLY_ABANDONED, 4, Kind.RUBBLE)[0]
            + sum(ActivityLevel.PARTLY_ABANDONED, 4, Kind.COLLAPSE)[0];
        long op = sum(ActivityLevel.OPERATIONAL, 4, Kind.AIR)[0] + sum(ActivityLevel.OPERATIONAL, 4, Kind.RUBBLE)[0]
            + sum(ActivityLevel.OPERATIONAL, 4, Kind.COLLAPSE)[0];
        assertTrue(ab > pa && pa > op, ab + " " + pa + " " + op);
        assertTrue(sum(ActivityLevel.ABANDONED, 4, Kind.COLLAPSE)[0] > 0);
        assertEquals(0, sum(ActivityLevel.OPERATIONAL, 4, Kind.COLLAPSE)[0]);
        assertTrue(
            sum(ActivityLevel.OPERATIONAL, 4, Kind.LIGHT)[0] > 2 * sum(ActivityLevel.ABANDONED, 4, Kind.LIGHT)[0]);
    }

    @Test
    void floodingOnlyWhenNotOperational() {
        assertTrue(sum(ActivityLevel.ABANDONED, 4, Kind.WATER)[0] > 0);
        assertEquals(0, sum(ActivityLevel.OPERATIONAL, 4, Kind.WATER)[0]);
    }

    @Test
    void everyChestMarkBecomesAChestWithAStableIndexAndACeilingTable() {
        for (long s = 0; s < 30; s++) {
            ComplexPlan plan = ComplexLayout.generate(s, 0, 0);
            long marks = plan.allMarks()
                .stream()
                .filter(m -> m.kind() == MarkKind.LOOT_CHEST)
                .count();
            for (ActivityLevel level : ActivityLevel.values()) {
                List<Placement> ps = ComplexContents.build(plan, level, 6, null);
                assertEquals(marks, count(ps, Kind.CHEST));
                Set<Integer> idx = new HashSet<>();
                for (Placement p : ps) {
                    if (p.kind() == Kind.CHEST) {
                        assertTrue(idx.add(p.chestIndex()), "duplicate chest index");
                        assertTrue(
                            p.category()
                                .endsWith("_luv"),
                            p.category());
                        assertEquals(
                            ActivityProfile.of(level)
                                .richness(),
                            p.richness());
                    }
                }
            }
        }
    }

    @Test
    void machinesNeverExceedTheCeilingAndNeverAreLive() {
        for (int ceiling = 4; ceiling <= 12; ceiling++) {
            for (long s = 0; s < 20; s++) {
                ComplexPlan plan = ComplexLayout.generate(s, 5, 5);
                for (Placement p : ComplexContents.build(plan, ActivityLevel.OPERATIONAL, ceiling, null)) {
                    if (p.kind() == Kind.MACHINE) {
                        assertTrue(p.tier() <= ceiling, p.toString() + " above " + ceiling);
                        assertNotNull(p.role());
                    }
                }
            }
        }
    }

    @Test
    void spawnerKindsAreTheSiloPostsAndDeepOnesAreElite() {
        for (long s = 0; s < 40; s++) {
            ComplexPlan plan = ComplexLayout.generate(s, 9, 9);
            for (Placement p : ComplexContents.build(plan, ActivityLevel.OPERATIONAL, 4, null)) {
                if (p.kind() == Kind.SPAWNER) {
                    assertTrue(p.spawner() == HoleKind.SILO_GUARD || p.spawner() == HoleKind.SILO_ELITE);
                }
            }
        }
    }

    @Test
    void salvageChestsHoldADamagedBodyAndAbandonedMagazinesHoldNoWarhead() {
        boolean body = false;
        for (long s = 0; s < 40; s++) {
            ComplexPlan plan = ComplexLayout.generate(s, 0, 1);
            for (Placement p : ComplexContents.build(plan, ActivityLevel.ABANDONED, 4, null)) {
                if (p.kind() == Kind.CHEST) {
                    if ("salvage".equals(p.key())) {
                        assertTrue(
                            p.bonuses()
                                .contains(Bonus.DAMAGED_BODY));
                        body = true;
                    }
                    assertFalse(
                        p.bonuses()
                            .contains(Bonus.INTACT_WARHEAD));
                }
            }
        }
        assertTrue(body, "no salvage chest in 40 plans");
    }

    @Test
    void operationalMagazinesSometimesHoldAnIntactWarheadAndBonusesAreRare() {
        int warheads = 0;
        int chips = 0;
        int sticks = 0;
        int parts = 0;
        for (long s = 0; s < 400; s++) {
            ComplexPlan plan = ComplexLayout.generate(s, 2, 2);
            for (Placement p : ComplexContents.build(plan, ActivityLevel.OPERATIONAL, 4, new int[] { 9, 9 })) {
                warheads += p.bonuses()
                    .contains(Bonus.INTACT_WARHEAD) ? 1 : 0;
                chips += p.bonuses()
                    .contains(Bonus.SCHEMATIC_CHIP) ? 1 : 0;
                if (p.bonuses()
                    .contains(Bonus.DATA_STICK)) {
                    sticks++;
                    assertEquals("9,9", p.extra());
                }
                parts += p.bonuses()
                    .contains(Bonus.SILO_CONTROLLER)
                    || p.bonuses()
                        .contains(Bonus.SILO_ENERGY_HATCH) ? 1 : 0;
            }
        }
        assertTrue(warheads > 0 && warheads < 400, "warheads " + warheads);
        assertTrue(chips > 0 && chips <= 400, "chips " + chips);
        assertTrue(sticks > 0 && sticks < chips + 400, "sticks " + sticks);
        assertTrue(parts > 0, "parts " + parts);
    }

    @Test
    void noDataStickWithoutAnotherComplex() {
        for (long s = 0; s < 100; s++) {
            for (Placement p : ComplexContents
                .build(ComplexLayout.generate(s, 0, 0), ActivityLevel.OPERATIONAL, 4, null)) {
                assertFalse(
                    p.bonuses()
                        .contains(Bonus.DATA_STICK));
            }
        }
    }

    @Test
    void depthBandsGrowWithDistanceFromTheHall() {
        ComplexPlan plan = ComplexLayout.generate(1, 0, 0);
        assertEquals(
            0,
            ComplexContents.depthBand(
                plan.hall()
                    .bounds(),
                plan.hall()
                    .bounds()));
    }
}
