package com.gregguns.worldgen.loot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.gregguns.api.Tier;
import com.gregguns.entity.npc.loot.LootDrop;

/**
 * A structure chest category: its {@code gregguns:*} name, the pick count of one chest, a maximum tier, and its
 * weighted lines. Pure data. Worldgen categories are deliberately separate from the NPC profiles in
 * {@code entity.npc.loot}; only {@link LootDrop} is shared.
 */
// Spec: docs/gregguns-spec/11-worldgen/structure-loot.md (GGS-11-21); docs/gregguns-spec/06-items/pack-loot.md
public final class LootCategory {

    private final String name;
    private final Tier ceiling;
    private final int minItems;
    private final int maxItems;
    private final List<LootEntry> entries;

    private LootCategory(String name, Tier ceiling, int minItems, int maxItems, List<LootEntry> entries) {
        this.name = name;
        this.ceiling = ceiling;
        this.minItems = minItems;
        this.maxItems = maxItems;
        this.entries = Collections.unmodifiableList(entries);
    }

    /**
     * @param name     the category name, {@code gregguns:<name>}
     * @param ceiling  the highest tier any line may declare
     * @param minItems fewest picks per chest
     * @param maxItems most picks per chest
     * @return a builder
     */
    public static Builder builder(String name, Tier ceiling, int minItems, int maxItems) {
        return new Builder(name, ceiling, minItems, maxItems);
    }

    /** @return the {@code gregguns:*} name */
    public String name() {
        return name;
    }

    /** @return the maximum tier (GGS-11-21) */
    public Tier ceiling() {
        return ceiling;
    }

    /** @return fewest picks per chest */
    public int minItems() {
        return minItems;
    }

    /** @return most picks per chest */
    public int maxItems() {
        return maxItems;
    }

    /** @return the lines, in a stable order */
    public List<LootEntry> entries() {
        return entries;
    }

    /** Builds a category line by line. */
    public static final class Builder {

        private final String name;
        private final Tier ceiling;
        private final int min;
        private final int max;
        private final List<LootEntry> entries = new ArrayList<>();

        private Builder(String name, Tier ceiling, int min, int max) {
            if (!name.startsWith("gregguns:") || min < 1 || max < min) {
                throw new IllegalArgumentException("category " + name + " " + min + ".." + max);
            }
            this.name = name;
            this.ceiling = Objects.requireNonNull(ceiling);
            this.min = min;
            this.max = max;
        }

        /**
         * @param ref    item reference
         * @param min    smallest stack
         * @param max    largest stack
         * @param weight base weight
         * @param tier   declared tier
         * @return this
         */
        public Builder add(String ref, int min, int max, int weight, Tier tier) {
            entries.add(new LootEntry(LootDrop.range(ref, min, max), weight, tier));
            return this;
        }

        /** @return the category */
        public LootCategory build() {
            return new LootCategory(name, ceiling, min, max, new ArrayList<>(entries));
        }
    }
}
