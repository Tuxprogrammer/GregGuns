package com.gregguns.worldgen.loot;

import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import com.gregguns.api.Tier;
import com.gregguns.entity.npc.loot.LootDrop;

/**
 * One line of a structure loot category: an item reference with a count range (the M7 {@link LootDrop}, reused), a
 * weight, the D1 tier the line declares, the pool it came from, and an optional companion. Pure.
 *
 * <p>
 * The reference scheme is {@code LootDrop}'s ({@code mod:name}, {@code mod:name#meta}, or a bare GregGuns gun name)
 * plus {@code gt:prefix.Material} for a GregTech part fetched through the unificator (GGS-06-03), for example
 * {@code gt:ingot.Tin}, and a {@code ~full} suffix for a GregGuns fluid tank that comes filled.
 *
 * <p>
 * A companion is a second stack that always comes with this line when it is drawn, without using a pick: a gun's
 * ammo, so a chest that holds a gun also holds something to fire from it.
 */
// Spec: docs/gregguns-spec/11-worldgen/structure-loot.md (GGS-11-21, GGS-11-26);
// docs/gregguns-spec/06-items/pack-loot.md
public final class LootEntry {

    private final LootDrop drop;
    private final int weight;
    private final Tier tier;
    private final String pool;
    private final @Nullable LootDrop companion;

    /**
     * A line with no pool name and no companion.
     *
     * @param drop   the item and its count range
     * @param weight the base weight, at least 1
     * @param tier   the tier this line declares (0001); checked against the category ceiling
     */
    public LootEntry(LootDrop drop, int weight, Tier tier) {
        this(drop, weight, tier, "", null);
    }

    /**
     * @param drop      the item and its count range
     * @param weight    the base weight, at least 1
     * @param tier      the tier this line declares (0001); checked against the category ceiling
     * @param pool      the pool the line came from ({@link LootPools}), for tests and reports; may be empty
     * @param companion a stack that comes with this line when drawn, or null
     */
    public LootEntry(LootDrop drop, int weight, Tier tier, String pool, @Nullable LootDrop companion) {
        if (weight < 1) {
            throw new IllegalArgumentException("weight " + weight + " for " + drop.ref());
        }
        this.drop = Objects.requireNonNull(drop);
        this.weight = weight;
        this.tier = Objects.requireNonNull(tier);
        this.pool = Objects.requireNonNull(pool);
        this.companion = companion;
    }

    /** @return the item reference and count range */
    public LootDrop drop() {
        return drop;
    }

    /** @return the item reference */
    public String ref() {
        return drop.ref();
    }

    /** @return the base weight */
    public int weight() {
        return weight;
    }

    /** @return the declared tier */
    public Tier tier() {
        return tier;
    }

    /** @return the pool name ({@link LootPools}), or empty for a hand-written line */
    public String pool() {
        return pool;
    }

    /** @return the stack that always comes with this line, or null */
    public @Nullable LootDrop companion() {
        return companion;
    }

    /**
     * @param weight the new weight
     * @return this line with another weight
     */
    public LootEntry withWeight(int weight) {
        return new LootEntry(drop, weight, tier, pool, companion);
    }

    /** @return this line without its companion (the companion's item is not in the pack) */
    public LootEntry withoutCompanion() {
        return new LootEntry(drop, weight, tier, pool, null);
    }
}
