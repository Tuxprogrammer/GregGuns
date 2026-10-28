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
    void salvageChestsHoldADamagedBody() {
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
                }
            }
        }
        assertTrue(body, "no salvage chest in 40 plans");
    }

    @Test
    void bonusesAreRare() {
        int chips = 0;
        int sticks = 0;
        int parts = 0;
        for (long s = 0; s < 400; s++) {
            ComplexPlan plan = ComplexLayout.generate(s, 2, 2);
            for (Placement p : ComplexContents.build(plan, ActivityLevel.OPERATIONAL, 4, new int[] { 9, 9 })) {
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
        assertTrue(chips > 0 && chips <= 400, "chips " + chips);
        assertTrue(sticks > 0 && sticks < chips + 400, "sticks " + sticks);
        assertTrue(parts > 0, "parts " + parts);
    }

    @Test
    void magazineChestsHoldCasingPlatesAtEveryLevel() {
        for (ActivityLevel level : ActivityLevel.values()) {
            boolean magazine = false;
            for (long s = 0; s < 40; s++) {
                for (Placement p : ComplexContents.build(ComplexLayout.generate(s, 1, 1), level, 4, null)) {
                    if (p.kind() == Kind.CHEST && "magazine".equals(p.key())) {
                        magazine = true;
                        assertTrue(
                            p.bonuses()
                                .contains(Bonus.CASING_PLATES),
                            level + " magazine chest holds plates");
                    }
                }
            }
            assertTrue(magazine, level + ": no magazine chest in 40 plans");
        }
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

    @Test
    void theDamagePassNeverBreaksTheWallBehindAnArmoryFrame() {
        int frames = 0;
        for (long s = 0; s < 60; s++) {
            ComplexPlan plan = ComplexLayout.generate(s, 5, 6);
            List<Placement> ps = ComplexContents.build(plan, ActivityLevel.ABANDONED, 4, null);
            for (Placement f : ps) {
                if (f.kind() != Kind.FRAME_WALL) continue;
                frames++;
                boolean alongZ = f.facing() == 1 || f.facing() == 3;
                int[] dx = { 0, -1, 0, 1 }, dz = { 1, 0, -1, 0 };
                for (int i = 0; i < f.extent(); i++) {
                    int wx = f.x() + (alongZ ? 0 : 2 * i) - dx[f.facing()];
                    int wz = f.z() + (alongZ ? 2 * i : 0) - dz[f.facing()];
                    for (Placement d : ps) {
                        boolean damage = d.kind() == Kind.AIR || d.kind() == Kind.RUBBLE;
                        assertFalse(
                            damage && d.x() == wx && d.y() == f.y() && d.z() == wz,
                            "wall behind a frame broken");
                    }
                }
            }
        }
        assertTrue(frames > 50, "armories exist: " + frames);
    }

    @Test
    void guardChancesGrowByAtMostHalfFromEvToUmv() {
        assertEquals(1.0, ComplexContents.guardScale(SiloCeiling.EV));
        assertEquals(1.5, ComplexContents.guardScale(SiloCeiling.UMV), 1e-9);
        assertEquals(1.0, ComplexContents.guardScale(0));
        assertEquals(1.5, ComplexContents.guardScale(99), 1e-9);
        for (int c = SiloCeiling.EV; c < SiloCeiling.UMV; c++) {
            assertTrue(ComplexContents.guardScale(c) < ComplexContents.guardScale(c + 1));
        }
    }

    @Test
    void partlyAbandonedComplexesHaveMoreGuardsAtTheTop() {
        int low = 0;
        int high = 0;
        for (long s = 0; s < 60; s++) {
            ComplexPlan plan = ComplexLayout.generate(s, 2, 2);
            low += spawners(ComplexContents.build(plan, ActivityLevel.PARTLY_ABANDONED, SiloCeiling.EV, null));
            high += spawners(ComplexContents.build(plan, ActivityLevel.PARTLY_ABANDONED, SiloCeiling.UMV, null));
        }
        assertTrue(high > low, "guards " + low + " at EV, " + high + " at UMV");
        assertTrue(high <= low * 1.5 + 1, "no more than half again: " + low + " vs " + high);
    }

    private static int spawners(java.util.List<Placement> placements) {
        int n = 0;
        for (Placement p : placements) {
            n += p.kind() == Kind.SPAWNER ? 1 : 0;
        }
        return n;
    }

    @Test
    void salvagedBodyFollowsTheRocketTierCappedAtThree() {
        assertEquals(1, ComplexContents.bodyTier(SiloCeiling.EV));
        assertEquals(2, ComplexContents.bodyTier(SiloCeiling.EV + 1));
        assertEquals(3, ComplexContents.bodyTier(SiloCeiling.EV + 2));
        assertEquals(3, ComplexContents.bodyTier(SiloCeiling.UMV));
    }

    @Test
    void everyChestKnowsItsCeilingAndFuelDepotsGetCells() {
        boolean fuel = false;
        for (long s = 0; s < 40; s++) {
            for (Placement p : ComplexContents
                .build(ComplexLayout.generate(s, 3, 3), ActivityLevel.OPERATIONAL, SiloCeiling.EV + 2, null)) {
                if (p.kind() == Kind.CHEST) {
                    assertEquals(SiloCeiling.EV + 2, p.ceiling());
                    if ("fuel".equals(p.key())) {
                        fuel = true;
                        assertTrue(
                            p.bonuses()
                                .contains(Bonus.FUEL_CELLS));
                    }
                }
            }
        }
        assertTrue(fuel, "no fuel depot in 40 plans");
    }
}
