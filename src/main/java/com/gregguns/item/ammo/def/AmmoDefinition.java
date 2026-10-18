package com.gregguns.item.ammo.def;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.gregguns.item.ammo.AmmoType;
import com.gregguns.item.ammo.MagazineType;
import com.gregguns.recipe.ProductionTable;

/**
 * One ammo type as plain data: the loose round ({@link AmmoType}), the magazines that carry it, the Ammo Press rows
 * that make the round and its magazines, the crafting-table refills, and the hooks for hand recipes and GT recycling
 * data. {@link AmmoDefinitions#ALL} lists them; {@code AmmoItems} registers the items, {@code AmmoTypes} indexes the
 * types and magazines, {@code ProductionTable} collects the rows and {@code ProductionRecipes} runs the refills and
 * hooks. Adding a caliber is one new definition class and one line in that list.
 *
 * <p>
 * No Minecraft types, so unit tests can read the whole list. The hooks are {@link Runnable}s that call into the
 * recipe package from a lambda body, which keeps GT classes from loading until a recipe is registered. Both sides.
 */
// Spec: docs/gregguns-spec/06-items/ammo.md, docs/techguns-spec/06-items/ammo.md
public final class AmmoDefinition {

    private final AmmoType type;
    private final List<MagazineType> magazines;
    private final List<ProductionTable.Row> rows;
    private final List<ProductionTable.Refill> refills;
    private final Runnable recipes;
    private final Runnable recycling;

    private AmmoDefinition(Builder b) {
        this.type = b.type;
        this.magazines = Collections.unmodifiableList(new ArrayList<>(b.magazines));
        this.rows = Collections.unmodifiableList(new ArrayList<>(b.rows));
        this.refills = Collections.unmodifiableList(new ArrayList<>(b.refills));
        this.recipes = b.recipes;
        this.recycling = b.recycling;
    }

    /**
     * @param type the loose round
     * @return a builder
     */
    public static Builder of(AmmoType type) {
        return new Builder(type);
    }

    /** @return the loose round type (its id is what guns call the ammo) */
    public AmmoType type() {
        return type;
    }

    /** @return the magazines that carry this round, in registration order; usually empty or one */
    public List<MagazineType> magazines() {
        return magazines;
    }

    /** @return the Ammo Press rows that make the round and its magazines */
    public List<ProductionTable.Row> rows() {
        return rows;
    }

    /** @return the crafting-table magazine refills (RC-51) */
    public List<ProductionTable.Refill> refills() {
        return refills;
    }

    /** @return the hook that registers hand recipes, or null. Run on GT's before-postload hook. */
    public Runnable recipes() {
        return recipes;
    }

    /** @return the hook that attaches GT recycling data, or null. Run on GT's before-postload hook. */
    public Runnable recycling() {
        return recycling;
    }

    /** Fluent builder for {@link AmmoDefinition}. */
    public static final class Builder {

        private final AmmoType type;
        private final List<MagazineType> magazines = new ArrayList<>();
        private final List<ProductionTable.Row> rows = new ArrayList<>();
        private final List<ProductionTable.Refill> refills = new ArrayList<>();
        private Runnable recipes;
        private Runnable recycling;

        private Builder(AmmoType type) {
            this.type = type;
        }

        /**
         * @param magazine a magazine family filled with this round
         * @return this
         */
        public Builder magazine(MagazineType magazine) {
            magazines.add(magazine);
            return this;
        }

        /**
         * @param row an Ammo Press row whose output is this round or one of its magazines
         * @return this
         */
        public Builder row(ProductionTable.Row row) {
            rows.add(row);
            return this;
        }

        /**
         * @param refill a shapeless magazine refill
         * @return this
         */
        public Builder refill(ProductionTable.Refill refill) {
            refills.add(refill);
            return this;
        }

        /**
         * @param hook registers hand (crafting-table) recipes
         * @return this
         */
        public Builder recipes(Runnable hook) {
            this.recipes = hook;
            return this;
        }

        /**
         * @param hook attaches GT {@code ItemData} for the round and its magazines
         * @return this
         */
        public Builder recycling(Runnable hook) {
            this.recycling = hook;
            return this;
        }

        /** @return the definition */
        public AmmoDefinition build() {
            return new AmmoDefinition(this);
        }
    }
}
