package com.gregguns.icbm.worldgen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gregguns.core.config.IcbmConfig;
import com.gregguns.icbm.complex.Box;
import com.gregguns.icbm.complex.ComplexPlan;
import com.gregguns.icbm.complex.LootKeys;
import com.gregguns.icbm.complex.Mark;
import com.gregguns.icbm.complex.MarkKind;
import com.gregguns.icbm.complex.Room;
import com.gregguns.icbm.complex.RoomKind;

/**
 * The chest budget (D315): picks exactly how many of a complex's chest spots become loot chests, 100 in an abandoned
 * complex, 200 in a partly abandoned one and 300 in an operational one (config starting values), the same on every
 * planet. Every chosen chest holds loot; a spot that is not chosen becomes a wooden crate with no inventory.
 *
 * <p>
 * The spots are the {@link MarkKind#LOOT_CHEST} marks of the hub and of every room that has not caved in. Tunnels hold
 * none, and a caved-in room gets neither chests nor crates. Armory display chests ({@link MarkKind#GUN_DISPLAY}) are
 * not spots: they sit outside the total.
 *
 * <p>
 * How the count is split:
 * <ol>
 * <li>Every room with a spot gets one chest.</li>
 * <li>The rest is split by largest remainder on the room's weight: its kind's share ({@link RoomLoot}) times its size
 * (interior over the kind's nominal interior, clamped 0.6 to 1.8) times its state (a partly abandoned complex's lit
 * room 1.3, a dark one 0.7).</li>
 * <li>No room takes more chests than it has spots. A full room's overflow goes first to the rooms of its theme group,
 * then to every room, until the count is placed or every spot is used.</li>
 * <li>Inside a room the commander's safe comes first, then the chests are spread evenly through the room's spot order,
 * so they are not bunched at one end.</li>
 * </ol>
 * Pure and deterministic: the answer depends on the plan, the level and the room states only, so every populate window
 * that rebuilds it agrees.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/loot-and-ceilings.md (Chest budget, GGI-08-15..17); D315
public final class ChestBudget {

    /** Starting chest totals: abandoned, partly abandoned, operational (owner, alpha 7). */
    public static final int[] DEFAULT_TARGETS = { 100, 200, 300 };

    private ChestBudget() {}

    /**
     * @param level a complex's activity level
     * @return the chest total for it, from {@code icbm.siloComplexChestTargets}
     */
    public static int target(ActivityLevel level) {
        return targetsFrom(IcbmConfig.siloComplexChestTargets)[level.ordinal()];
    }

    /**
     * @param configured the configured totals
     * @return them when they are three whole numbers, 0 or more; else {@link #DEFAULT_TARGETS}
     */
    public static int[] targetsFrom(int[] configured) {
        if (configured == null || configured.length != 3) {
            return DEFAULT_TARGETS;
        }
        for (int t : configured) {
            if (t < 0) {
                return DEFAULT_TARGETS;
            }
        }
        return configured;
    }

    /**
     * @param plan      the layout
     * @param level     the activity level
     * @param states    each room's state by room id; a missing room counts as the level's plain state
     * @param collapsed the ids of the rooms that caved in
     * @param target    how many chests to place
     * @return the chosen {@link MarkKind#LOOT_CHEST} marks, an identity set
     */
    public static Set<Mark> choose(ComplexPlan plan, ActivityLevel level, Map<Integer, RoomState> states,
        Set<Integer> collapsed, int target) {
        return allocate(plan, level, states, collapsed, target).chosen();
    }

    /** The budget's full answer, for the contents pass and the tests. */
    public static final class Allocation {

        private final Set<Mark> chosen;
        private final Map<Integer, Integer> quotas;
        private final Map<Integer, Integer> slots;
        private final int eligible;
        private final int target;

        Allocation(Set<Mark> chosen, Map<Integer, Integer> quotas, Map<Integer, Integer> slots, int eligible,
            int target) {
            this.chosen = Collections.unmodifiableSet(chosen);
            this.quotas = Collections.unmodifiableMap(quotas);
            this.slots = Collections.unmodifiableMap(slots);
            this.eligible = eligible;
            this.target = target;
        }

        /** @return the chosen chest marks (identity set) */
        public Set<Mark> chosen() {
            return chosen;
        }

        /** @return chests per room id, for every eligible room */
        public Map<Integer, Integer> quotas() {
            return quotas;
        }

        /** @return chest spots per eligible room id */
        public Map<Integer, Integer> slots() {
            return slots;
        }

        /** @return how many spots could hold a chest */
        public int eligible() {
            return eligible;
        }

        /** @return how many chests short of the target the complex is (0 when it has enough spots) */
        public int shortfall() {
            return Math.max(0, target - chosen.size());
        }
    }

    /** One eligible room during the split. */
    private static final class Entry {

        final Room room;
        final List<Mark> spots;
        final double weight;
        final RoomLoot.Group group;
        int quota;

        Entry(Room room, List<Mark> spots, double weight, RoomLoot.Group group) {
            this.room = room;
            this.spots = spots;
            this.weight = weight;
            this.group = group;
        }

        int room() {
            return spots.size() - quota;
        }
    }

    /**
     * As {@link #choose}, with the per-room quotas and the spot counts.
     *
     * @return the allocation
     */
    public static Allocation allocate(ComplexPlan plan, ActivityLevel level, Map<Integer, RoomState> states,
        Set<Integer> collapsed, int target) {
        List<Room> rooms = new ArrayList<>();
        rooms.add(plan.hall());
        rooms.addAll(plan.rooms());
        List<Entry> entries = new ArrayList<>();
        int eligible = 0;
        for (Room r : rooms) {
            if (collapsed != null && collapsed.contains(r.id())) {
                continue;
            }
            List<Mark> spots = new ArrayList<>();
            for (Mark m : r.marks()) {
                if (m.kind() == MarkKind.LOOT_CHEST) {
                    spots.add(m);
                }
            }
            if (spots.isEmpty()) {
                continue;
            }
            eligible += spots.size();
            RoomState s = states == null ? null : states.get(r.id());
            RoomLoot.Row row = RoomLoot.row(
                r.kind() == null ? null
                    : r.kind()
                        .name());
            entries.add(new Entry(r, spots, row.share() * size(r) * stateFactor(s), row.group()));
        }
        int n = Math.max(0, Math.min(target, eligible));
        if (n < entries.size()) {
            // fewer chests than rooms: the heaviest rooms get one each
            List<Entry> byWeight = new ArrayList<>(entries);
            byWeight.sort((a, b) -> Double.compare(b.weight, a.weight));
            for (int i = 0; i < n; i++) {
                byWeight.get(i).quota = 1;
            }
        } else {
            for (Entry e : entries) {
                e.quota = 1;
            }
            spread(entries, n - entries.size());
        }
        Set<Mark> chosen = Collections.newSetFromMap(new IdentityHashMap<>());
        Map<Integer, Integer> quotas = new LinkedHashMap<>();
        Map<Integer, Integer> slots = new LinkedHashMap<>();
        for (Entry e : entries) {
            chosen.addAll(pickSpots(e.spots, e.quota));
            quotas.put(e.room.id(), e.quota);
            slots.put(e.room.id(), e.spots.size());
        }
        return new Allocation(chosen, quotas, slots, eligible, target);
    }

    /**
     * Places {@code amount} more chests by largest remainder on weight, capped at each room's spots. A full room's
     * overflow goes to the open rooms of its own theme group first, then to every open room, round after round.
     */
    private static void spread(List<Entry> entries, int amount) {
        int left = amount;
        while (left > 0) {
            List<Entry> open = open(entries, null);
            if (open.isEmpty()) {
                return;
            }
            Map<RoomLoot.Group, Integer> overflow = new LinkedHashMap<>();
            give(open, left, overflow);
            left = 0;
            for (Map.Entry<RoomLoot.Group, Integer> o : overflow.entrySet()) {
                int spill = o.getValue();
                List<Entry> mates = open(entries, o.getKey());
                if (!mates.isEmpty()) {
                    Map<RoomLoot.Group, Integer> again = new LinkedHashMap<>();
                    give(mates, spill, again);
                    spill = 0;
                    for (int v : again.values()) {
                        spill += v;
                    }
                }
                left += spill;
            }
        }
    }

    private static List<Entry> open(List<Entry> entries, RoomLoot.Group group) {
        List<Entry> out = new ArrayList<>();
        for (Entry e : entries) {
            if (e.room() > 0 && (group == null || e.group == group)) {
                out.add(e);
            }
        }
        return out;
    }

    /**
     * Largest remainder of {@code amount} over {@code rooms} by weight; what a room cannot take past its spots goes to
     * {@code overflow} under its group. Ties go to the room listed first, so the split is deterministic.
     */
    private static void give(List<Entry> rooms, int amount, Map<RoomLoot.Group, Integer> overflow) {
        double total = 0;
        for (Entry e : rooms) {
            total += e.weight;
        }
        int[] raw = new int[rooms.size()];
        double[] rem = new double[rooms.size()];
        int given = 0;
        for (int i = 0; i < raw.length; i++) {
            double ideal = total > 0 ? amount * rooms.get(i).weight / total : (double) amount / raw.length;
            raw[i] = (int) Math.floor(ideal);
            rem[i] = ideal - raw[i];
            given += raw[i];
        }
        for (int k = given; k < amount; k++) {
            int best = -1;
            for (int i = 0; i < rem.length; i++) {
                if (rem[i] >= 0 && (best < 0 || rem[i] > rem[best])) {
                    best = i;
                }
            }
            raw[best]++;
            rem[best] = -1;
        }
        for (int i = 0; i < raw.length; i++) {
            Entry e = rooms.get(i);
            int take = Math.min(raw[i], e.room());
            e.quota += take;
            if (raw[i] > take) {
                overflow.merge(e.group, raw[i] - take, Integer::sum);
            }
        }
    }

    /**
     * @param spots a room's chest spots, in its order (primary first)
     * @param q     how many to take
     * @return the safe first, then the rest spread evenly through the order
     */
    static List<Mark> pickSpots(List<Mark> spots, int q) {
        List<Mark> out = new ArrayList<>();
        List<Mark> rest = new ArrayList<>();
        for (Mark m : spots) {
            if (LootKeys.SAFE.equals(m.key()) && out.size() < q) {
                out.add(m);
            } else {
                rest.add(m);
            }
        }
        int r = q - out.size();
        int m = rest.size();
        for (int i = 0; i < r && i < m; i++) {
            out.add(rest.get((int) Math.floor((i + 0.5) * m / r)));
        }
        return out;
    }

    /** @return the room's interior over its kind's nominal interior, clamped 0.6 to 1.8 */
    static double size(Room r) {
        RoomKind k = r.kind();
        Box b = r.bounds();
        if (k == null || b == null) {
            return 1.0;
        }
        double area = Math.max(1, b.maxX - b.minX - 2) * (double) Math.max(1, b.maxZ - b.minZ - 2);
        double nominal = Math.max(1, k.length - 2) * (double) Math.max(1, k.depth - 2);
        return Math.max(0.6, Math.min(1.8, area / nominal));
    }

    /** @return the weight factor of a room state: a lit, worked room is better stocked than a dark one */
    static double stateFactor(RoomState s) {
        if (s == RoomState.IN_SERVICE) {
            return 1.3;
        }
        if (s == RoomState.MOTHBALLED) {
            return 0.7;
        }
        return 1.0;
    }
}
