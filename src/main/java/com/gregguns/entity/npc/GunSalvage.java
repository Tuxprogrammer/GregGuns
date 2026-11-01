package com.gregguns.entity.npc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.gregguns.gun.def.GunDefinition;
import com.gregguns.gun.def.GunDefinitions;
import com.gregguns.recipe.ComponentTable;
import com.gregguns.recipe.ProductionTable;

/**
 * What a dead NPC's energy gun leaves instead of ammo (D312): a few parts and materials from the gun's own recipe
 * tree. An energy rifle has no ammo item and never drops a battery; its kill drops salvage instead.
 *
 * <p>
 * The pool is read from the gun's assembly row and, one level down, from the rows of the GregGuns components it takes
 * (a receiver's plates and screws, a barrel's foil and focus). It holds:
 * <ul>
 * <li>every GT part (plates, screws, foil, small gears and so on) at {@link #PART_WEIGHT}, 1 to half its recipe count
 * (at most {@link #MAX_PART_COUNT});</li>
 * <li>every GregGuns item (receivers, barrels, stocks, mechanical parts) at {@link #ITEM_WEIGHT}, one at a time.</li>
 * </ul>
 * Circuits and GT {@code ItemList} parts (batteries, emitters, motors) are left out, as are fluids and ore inputs.
 * Each kill makes {@link #DRAWS} weighted picks. Simulation-friendly: integer draws on the given random source, the
 * same number of draws whatever is picked. No Minecraft types; both sides.
 */
// Spec: docs/gregguns-spec/10-entities-npcs/mob-drops-nei.md (GGS-10-40)
public final class GunSalvage {

    /** Weighted picks per kill. */
    public static final int DRAWS = 2;
    /** Weight of a GT part in the pool. */
    public static final int PART_WEIGHT = 3;
    /** Weight of a GregGuns item in the pool. */
    public static final int ITEM_WEIGHT = 1;
    /** Most of one GT part a single pick drops. */
    public static final int MAX_PART_COUNT = 4;

    private static final String GT_PREFIX = "gt:";
    private static final String MOD_PREFIX = "gregguns:";

    /** One kind of salvage: an item reference, its count range and its weight in the pool. */
    public static final class Entry {

        private final String ref;
        private final int min;
        private final int max;
        private final int weight;

        Entry(String ref, int min, int max, int weight) {
            this.ref = ref;
            this.min = min;
            this.max = max;
            this.weight = weight;
        }

        /** @return {@code gt:<prefix>.<Material>} for a GT part, {@code gregguns:<name>} for one of ours */
        public String ref() {
            return ref;
        }

        /** @return fewest dropped by one pick */
        public int min() {
            return min;
        }

        /** @return most dropped by one pick */
        public int max() {
            return max;
        }

        /** @return weight in the pool */
        public int weight() {
            return weight;
        }

        @Override
        public String toString() {
            return ref + " " + min + "-" + max + " w" + weight;
        }
    }

    /** One pick: an item reference and a count. */
    public static final class Pick {

        private final String ref;
        private final int count;

        Pick(String ref, int count) {
            this.ref = ref;
            this.count = count;
        }

        /** @return the item reference, as {@link Entry#ref} */
        public String ref() {
            return ref;
        }

        /** @return how many */
        public int count() {
            return count;
        }
    }

    private GunSalvage() {}

    /**
     * @param gunName a gun's registry name
     * @return its salvage pool, in recipe order; empty for an unknown gun or one with no assembly row
     */
    public static List<Entry> pool(String gunName) {
        GunDefinition def = GunDefinitions.byName(gunName);
        ProductionTable.Row row = def == null ? null : def.assembly();
        if (row == null) {
            return Collections.emptyList();
        }
        Map<String, Entry> out = new LinkedHashMap<>();
        Map<String, ProductionTable.Row> components = ComponentTable.byOutput();
        for (ProductionTable.Input in : row.inputs()) {
            add(out, in);
        }
        for (ProductionTable.Input in : row.inputs()) {
            ProductionTable.Row sub = in.isGregGuns() ? components.get(in.itemName()) : null;
            if (sub != null) {
                for (ProductionTable.Input part : sub.inputs()) {
                    add(out, part);
                }
            }
        }
        return Collections.unmodifiableList(new ArrayList<>(out.values()));
    }

    private static void add(Map<String, Entry> out, ProductionTable.Input in) {
        Entry e = entry(in);
        if (e == null) {
            return;
        }
        Entry had = out.get(e.ref());
        // The same part twice in the tree (titanium screws in the gun and in its mechanical parts) counts once, with
        // the larger count range.
        if (had == null || had.max() < e.max()) {
            out.put(e.ref(), e);
        }
    }

    private static Entry entry(ProductionTable.Input in) {
        if (in.nonConsumed()) {
            return null;
        }
        if (in.isGregGuns()) {
            return new Entry(MOD_PREFIX + in.itemName(), 1, 1, ITEM_WEIGHT);
        }
        if (in.special() != null || in.oreName() != null || in.prefix() == null || "circuit".equals(in.prefix())) {
            return null;
        }
        int max = Math.max(1, Math.min(MAX_PART_COUNT, (in.count() + 1) / 2));
        return new Entry(GT_PREFIX + in.prefix() + "." + in.material(), 1, max, PART_WEIGHT);
    }

    /**
     * Makes this kill's picks.
     *
     * @param pool the gun's pool, from {@link #pool}
     * @param rand the NPC's random source
     * @return {@link #DRAWS} picks, or none for an empty pool
     */
    public static List<Pick> roll(List<Entry> pool, Random rand) {
        int total = 0;
        for (Entry e : pool) {
            total += e.weight();
        }
        if (total <= 0) {
            return Collections.emptyList();
        }
        List<Pick> out = new ArrayList<>(DRAWS);
        for (int i = 0; i < DRAWS; i++) {
            int w = rand.nextInt(total);
            Entry pick = pool.get(pool.size() - 1);
            for (Entry e : pool) {
                if (w < e.weight()) {
                    pick = e;
                    break;
                }
                w -= e.weight();
            }
            out.add(new Pick(pick.ref(), pick.min() + rand.nextInt(pick.max() - pick.min() + 1)));
        }
        return out;
    }

    /**
     * @param pool  the gun's pool
     * @param entry one of its entries
     * @return how many of that entry one kill drops on average
     */
    public static double expectedCount(List<Entry> pool, Entry entry) {
        int total = 0;
        for (Entry e : pool) {
            total += e.weight();
        }
        return total <= 0 ? 0.0 : DRAWS * (double) entry.weight() / total * (entry.min() + entry.max()) / 2.0;
    }
}
