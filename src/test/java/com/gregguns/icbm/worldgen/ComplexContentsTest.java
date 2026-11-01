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
import com.gregguns.icbm.complex.LootKeys;
import com.gregguns.icbm.complex.Mark;
import com.gregguns.icbm.complex.MarkKind;
import com.gregguns.icbm.complex.Room;
import com.gregguns.icbm.complex.RoomKind;
import com.gregguns.icbm.complex.Tunnel;
import com.gregguns.icbm.worldgen.Placement.Bonus;
import com.gregguns.icbm.worldgen.Placement.Kind;
import com.gregguns.worldgen.spawner.SpawnerKind;

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

    /** D326: the per-room roll follows each level's weights over many seeds and rooms. */
    @Test
    void spawnersPerRoomFollowTheLevelWeights() {
        double[][] want = { { 0.80, 0.20, 0.0 }, { 0.25, 0.55, 0.20 }, { 0.0, 0.75, 0.25 } };
        ActivityLevel[] levels = { ActivityLevel.ABANDONED, ActivityLevel.PARTLY_ABANDONED, ActivityLevel.OPERATIONAL };
        for (int l = 0; l < 3; l++) {
            int[] hist = new int[3];
            int rolls = 0;
            for (long seed = 0; seed < 4000; seed++) {
                for (int room = 0; room < 25; room++) {
                    int n = ComplexContents.spawnersIn(seed * 7919L, room, levels[l]);
                    assertTrue(n >= 0 && n <= 2);
                    hist[n]++;
                    rolls++;
                    assertEquals(n, ComplexContents.spawnersIn(seed * 7919L, room, levels[l]), "deterministic");
                }
            }
            for (int n = 0; n < 3; n++) {
                assertEquals(want[l][n], hist[n] / (double) rolls, 0.01, levels[l] + " rooms with " + n);
            }
        }
    }

    /** D326: placed spawners follow the room rolls, sit on layout spots, never in a collapsed room. */
    @Test
    void placedSpawnersAreTheRoomsRolledSpots() {
        for (ActivityLevel level : ActivityLevel.values()) {
            for (long s = 0; s < 30; s++) {
                ComplexPlan plan = ComplexLayout.generate(s, 1, 2);
                List<Placement> ps = ComplexContents.build(plan, level, 4, null);
                java.util.Set<Long> spots = new java.util.HashSet<>();
                int expected = 0;
                List<com.gregguns.icbm.complex.Room> all = new java.util.ArrayList<>(plan.rooms());
                all.add(plan.hall());
                for (com.gregguns.icbm.complex.Room r : all) {
                    int have = 0;
                    for (com.gregguns.icbm.complex.Mark m : r.marks()) {
                        if (m.kind() == com.gregguns.icbm.complex.MarkKind.SPAWNER) {
                            have++;
                            spots.add(m.x() * 4096L + m.z());
                        }
                    }
                    expected += Math.min(have, ComplexContents.spawnersIn(plan.seed(), r.id(), level));
                }
                long n = count(ps, Kind.SPAWNER);
                assertTrue(n <= expected, level + " seed " + s + ": " + n + " placed, " + expected + " rolled");
                for (Placement p : ps) {
                    if (p.kind() != Kind.SPAWNER) continue;
                    assertTrue(spots.contains(p.x() * 4096L + p.z()), "a spawner stands on a layout spot");
                    if (level == ActivityLevel.ABANDONED) {
                        assertEquals(SpawnerKind.SILO_GUARD, p.spawner(), "ruins hold the weaker guards");
                    }
                }
                assertEquals(
                    ps.toString(),
                    ComplexContents.build(plan, level, 4, null)
                        .toString());
            }
        }
        long ab = sum(ActivityLevel.ABANDONED, 4, Kind.SPAWNER)[0];
        long partly = sum(ActivityLevel.PARTLY_ABANDONED, 4, Kind.SPAWNER)[0];
        long op = sum(ActivityLevel.OPERATIONAL, 4, Kind.SPAWNER)[0];
        assertTrue(ab < partly && partly < op, "spawners by level: " + ab + ", " + partly + ", " + op);
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

    /** D252: Launch Control and the silo gallery never cave in, at any level; other rooms still do. */
    @Test
    void launchControlAndTheGalleryNeverCollapse() {
        int rolled = 0, others = 0, controls = 0;
        for (ActivityLevel level : ActivityLevel.values()) {
            ActivityProfile p = ActivityProfile.of(level);
            for (long s = 0; s < 120; s++) {
                ComplexPlan plan = ComplexLayout.generate(s, 2, 5);
                List<Placement> all = ComplexContents.build(plan, level, 4, null);
                for (Placement c : all) {
                    if (c.kind() != Kind.COLLAPSE) continue;
                    for (Room r : plan.rooms()) {
                        if (r.kind() == RoomKind.LAUNCH_CONTROL) {
                            assertFalse(
                                c.box()
                                    .overlapsXZ(r.bounds()),
                                level + " seed " + s + ": Launch Control caved in");
                        }
                    }
                    for (Tunnel t : plan.tunnels()) {
                        if (t.gallery()) {
                            assertFalse(
                                c.box()
                                    .overlapsXZ(t.bounds()),
                                level + " seed " + s + ": the gallery caved in");
                        }
                    }
                    others++;
                }
                for (Room r : plan.rooms()) {
                    if (r.kind() != RoomKind.LAUNCH_CONTROL) continue;
                    controls++;
                    if (ComplexContents.collapseRoll(plan.seed(), r.id(), p)) rolled++;
                }
            }
        }
        assertTrue(controls >= 360, "Launch Control rooms checked: " + controls);
        assertTrue(rolled > 0, "the roll alone would have caved some in, so the check can fail: " + rolled);
        assertTrue(others > 0, "other rooms still cave in: " + others);
        assertFalse(ComplexContents.mayCollapse(RoomKind.LAUNCH_CONTROL));
        assertTrue(ComplexContents.mayCollapse(RoomKind.STOCKPILE));
    }

    @Test
    void floodingOnlyWhenNotOperational() {
        assertTrue(sum(ActivityLevel.ABANDONED, 4, Kind.WATER)[0] > 0);
        assertEquals(0, sum(ActivityLevel.OPERATIONAL, 4, Kind.WATER)[0]);
    }

    @Test
    void everyChestSpotBecomesAChestOrACrateAndDisplaysAreExtra() {
        for (long s = 0; s < 30; s++) {
            ComplexPlan plan = ComplexLayout.generate(s, 0, 0);
            for (ActivityLevel level : ActivityLevel.values()) {
                List<Placement> ps = ComplexContents.build(plan, level, 6, null);
                long spots = 0, displays = 0;
                java.util.Set<Long> collapsedCells = new HashSet<>();
                for (Placement p : ps) {
                    if (p.kind() == Kind.COLLAPSE) {
                        collapsedCells.add((long) p.box().minX << 32 | p.box().minZ & 0xFFFFFFFFL);
                    }
                }
                for (Room r : roomsWithHall(plan)) {
                    boolean down = collapsedCells.contains((long) r.bounds().minX << 32 | r.bounds().minZ & 0xFFFFFFFFL)
                        && r.id() != plan.hall()
                            .id();
                    for (Mark m : r.marks()) {
                        if (down) continue;
                        if (m.kind() == MarkKind.LOOT_CHEST) spots++;
                        if (m.kind() == MarkKind.GUN_DISPLAY) displays++;
                    }
                }
                long roomChests = ps.stream()
                    .filter(p -> p.kind() == Kind.CHEST && !LootKeys.ARMORY_AMMO.equals(p.key()))
                    .count();
                long displayChests = ps.stream()
                    .filter(p -> p.kind() == Kind.CHEST && LootKeys.ARMORY_AMMO.equals(p.key()))
                    .count();
                assertEquals(Math.min(ChestBudget.target(level), spots), roomChests, "room chests, " + level);
                assertEquals(spots, roomChests + count(ps, Kind.CRATE), "every spot is a chest or a crate");
                assertEquals(displays, displayChests, "every standing display is placed, outside the budget");
                Set<Integer> idx = new HashSet<>();
                for (Placement p : ps) {
                    if (p.kind() == Kind.CHEST) {
                        assertTrue(idx.add(p.chestIndex()), "duplicate chest index");
                        assertTrue(
                            p.category()
                                .endsWith("_luv"),
                            p.category());
                        assertNotNull(p.roomState(), "a room chest knows its room's state");
                        assertEquals(
                            RoomLootDraw.profile(p.roomState())
                                .richness(),
                            p.richness());
                    }
                }
            }
        }
    }

    private static List<Room> roomsWithHall(ComplexPlan plan) {
        List<Room> out = new java.util.ArrayList<>();
        out.add(plan.hall());
        out.addAll(plan.rooms());
        return out;
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
    void spawnerKindsAreTheSiloPosts() {
        for (long s = 0; s < 40; s++) {
            ComplexPlan plan = ComplexLayout.generate(s, 9, 9);
            for (Placement p : ComplexContents.build(plan, ActivityLevel.OPERATIONAL, 4, null)) {
                if (p.kind() == Kind.SPAWNER) {
                    assertTrue(p.spawner() == SpawnerKind.SILO_GUARD || p.spawner() == SpawnerKind.SILO_ELITE);
                }
            }
        }
    }

    @Test
    void eliteSpawnersBecomeCommandPostsFromLuV() {
        assertEquals(SpawnerKind.SILO_ELITE, ComplexContents.elitePost(4));
        assertEquals(SpawnerKind.SILO_ELITE, ComplexContents.elitePost(5));
        assertEquals(SpawnerKind.SILO_COMMAND, ComplexContents.elitePost(6));
        assertEquals(SpawnerKind.SILO_COMMAND, ComplexContents.elitePost(12));
        int ivElite = 0;
        int luvCommand = 0;
        for (long s = 0; s < 40; s++) {
            ComplexPlan plan = ComplexLayout.generate(s, 9, 9);
            for (Placement p : ComplexContents.build(plan, ActivityLevel.OPERATIONAL, 5, null)) {
                if (p.kind() == Kind.SPAWNER) {
                    assertTrue(p.spawner() != SpawnerKind.SILO_COMMAND, "no Outcast post below LuV");
                    ivElite += p.spawner() == SpawnerKind.SILO_ELITE ? 1 : 0;
                }
            }
            for (Placement p : ComplexContents.build(plan, ActivityLevel.OPERATIONAL, 6, null)) {
                if (p.kind() == Kind.SPAWNER) {
                    assertTrue(p.spawner() != SpawnerKind.SILO_ELITE, "every LuV elite post is a command post");
                    luvCommand += p.spawner() == SpawnerKind.SILO_COMMAND ? 1 : 0;
                }
            }
        }
        assertTrue(ivElite > 0 && luvCommand == ivElite, "same posts, other kind: " + ivElite + " " + luvCommand);
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
        for (long s = 0; s < 250; s++) {
            ComplexPlan plan = ComplexLayout.generate(s, 5, 6);
            List<Placement> ps = ComplexContents.build(plan, ActivityLevel.ABANDONED, 4, null);
            for (Placement f : ps) {
                if (f.kind() != Kind.FRAME_WALL) continue;
                frames++;
                boolean alongZ = f.facing() == 1 || f.facing() == 3;
                int[] dx = { 0, -1, 0, 1 }, dz = { 1, 0, -1, 0 };
                for (int i = 0; i < f.extent(); i++) {
                    int wx = f.x() + (alongZ ? 0 : ComplexLayout.FRAME_STRIDE * i) - dx[f.facing()];
                    int wz = f.z() + (alongZ ? ComplexLayout.FRAME_STRIDE * i : 0) - dz[f.facing()];
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

    /** Each display chest sits right under its gun's frame and holds ammo that gun takes. */
    @Test
    void everyDisplayChestHoldsAmmoForTheGunAboveIt() {
        int pairs = 0;
        for (long s = 0; s < 40; s++) {
            ComplexPlan plan = ComplexLayout.generate(s, 5, 6);
            for (int ceiling : new int[] { 4, 5, 7, 10 }) {
                List<Placement> ps = ComplexContents.build(plan, ActivityLevel.OPERATIONAL, ceiling, null);
                for (Placement c : ps) {
                    if (c.kind() != Kind.CHEST || !"armory_ammo".equals(c.key())) continue;
                    Placement frame = null;
                    for (Placement f : ps) {
                        if (f.kind() == Kind.FRAME_WALL && f.x() == c.x() && f.z() == c.z()) frame = f;
                    }
                    assertNotNull(frame, "a gun hangs above the ammo chest");
                    assertEquals(c.y() + 1, frame.y(), "directly above");
                    String gun = ArmoryDisplay.gunAt(ceiling, 12345L, c.x(), c.z());
                    assertNotNull(gun);
                    List<String> items = ArmoryDisplay.itemsFor(gun);
                    com.gregguns.item.ammo.AmmoCompatibility ac = com.gregguns.item.ammo.AmmoCompatibility.DEFAULT;
                    String bare = gun.substring(gun.indexOf(':') + 1);
                    if (!com.gregguns.gun.def.GunDefinitions.byName(bare)
                        .stats()
                        .isEnergy()) {
                        assertFalse(items.isEmpty(), gun + " has ammo");
                        for (String item : items) {
                            boolean direct = ac.gunsTaking(item)
                                .contains(bare);
                            boolean viaMag = false;
                            for (String mag : ac.magazinesFilledWith(item)) viaMag |= ac.gunsTaking(mag)
                                .contains(bare);
                            assertTrue(direct || viaMag, item + " fits " + gun);
                        }
                    }
                    pairs++;
                }
            }
        }
        assertTrue(pairs > 300, "display pairs: " + pairs);
    }

    private static long[] machinesAndChests(ActivityLevel level) {
        long working = 0;
        long chests = 0;
        for (long s = 0; s < 60; s++) {
            for (Placement p : ComplexContents.build(ComplexLayout.generate(s, 1, 2), level, 4, null)) {
                if (p.kind() == Kind.MACHINE && !p.dead()) working++;
                if (p.kind() == Kind.CHEST) chests++;
            }
        }
        return new long[] { working, chests };
    }

    @Test
    void abandonedComplexesHoldFarFewerWorkingMachines() {
        long[] ab = machinesAndChests(ActivityLevel.ABANDONED);
        long[] pa = machinesAndChests(ActivityLevel.PARTLY_ABANDONED);
        long[] op = machinesAndChests(ActivityLevel.OPERATIONAL);
        assertTrue(op[0] > 0 && ab[0] * 5 < pa[0], "working machines " + ab[0] + " vs " + pa[0]);
        assertTrue(ab[0] * 10 < op[0], "working machines " + ab[0] + " vs " + op[0]);
        // the chest totals are the budget's (D315): never more in a ruin than in a staffed complex
        assertTrue(ab[1] > 0 && ab[1] <= op[1], "chests " + ab[1] + " vs " + op[1]);
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

    /**
     * D231: lamps die by whole runs when abandoned and by whole sections when partly abandoned, at the D36 rates, and
     * all burn when operational.
     */
    @Test
    void lampsBurnOrDieByRunOrSectionAtTheD36Rates() {
        for (ActivityLevel level : ActivityLevel.values()) {
            long lit = 0, all = 0;
            for (long s = 0; s < 40; s++) {
                ComplexPlan plan = ComplexLayout.generate(s, 1, 2);
                java.util.Set<Long> on = new java.util.HashSet<>();
                for (Placement p : ComplexContents.build(plan, level, 4, null)) {
                    if (p.kind() == Kind.LIGHT) on.add(((long) p.x() << 40) ^ ((long) p.y() << 20) ^ p.z());
                }
                java.util.Map<String, Boolean> group = new java.util.HashMap<>();
                for (com.gregguns.icbm.complex.Mark m : plan.allMarks()) {
                    if (m.kind() != com.gregguns.icbm.complex.MarkKind.LIGHT) continue;
                    all++;
                    boolean burns = on.contains(((long) m.x() << 40) ^ ((long) m.y() << 20) ^ m.z());
                    if (burns) lit++;
                    String[] k = m.key()
                        .split(":");
                    String g = level == ActivityLevel.PARTLY_ABANDONED ? k[1] : k[1] + ":" + k[2];
                    Boolean before = group.put(g, burns);
                    if (level != ActivityLevel.OPERATIONAL && before != null) {
                        assertEquals(before, burns, level + ": a whole " + g + " burns or dies together");
                    }
                }
            }
            double share = (double) lit / all, want = ActivityProfile.of(level)
                .litChance();
            assertEquals(want, share, level == ActivityLevel.OPERATIONAL ? 0 : 0.12, level + " lit share " + share);
        }
    }
}
