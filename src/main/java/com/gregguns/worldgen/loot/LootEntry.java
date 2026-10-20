package com.gregguns.worldgen.loot;

import java.util.Objects;

import com.gregguns.api.Tier;
import com.gregguns.entity.npc.loot.LootDrop;

/**
 * One line of a structure loot category: an item reference with a count range (the M7 {@link LootDrop}, reused), a
 * weight, and the D1 tier the line declares. Pure.
 *
 * <p>
 * The reference scheme is {@code LootDrop}'s ({@code mod:name}, or a bare GregGuns gun name) plus
 * {@code gt:prefix.Material} for a GregTech part fetched through the unificator (GGS-06-03), for example
 * {@code gt:ingot.Tin}.
 */
// Spec: docs/gregguns-spec/11-worldgen/structure-loot.md (GGS-11-21); docs/gregguns-spec/06-items/pack-loot.md
public final class LootEntry {

    private final LootDrop drop;
    private final int weight;
    private final Tier tier;

    /**
     * @param drop   the item and its count range
     * @param weight the base weight, at least 1
     * @param tier   the tier this line declares (0001); checked against the category ceiling
     */
    public LootEntry(LootDrop drop, int weight, Tier tier) {
        if (weight < 1) {
            throw new IllegalArgumentException("weight " + weight + " for " + drop.ref());
        }
        this.drop = Objects.requireNonNull(drop);
        this.weight = weight;
        this.tier = Objects.requireNonNull(tier);
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
}
