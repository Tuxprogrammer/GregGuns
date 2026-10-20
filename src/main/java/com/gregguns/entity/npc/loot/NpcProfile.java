package com.gregguns.entity.npc.loot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.gregguns.api.Tier;

/**
 * Everything the loot and equipment core knows about one mob: its drop table, fixed drops, whether the held item can
 * drop as a rare drop, what it carries, and its tier ceiling (everything it can hold, wear or drop is at or below it,
 * GGS-10-21). Pure data.
 */
// Spec: docs/gregguns-spec/10-entities-npcs/mob-drops-nei.md (GGS-10-17, GGS-10-21)
public final class NpcProfile {

    private final String id;
    private final Tier ceiling;
    private final @Nullable LootTable table;
    private final List<LootDrop> fixedDrops;
    private final boolean rareDrop;
    private final @Nullable EquipmentProfile equipment;

    private NpcProfile(Builder b) {
        this.id = b.id;
        this.ceiling = b.ceiling;
        this.table = b.table;
        this.fixedDrops = Collections.unmodifiableList(new ArrayList<>(b.fixed));
        this.rareDrop = b.rareDrop;
        this.equipment = b.equipment;
    }

    /**
     * @param id      the mob's registry name without the mod id, for example {@code ZombieSoldier}
     * @param ceiling the tier ceiling
     * @return a builder; the rare drop is on by default
     */
    public static Builder builder(String id, Tier ceiling) {
        return new Builder(id, ceiling);
    }

    /** @return the mob's registry name without the mod id */
    public String id() {
        return id;
    }

    /** @return the tier ceiling */
    public Tier ceiling() {
        return ceiling;
    }

    /** @return the random drop table, or null when the mob has none */
    public @Nullable LootTable table() {
        return table;
    }

    /** @return drops that always fall, however the mob died */
    public List<LootDrop> fixedDrops() {
        return fixedDrops;
    }

    /** @return true when a player kill may drop a fresh copy of the held item (2.5% plus looting) */
    public boolean rareDrop() {
        return rareDrop;
    }

    /** @return what the mob carries, or null for nothing */
    public @Nullable EquipmentProfile equipment() {
        return equipment;
    }

    /** Builds a profile. */
    public static final class Builder {

        private final String id;
        private final Tier ceiling;
        private LootTable table;
        private final List<LootDrop> fixed = new ArrayList<>();
        private boolean rareDrop = true;
        private EquipmentProfile equipment;

        Builder(String id, Tier ceiling) {
            this.id = id;
            this.ceiling = ceiling;
        }

        /**
         * @param t the random drop table
         * @return this
         */
        public Builder table(LootTable t) {
            table = t;
            return this;
        }

        /**
         * @param drop a drop that always falls
         * @return this
         */
        public Builder fixed(LootDrop drop) {
            fixed.add(drop);
            return this;
        }

        /**
         * @param on whether the held item can drop as a rare drop
         * @return this
         */
        public Builder rareDrop(boolean on) {
            rareDrop = on;
            return this;
        }

        /**
         * @param e what the mob carries
         * @return this
         */
        public Builder equipment(EquipmentProfile e) {
            equipment = e;
            return this;
        }

        /** @return the profile */
        public NpcProfile build() {
            return new NpcProfile(this);
        }
    }
}
