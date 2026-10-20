package com.gregguns.entity.npc.loot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.jetbrains.annotations.Nullable;

/**
 * A mob's loot table: a list of equally likely slots, each a {@link LootDrop} or nothing. This is Techguns'
 * {@code nextInt(n)} switch as data, so a slot repeated twice is twice as likely. Pure.
 *
 * <p>
 * {@link #rolls} gives how many slots one death draws: {@code nextInt(3 + looting)}, so 0 to 2 plus looting, as in
 * Techguns. Everything draws from the caller's {@link Random} (the entity's own), nothing else (GGS-10-15).
 */
// Spec: docs/techguns-spec/10-entities-npcs/per-mob.md (Drops), docs/gregguns-spec/10-entities-npcs/mob-drops-nei.md
public final class LootTable {

    private final List<LootDrop> slots;

    private LootTable(List<LootDrop> slots) {
        this.slots = Collections.unmodifiableList(slots);
    }

    /** @return a builder */
    public static Builder builder() {
        return new Builder();
    }

    /** @return the slots in order; an entry is null for "nothing" */
    public List<LootDrop> slots() {
        return slots;
    }

    /**
     * @param rand    the entity's random source
     * @param looting the looting level of the kill
     * @return how many slots this death draws
     */
    public static int rolls(Random rand, int looting) {
        return rand.nextInt(3 + Math.max(0, looting));
    }

    /**
     * @param rand the entity's random source
     * @return one slot's drop, or null for nothing
     */
    public @Nullable LootDrop pick(Random rand) {
        return slots.isEmpty() ? null : slots.get(rand.nextInt(slots.size()));
    }

    /** Builds a table slot by slot. */
    public static final class Builder {

        private final List<LootDrop> slots = new ArrayList<>();

        /**
         * @param drop a drop
         * @return this
         */
        public Builder slot(LootDrop drop) {
            slots.add(drop);
            return this;
        }

        /**
         * @param drop  a drop
         * @param times how many slots it fills
         * @return this
         */
        public Builder slots(LootDrop drop, int times) {
            for (int i = 0; i < times; i++) {
                slots.add(drop);
            }
            return this;
        }

        /**
         * @param times how many empty slots
         * @return this
         */
        public Builder nothing(int times) {
            for (int i = 0; i < times; i++) {
                slots.add(null);
            }
            return this;
        }

        /** @return the table */
        public LootTable build() {
            return new LootTable(new ArrayList<>(slots));
        }
    }
}
