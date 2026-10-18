package com.gregguns.gun.def;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import com.gregguns.api.Tier;
import com.gregguns.combat.DamageType;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.sound.GunSoundEvents;
import com.gregguns.recipe.ProductionTable;

/**
 * Everything the common side knows about one gun, in one object: its {@link GunStats}, the item's registration data
 * (registry name, tier, damage type, sound events, camo count), its final-assembly recipe rows, the hooks for its
 * crafting-table recipes and GT recycling data, and its in-game scenarios.
 *
 * <p>
 * A gun is one definition class (for example {@code PistolDef}) with a {@code DEFINITION} constant, plus one line in
 * {@link GunDefinitions#ALL}. {@code GunItems} registers the item from it, {@code GunRecipes} registers its rows and
 * hooks, {@code GunsModule} registers its scenarios, and the table-driven tests walk the list. The client half (model
 * and render profile) is {@code com.gregguns.client.render.profile.ClientGunDefinition}, listed in
 * {@code ClientGunDefinitions.ALL}.
 *
 * <p>
 * No Minecraft types, so unit tests can read the whole list. The hooks and the scenario registration are lambdas that
 * call into Minecraft- or GT-facing classes from their bodies, which keeps those classes from loading until the hook
 * runs. Both sides.
 *
 * <p>
 * Sound events follow {@link GunSoundEvents}: {@code guns.<name>.fire}, {@code guns.<name>.reload} and, for a gun that
 * works a bolt or pump after each shot, {@code guns.<name>.rechamber}. They are derived from the name, so a new gun
 * adds no constant there, only its {@code sounds.json} entries.
 */
// Spec: docs/techguns-spec/02-guns/gun-lifecycle.md; docs/gregguns-spec/02-guns/index.md
public final class GunDefinition {

    private final String name;
    private final GunStats stats;
    private final Tier tier;
    private final DamageType damageType;
    private final boolean rechambers;
    private final int camoCount;
    private final List<ProductionTable.Row> rows;
    private final Runnable recipes;
    private final Runnable recycling;
    private final Consumer<ScenarioRegistry> scenarios;

    private GunDefinition(Builder b) {
        this.name = b.name;
        this.stats = b.stats;
        this.tier = Objects.requireNonNull(b.tier, "tier of " + b.name);
        this.damageType = Objects.requireNonNull(b.damageType, "damageType of " + b.name);
        this.rechambers = b.rechambers;
        this.camoCount = b.camoCount;
        this.rows = Collections.unmodifiableList(new ArrayList<>(b.rows));
        this.recipes = b.recipes;
        this.recycling = b.recycling;
        this.scenarios = b.scenarios;
    }

    /**
     * @param name  the item's registry name without the mod id, also the sound prefix and the texture name, for
     *              example {@code "bolt_action_rifle"}
     * @param stats the gun's stats
     * @return a builder; {@code tier} and {@code damageType} are required
     */
    public static Builder of(String name, GunStats stats) {
        return new Builder(name, stats);
    }

    /** @return the registry name without the mod id */
    public String name() {
        return name;
    }

    /** @return the gun's stats */
    public GunStats stats() {
        return stats;
    }

    /** @return the GregTech tier the gun belongs to */
    public Tier tier() {
        return tier;
    }

    /** @return the damage type of its projectiles, for the tooltip */
    public DamageType damageType() {
        return damageType;
    }

    /** @return true if the gun plays a second sound, {@link #rechamberEvent()}, with every shot */
    public boolean rechambers() {
        return rechambers;
    }

    /**
     * @return how many camo skins the gun has, the default included; 1 means the Camo Bench has nothing to cycle. The
     *         model needs one texture per skin ({@code <name>.png}, {@code <name>_camo1.png}, ...), which a test
     *         checks against the files.
     */
    public int camoCount() {
        return camoCount;
    }

    /** @return the unprefixed sound event of one shot, {@code guns.<name>.fire} */
    public String fireEvent() {
        return GunSoundEvents.gun(name, GunSoundEvents.FIRE);
    }

    /** @return the unprefixed sound event of a reload, {@code guns.<name>.reload} */
    public String reloadEvent() {
        return GunSoundEvents.gun(name, GunSoundEvents.RELOAD);
    }

    /** @return the unprefixed rechamber sound event, or null if the gun does not rechamber */
    public String rechamberEvent() {
        return rechambers ? GunSoundEvents.gun(name, GunSoundEvents.RECHAMBER) : null;
    }

    /** @return the final-assembly machine rows (the Assembler), empty for a gun that is only crafted by hand */
    public List<ProductionTable.Row> rows() {
        return rows;
    }

    /** @return the first row, or null if the gun has none */
    public ProductionTable.Row assembly() {
        return rows.isEmpty() ? null : rows.get(0);
    }

    /** @return the hook that registers crafting-table recipes, or null. Run on GT's before-postload hook. */
    public Runnable recipes() {
        return recipes;
    }

    /** @return the hook that attaches GT recycling data, or null. Run on GT's before-postload hook. */
    public Runnable recycling() {
        return recycling;
    }

    /**
     * Registers the gun's in-game scenarios (dev environment only).
     *
     * @param registry where to add them
     */
    public void registerScenarios(ScenarioRegistry registry) {
        if (scenarios != null) {
            scenarios.accept(registry);
        }
    }

    /** Fluent builder for {@link GunDefinition}. */
    public static final class Builder {

        private final String name;
        private final GunStats stats;
        private Tier tier;
        private DamageType damageType;
        private boolean rechambers;
        private int camoCount = 1;
        private final List<ProductionTable.Row> rows = new ArrayList<>();
        private Runnable recipes;
        private Runnable recycling;
        private Consumer<ScenarioRegistry> scenarios;

        private Builder(String name, GunStats stats) {
            this.name = Objects.requireNonNull(name, "name");
            this.stats = Objects.requireNonNull(stats, "stats");
        }

        /**
         * @param tier the GregTech tier
         * @return this
         */
        public Builder tier(Tier tier) {
            this.tier = tier;
            return this;
        }

        /**
         * @param damageType the damage type of the gun's projectiles
         * @return this
         */
        public Builder damageType(DamageType damageType) {
            this.damageType = damageType;
            return this;
        }

        /**
         * Marks a gun that plays {@code guns.<name>.rechamber} with every shot.
         *
         * @return this
         */
        public Builder rechambers() {
            this.rechambers = true;
            return this;
        }

        /**
         * @param count camo skins including the default; at least 1
         * @return this
         */
        public Builder camos(int count) {
            if (count < 1) {
                throw new IllegalArgumentException("a gun has at least its default camo: " + count);
            }
            this.camoCount = count;
            return this;
        }

        /**
         * @param row a final-assembly row whose output is this gun
         * @return this
         */
        public Builder row(ProductionTable.Row row) {
            rows.add(row);
            return this;
        }

        /**
         * @param hook registers the gun's crafting-table recipes
         * @return this
         */
        public Builder recipes(Runnable hook) {
            this.recipes = hook;
            return this;
        }

        /**
         * @param hook attaches the gun's GT {@code ItemData}
         * @return this
         */
        public Builder recycling(Runnable hook) {
            this.recycling = hook;
            return this;
        }

        /**
         * @param registration registers the gun's scenarios; call a {@code registerAll(registry)} from a lambda body
         * @return this
         */
        public Builder scenarios(Consumer<ScenarioRegistry> registration) {
            this.scenarios = registration;
            return this;
        }

        /** @return the definition */
        public GunDefinition build() {
            return new GunDefinition(this);
        }
    }
}
