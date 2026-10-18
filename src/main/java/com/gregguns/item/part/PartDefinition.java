package com.gregguns.item.part;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.gregguns.api.Tier;
import com.gregguns.recipe.ProductionTable;

/**
 * One gun component (a barrel, mechanical parts, a stock) as plain data: its registry name, tier, the machine row that
 * makes it (if any) and the hooks for its hand recipe and its GT recycling data. {@link PartDefinitions#ALL} lists
 * them; {@link PartItems} registers one {@link ItemPart} per entry, {@code ProductionTable} collects the rows and
 * {@code ProductionRecipes} runs the hooks. A part several guns share is defined once, here, and a gun names it by
 * registry name in its own recipe row.
 *
 * <p>
 * No Minecraft types, so unit tests can read the whole list. The hooks are {@link Runnable}s that call into the
 * recipe package from a lambda body, which keeps GT classes from loading until a recipe is actually registered.
 * Both sides.
 */
// Spec: docs/gregguns-spec/06-items/recipes.md (component index)
public final class PartDefinition {

    private final String name;
    private final Tier tier;
    private final List<ProductionTable.Row> rows;
    private final boolean handCrafted;
    private final Runnable recipes;
    private final Runnable recycling;

    private PartDefinition(Builder b) {
        this.name = b.name;
        this.tier = b.tier;
        this.rows = Collections.unmodifiableList(new ArrayList<>(b.rows));
        this.handCrafted = b.handCrafted;
        this.recipes = b.recipes;
        this.recycling = b.recycling;
    }

    /**
     * @param name the registry name without the mod id, for example {@code "part_barrel_iron"}; also the texture name
     * @param tier the part's D1 tier
     * @return a builder
     */
    public static Builder of(String name, Tier tier) {
        return new Builder(name, tier);
    }

    /** @return the registry name without the mod id */
    public String name() {
        return name;
    }

    /** @return the tier */
    public Tier tier() {
        return tier;
    }

    /** @return the machine rows that make this part; empty when it is only crafted by hand */
    public List<ProductionTable.Row> rows() {
        return rows;
    }

    /** @return true if the part is made at the crafting table, so guns may use it with no machine row */
    public boolean handCrafted() {
        return handCrafted;
    }

    /** @return the hook that registers the hand recipe, or null. Run on GT's before-postload hook. */
    public Runnable recipes() {
        return recipes;
    }

    /** @return the hook that attaches GT recycling data, or null. Run on GT's before-postload hook. */
    public Runnable recycling() {
        return recycling;
    }

    /** Fluent builder for {@link PartDefinition}. */
    public static final class Builder {

        private final String name;
        private final Tier tier;
        private final List<ProductionTable.Row> rows = new ArrayList<>();
        private boolean handCrafted;
        private Runnable recipes;
        private Runnable recycling;

        private Builder(String name, Tier tier) {
            this.name = name;
            this.tier = tier;
        }

        /**
         * @param row a machine row whose output is this part
         * @return this
         */
        public Builder row(ProductionTable.Row row) {
            rows.add(row);
            return this;
        }

        /**
         * Marks the part as crafted by hand (ULV).
         *
         * @return this
         */
        public Builder handCrafted() {
            this.handCrafted = true;
            return this;
        }

        /**
         * @param hook registers the part's crafting-table recipe
         * @return this
         */
        public Builder recipes(Runnable hook) {
            this.recipes = hook;
            return this;
        }

        /**
         * @param hook attaches the part's GT {@code ItemData}
         * @return this
         */
        public Builder recycling(Runnable hook) {
            this.recycling = hook;
            return this;
        }

        /** @return the definition */
        public PartDefinition build() {
            return new PartDefinition(this);
        }
    }
}
