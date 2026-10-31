package com.gregguns.icbm.complex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.gregguns.icbm.worldgen.ActivityLevel;
import com.gregguns.icbm.worldgen.ChestBudget;
import com.gregguns.icbm.worldgen.RoomLoot;
import com.gregguns.icbm.worldgen.RoomState;

/**
 * The chest budget (D315, GGI-08-15..17): exactly 100, 200 or 300 chests per complex, at least one per standing room,
 * none in a caved-in room, the safe always, spread through each room. Synthetic plans stand in for the blueprint rooms
 * (about 550 to 650 chest spots, as the blueprints plan), and today's layouts check the short case.
 */
class ChestBudgetTest {

    /** Spots per kind at the blueprints' median size (F-loot-tables.md, "Slots" and "Needs"). */
    private static final Map<RoomKind, Integer> SPOTS = new EnumMap<>(RoomKind.class);

    static {
        SPOTS.put(RoomKind.LAUNCH_CONTROL, 12);
        SPOTS.put(RoomKind.RADAR_TRACKING, 12);
        SPOTS.put(RoomKind.COMMS, 15);
        SPOTS.put(RoomKind.SERVER_ROOM, 10);
        SPOTS.put(RoomKind.COMMANDER_OFFICE, 6);
        SPOTS.put(RoomKind.WARHEAD_MAGAZINE, 22);
        SPOTS.put(RoomKind.MISSILE_ASSEMBLY, 24);
        SPOTS.put(RoomKind.FUEL_DEPOT, 16);
        SPOTS.put(RoomKind.GENERATOR_HALL, 8);
        SPOTS.put(RoomKind.AIR_HANDLING, 12);
        SPOTS.put(RoomKind.PUMP_ROOM, 10);
        SPOTS.put(RoomKind.WATER_TREATMENT, 10);
        SPOTS.put(RoomKind.WORKSHOP, 15);
        SPOTS.put(RoomKind.VEHICLE_BAY, 19);
        SPOTS.put(RoomKind.STOCKPILE, 34);
        SPOTS.put(RoomKind.MESS_GALLEY, 13);
        SPOTS.put(RoomKind.CAFETERIA, 16);
        SPOTS.put(RoomKind.BUNKS, 34);
        SPOTS.put(RoomKind.INFIRMARY, 23);
        SPOTS.put(RoomKind.DECON_AIRLOCK, 4);
        SPOTS.put(RoomKind.GUARD_POST, 3);
        SPOTS.put(RoomKind.ARMORY, 27);
    }

    /** A synthetic complex: the hub plus about 30 rooms dealt from the kinds above, some kinds twice. */
    static ComplexPlan synth(long seed) {
        Random r = new Random(seed);
        Room hall = room(0, RoomKind.ASSEMBLY_BAY, 51, 40, 9 + r.nextInt(6), false);
        List<Room> rooms = new ArrayList<>();
        List<RoomKind> deck = new ArrayList<>(SPOTS.keySet());
        Collections.shuffle(deck, r);
        int id = 1;
        for (RoomKind k : deck) {
            rooms.add(scaled(id++, k, r));
        }
        for (int i = 0; i < 4 + r.nextInt(6); i++) {
            rooms.add(scaled(id++, deck.get(r.nextInt(deck.size())), r));
        }
        rooms.add(room(id++, RoomKind.GUARD_POST, 10, 10, 3, false));
        return new ComplexPlan(
            seed,
            0,
            0,
            hall,
            rooms,
            new ArrayList<Tunnel>(),
            new ArrayList<SiloSlot>(),
            new ArrayList<Door>(),
            new int[] { 0, 0, 2, 0 },
            new ArrayList<int[]>());
    }

    private static Room scaled(int id, RoomKind k, Random r) {
        double f = 0.85 + r.nextDouble() * 0.6;
        int spots = Math.max(1, (int) Math.round(SPOTS.get(k) * f));
        return room(id, k, (int) (k.length * f), (int) (k.depth * f), spots, k == RoomKind.COMMANDER_OFFICE);
    }

    private static Room room(int id, RoomKind k, int lx, int lz, int spots, boolean safe) {
        int x0 = id * 100;
        Room room = new Room(id, k, new Box(x0, 0, 0, x0 + lx, 6, lz), id == 0 ? -1 : 0);
        for (int i = 0; i < spots; i++) {
            room.add(
                MarkKind.LOOT_CHEST,
                x0 + 1 + i % (lx - 2),
                1,
                1 + i / (lx - 2),
                safe && i == 0 ? LootKeys.SAFE : "x");
        }
        room.add(MarkKind.LIGHT, x0 + 2, 5, 2, "lamp:0:0");
        return room;
    }

    private static Set<Integer> collapsed(ComplexPlan plan, ActivityLevel level, Random r) {
        double chance = level == ActivityLevel.ABANDONED ? 0.3 : level == ActivityLevel.PARTLY_ABANDONED ? 0.1 : 0;
        Set<Integer> out = new HashSet<>();
        for (Room room : plan.rooms()) {
            if (room.kind() != RoomKind.LAUNCH_CONTROL && r.nextDouble() < chance) out.add(room.id());
        }
        return out;
    }

