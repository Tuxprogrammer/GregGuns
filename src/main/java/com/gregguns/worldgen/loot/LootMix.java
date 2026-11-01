package com.gregguns.worldgen.loot;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.gregguns.api.Tier;
import com.gregguns.entity.npc.loot.LootDrop;

/**
 * Weighs pools into one {@link LootCategory}: each pool gets a share of the category's picks, split over its lines by
 * their in-pool weights. Pure.
 *
 * <p>
 * Lines above the category ceiling are dropped first, so a pool that runs up to ZPM scales to any table, and the
 * share of a pool is the share of what is left. A reference that two pools both hold (arrows are junk and melee) is
 * merged into one line: the weights add, the count range widens, the higher tier and the first pool name stay. Every
 * line takes its pool's name ({@link LootEntry#pool()}).
 *
 * <p>
 * Usage: {@code LootMix.of(name, Tier.MV, 4, 9).pool(30, Arsenal.ammo(2)).pool(5, Arsenal.guns(2)).build()}.
 */
// Spec: docs/gregguns-spec/06-items/pack-loot.md (Structure chest mix)
public final class LootMix {

    /** Flattened weight per share point: a share of 1 split over a pool is worth this much weight in total. */
    static final int SCALE = 100;

    private final String name;
    private final Tier ceiling;
    private final int min;
    private final int max;
    private final Map<String, LootEntry> lines = new LinkedHashMap<>();
    private final Map<String, Integer> shares = new LinkedHashMap<>();

    private LootMix(String name, Tier ceiling, int min, int max) {
        this.name = name;
        this.ceiling = ceiling;
        this.min = min;
        this.max = max;
    }

    /**
     * @param name    the category name, {@code gregguns:<name>}
     * @param ceiling the category ceiling; lines above it are dropped
     * @param min     fewest picks per chest
     * @param max     most picks per chest
     * @return a mix with no pools yet
     */
    public static LootMix of(String name, Tier ceiling, int min, int max) {
        return new LootMix(name, ceiling, min, max);
    }

    /**
     * Adds a pool with a share of the picks. A pool with no line at or below the ceiling adds nothing.
     *
     * @param share the pool's share, in points (a category's shares usually add to 100)
     * @param pool  the pool
     * @return this
     */
    public LootMix pool(int share, LootPools.Pool pool) {
        if (share < 1) {
            throw new IllegalArgumentException("share " + share + " for " + pool.name());
        }
        List<LootEntry> kept = new ArrayList<>();
        int total = 0;
        for (LootEntry e : pool.lines()) {
            if (e.tier()
                .ordinal() <= ceiling.ordinal()) {
                kept.add(e);
                total += e.weight();
            }
        }
        if (total == 0) {
            return this;
        }
        shares.merge(pool.name(), share, Integer::sum);
        for (LootEntry e : kept) {
            int w = Math.max(1, (int) Math.round((double) share * SCALE * e.weight() / total));
            merge(new LootEntry(e.drop(), w, e.tier(), pool.name(), e.companion()));
        }
        return this;
    }

    private void merge(LootEntry e) {
        LootEntry old = lines.get(e.ref());
        if (old == null) {
            lines.put(e.ref(), e);
            return;
        }
        LootDrop d = LootDrop.range(
            e.ref(),
            Math.min(
                old.drop()
                    .min(),
                e.drop()
                    .min()),
            Math.max(
                old.drop()
                    .max(),
                e.drop()
                    .max()));
        Tier tier = old.tier()
            .ordinal()
            >= e.tier()
                .ordinal() ? old.tier() : e.tier();
        lines.put(
            e.ref(),
            new LootEntry(
                d,
                old.weight() + e.weight(),
                tier,
                old.pool(),
                old.companion() != null ? old.companion() : e.companion()));
    }

    /** @return the share points of each pool added, in order; for tests and the docs */
    public Map<String, Integer> shares() {
        return new LinkedHashMap<>(shares);
    }

    /** @return the category */
    public LootCategory build() {
        LootCategory.Builder b = LootCategory.builder(name, ceiling, min, max);
        for (LootEntry e : lines.values()) {
            b.add(e);
        }
        return b.build();
    }
}