    private static Map<Integer, RoomState> states(ComplexPlan plan, ActivityLevel level, Random r) {
        Map<Integer, RoomState> out = new HashMap<>();
        out.put(
            plan.hall()
                .id(),
            state(level, r));
        for (Room room : plan.rooms()) out.put(room.id(), state(level, r));
        return out;
    }

    private static RoomState state(ActivityLevel level, Random r) {
        if (level == ActivityLevel.OPERATIONAL) return RoomState.STAFFED;
        if (level == ActivityLevel.ABANDONED) return RoomState.GUTTED;
        return r.nextDouble() < 0.5 ? RoomState.IN_SERVICE : RoomState.MOTHBALLED;
    }

    private static int target(ActivityLevel level) {
        return ChestBudget.DEFAULT_TARGETS[level.ordinal()];
    }

    @Test
    void exactlyTheTargetOnEverySeedAndLevel() {
        for (long seed = 0; seed < 1000; seed++) {
            ComplexPlan plan = synth(seed);
            for (ActivityLevel level : ActivityLevel.values()) {
                Random r = new Random(seed * 31 + level.ordinal());
                Set<Integer> down = collapsed(plan, level, r);
                Map<Integer, RoomState> st = states(plan, level, r);
                ChestBudget.Allocation a = ChestBudget.allocate(plan, level, st, down, target(level));
                assertEquals(
                    target(level),
                    a.chosen()
                        .size(),
                    "seed " + seed + " " + level + " of " + a.eligible());
                assertEquals(0, a.shortfall());
                int sum = 0;
                for (int q : a.quotas()
                    .values()) sum += q;
                assertEquals(target(level), sum);
            }
        }
    }

    @Test
    void everyStandingRoomGetsOneAndCaveInsNone() {
        for (long seed = 0; seed < 300; seed++) {
            ComplexPlan plan = synth(seed);
            for (ActivityLevel level : ActivityLevel.values()) {
                Random r = new Random(seed * 31 + level.ordinal());
                Set<Integer> down = collapsed(plan, level, r);
                ChestBudget.Allocation a = ChestBudget
                    .allocate(plan, level, states(plan, level, r), down, target(level));
                List<Room> all = new ArrayList<>(plan.rooms());
                all.add(plan.hall());
                for (Room room : all) {
                    int chosen = 0;
                    for (Mark m : room.marks()) if (a.chosen()
                        .contains(m)) chosen++;
                    if (down.contains(room.id())) {
                        assertEquals(0, chosen, "no chest in a caved-in room");
                    } else {
                        assertTrue(chosen >= 1, room.kind() + " has a chest, seed " + seed);
                        assertTrue(
                            chosen <= room.marks()
                                .size(),
                            "never more chests than spots");
                    }
                }
            }
        }
    }

    @Test
    void theSafeIsAlwaysChosen() {
        for (long seed = 0; seed < 300; seed++) {
            ComplexPlan plan = synth(seed);
            for (ActivityLevel level : ActivityLevel.values()) {
                Set<Mark> chosen = ChestBudget.choose(
                    plan,
                    level,
                    states(plan, level, new Random(seed)),
                    Collections.<Integer>emptySet(),
                    target(level));
                for (Room room : plan.rooms()) {
                    for (Mark m : room.marks()) {
                        if (LootKeys.SAFE.equals(m.key())) assertTrue(chosen.contains(m), "the safe, seed " + seed);
                    }
                }
            }
        }
    }

    @Test
    void sameInputsSameChests() {
        for (long seed = 0; seed < 50; seed++) {
            ComplexPlan plan = synth(seed);
            Map<Integer, RoomState> st = states(plan, ActivityLevel.PARTLY_ABANDONED, new Random(seed));
            Set<Integer> down = collapsed(plan, ActivityLevel.PARTLY_ABANDONED, new Random(seed));
            Set<Mark> a = ChestBudget.choose(plan, ActivityLevel.PARTLY_ABANDONED, st, down, 200);
            Set<Mark> b = ChestBudget.choose(plan, ActivityLevel.PARTLY_ABANDONED, st, down, 200);
            assertEquals(a, b);
        }
    }

    @Test
    void themeSharesHoldOverManyComplexes() {
        // the aggregate share of each theme group stays within 30% (relative) of its planned share
        Map<RoomLoot.Group, Long> chests = new EnumMap<>(RoomLoot.Group.class);
        Map<RoomLoot.Group, Double> planned = new EnumMap<>(RoomLoot.Group.class);
        long total = 0;
        double plannedTotal = 0;
        for (long seed = 0; seed < 300; seed++) {
            ComplexPlan plan = synth(seed);
            ChestBudget.Allocation a = ChestBudget.allocate(
                plan,
                ActivityLevel.OPERATIONAL,
                states(plan, ActivityLevel.OPERATIONAL, new Random(seed)),
                Collections.<Integer>emptySet(),
                300);
            List<Room> all = new ArrayList<>(plan.rooms());
            all.add(plan.hall());
            for (Room room : all) {
                RoomLoot.Row row = RoomLoot.row(
                    room.kind()
                        .name());
                chests.merge(
                    row.group(),
                    (long) a.quotas()
                        .get(room.id()),
                    Long::sum);
                planned.merge(row.group(), (double) row.share(), Double::sum);
                plannedTotal += row.share();
            }
            total += 300;
        }
        for (RoomLoot.Group g : planned.keySet()) {
            double got = chests.get(g) / (double) total;
            double want = planned.get(g) / plannedTotal;
            assertTrue(Math.abs(got - want) <= 0.3 * want, g + ": " + got + " vs planned " + want);
        }
    }

    @Test
    void aLitRoomHoldsMoreChestsPerSpotThanADarkOne() {
        long litChests = 0, litSpots = 0, darkChests = 0, darkSpots = 0;
        for (long seed = 0; seed < 300; seed++) {
            ComplexPlan plan = synth(seed);
            Map<Integer, RoomState> st = states(plan, ActivityLevel.PARTLY_ABANDONED, new Random(seed));
            ChestBudget.Allocation a = ChestBudget
                .allocate(plan, ActivityLevel.PARTLY_ABANDONED, st, Collections.<Integer>emptySet(), 200);
            for (Room room : plan.rooms()) {
                int q = a.quotas()
                    .get(room.id());
                int n = a.slots()
                    .get(room.id());
                if (st.get(room.id()) == RoomState.IN_SERVICE) {
                    litChests += q;
                    litSpots += n;
                } else {
                    darkChests += q;
                    darkSpots += n;
                }
            }
        }
        assertTrue(litChests / (double) litSpots > darkChests / (double) darkSpots);
    }

    @Test
    void chestsSpreadThroughTheRoom() {
        // via the allocation of one room: pick 4 of 20 spots, expect them spread
        Room room = room(1, RoomKind.STOCKPILE, 28, 24, 20, false);
        List<Room> rooms = new ArrayList<>();
        rooms.add(room);
        ComplexPlan one = new ComplexPlan(
            1,
            0,
            0,
            room(0, RoomKind.ASSEMBLY_BAY, 51, 40, 1, false),
            rooms,
            new ArrayList<Tunnel>(),
            new ArrayList<SiloSlot>(),
            new ArrayList<Door>(),
            new int[] { 0, 0, 2, 0 },
            new ArrayList<int[]>());
        Set<Mark> chosen = ChestBudget.choose(one, ActivityLevel.OPERATIONAL, null, null, 5);
        List<Integer> idx = new ArrayList<>();
        List<Mark> chestSpots = new ArrayList<>();
        for (Mark m : room.marks()) if (m.kind() == MarkKind.LOOT_CHEST) chestSpots.add(m);
        for (int i = 0; i < chestSpots.size(); i++) if (chosen.contains(chestSpots.get(i))) idx.add(i);
        assertEquals(4, idx.size(), "hub 1, the room 4: " + idx);
        assertTrue(idx.get(0) < 5 && idx.get(3) >= 15, "spread, not bunched: " + idx);
    }

    @Test
    void aShortComplexPlacesEverySpotAndReportsTheShortfall() {
        for (long seed = 0; seed < 40; seed++) {
            ComplexPlan plan = ComplexLayout.generate(seed, 0, 0);
            ChestBudget.Allocation a = ChestBudget
                .allocate(plan, ActivityLevel.OPERATIONAL, null, Collections.<Integer>emptySet(), 300);
            assertEquals(
                Math.min(300, a.eligible()),
                a.chosen()
                    .size());
            assertEquals(Math.max(0, 300 - a.eligible()), a.shortfall());
        }
    }

    @Test
    void fewerChestsThanRoomsGoToTheHeaviestRooms() {
        ComplexPlan plan = synth(7);
        Set<Mark> chosen = ChestBudget.choose(plan, ActivityLevel.ABANDONED, null, null, 5);
        assertEquals(5, chosen.size());
    }

    @Test
    void targetsFallBackOnABadList() {
        assertEquals(3, ChestBudget.targetsFrom(null).length);
        assertEquals(100, ChestBudget.targetsFrom(new int[] { 1, 2 })[0]);
        assertEquals(100, ChestBudget.targetsFrom(new int[] { -1, 2, 3 })[0]);
        assertEquals(7, ChestBudget.targetsFrom(new int[] { 7, 8, 9 })[0]);
        assertEquals(100, ChestBudget.target(ActivityLevel.ABANDONED));
        assertEquals(200, ChestBudget.target(ActivityLevel.PARTLY_ABANDONED));
        assertEquals(300, ChestBudget.target(ActivityLevel.OPERATIONAL));
    }
}
