package com.gregguns.machine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.ToLongFunction;

import com.gregguns.api.Tier;
import com.gregguns.item.ammo.MagazineType;
import com.gregguns.item.ammo.def.AmmoDefinition;
import com.gregguns.item.ammo.def.AmmoDefinitions;
import com.gregguns.recipe.ProductionTable;

/**
 * The Ammunition Works' filling bench as plain logic: which loaded-magazine recipes it fills, how many runs one cycle
 * does, how long a cycle takes, what it costs, and the simulate-then-commit transfer between three item stores (the
 * station's Output Buses, the bench's Input Buses and its Output Buses). No Minecraft types, so the unit tests can pin
 * every number; {@link MTEAmmoWorks} supplies the real stores. Both sides.
 *
 * <p>
 * The bench invents no recipes. Each fill recipe is the existing {@code gregguns.ammoPress} row that makes a loaded
 * magazine (RC-44 for the 9mm pistol magazine): {@code N} empty magazines plus {@code N x rounds} loose rounds become
 * {@code N} loaded magazines in {@code ticks} ticks at {@code eut} EU/t. One "run" is one such recipe, so the Works'
 * parallel count caps the runs per cycle exactly as it caps the normal recipes.
 *
 * <p>
 * Usage: {@code AmmoWorksFill.plan(recipe, parallels, hatchTier, belt, maxEut, empties, rounds, out)}, pay
 * {@link Plan#eutPerTick} for {@link Plan#ticks} ticks, then {@link #commit}.
 */
// Spec: docs/gregguns-spec/08-machines-production/multiblocks.md (Ammunition Works: Filling bench, GGS-08-90 to
// GGS-08-95), docs/gregguns-spec/08-machines-production/ammo-press.md (RC-44 and its siblings)
public final class AmmoWorksFill {

    /** Ticks the bench waits before it looks again after a cycle found nothing to do. */
    public static final int IDLE_RETRY_TICKS = 20;

    private AmmoWorksFill() {}

    /** One fill recipe: a loaded-magazine row of the ammo press, reduced to what the bench needs. */
    public static final class Recipe {

        /** The press row's sheet id, for example {@code "RC-44"}. */
        public final String id;
        /** Registry name of the loaded magazine (the output). */
        public final String loaded;
        /** Registry name of the empty magazine (an input). */
        public final String empty;
        /** Registry name of the loose round (an input). */
        public final String round;
        /** Loaded magazines (and empty magazines) per run. */
        public final int magazinesPerRun;
        /** Loose rounds per run. */
        public final int roundsPerRun;
        /** The row's registered duration in ticks, before overclock and belt. */
        public final int ticks;
        /** The row's EU/t for one run. */
        public final long eut;
        /** The row's tier as GT numbers voltage (LV is 1). */
        public final int tierIndex;

        Recipe(String id, String loaded, String empty, String round, int magazinesPerRun, int roundsPerRun, int ticks,
            long eut, int tierIndex) {
            this.id = id;
            this.loaded = loaded;
            this.empty = empty;
            this.round = round;
            this.magazinesPerRun = magazinesPerRun;
            this.roundsPerRun = roundsPerRun;
            this.ticks = ticks;
            this.eut = eut;
            this.tierIndex = tierIndex;
        }
    }

    /**
     * Reads the fill recipes from the ammo definitions: every ammo press row whose output is a loaded magazine and
     * whose inputs are exactly that magazine's empty item and its loose round.
     *
     * @param eutFor the recipe EU/t of a tier (the registered value, {@code ProductionRecipes.eutFor} at runtime)
     * @return the recipes in definition order; never null
     */
    public static List<Recipe> recipes(ToLongFunction<Tier> eutFor) {
        List<Recipe> out = new ArrayList<>();
        for (AmmoDefinition def : AmmoDefinitions.ALL) {
            for (MagazineType mag : def.magazines()) {
                for (ProductionTable.Row row : def.rows()) {
                    Recipe recipe = derive(row, mag, eutFor);
                    if (recipe != null) {
                        out.add(recipe);
                    }
                }
            }
        }
        return Collections.unmodifiableList(out);
    }

    /**
     * @param row    an ammo press row
     * @param mag    a magazine type
     * @param eutFor the recipe EU/t of a tier
     * @return the fill recipe if the row loads that magazine from its empty item and its round; otherwise null
     */
    static Recipe derive(ProductionTable.Row row, MagazineType mag, ToLongFunction<Tier> eutFor) {
        if (row.target() != ProductionTable.Target.AMMO_PRESS || !row.output()
            .equals(mag.loadedItemName())
            || row.inputs()
                .size() != 2) {
            return null;
        }
        String roundName = mag.ammo()
            .roundItemName();
        int empties = 0;
        int rounds = 0;
        for (ProductionTable.Input in : row.inputs()) {
            if (!in.isGregGuns()) {
                return null;
            }
            if (in.itemName()
                .equals(mag.emptyItemName())) {
                empties = in.count();
            } else if (in.itemName()
                .equals(roundName)) {
                    rounds = in.count();
                }
        }
        if (empties != row.outputCount() || rounds < 1) {
            return null;
        }
        return new Recipe(
            row.id(),
            mag.loadedItemName(),
            mag.emptyItemName(),
            roundName,
            row.outputCount(),
            rounds,
            row.ticks(),
            eutFor.applyAsLong(row.tier()),
            row.tier()
                .voltageIndex());
    }

    /** Where items come from and go to. Keys are registry names; amounts are item counts. */
    public interface Store {

        /** @return how many of the item the store holds */
        long count(String item);

        /** @return how many were actually removed, at most {@code amount} */
        long take(String item, long amount);

        /** @return whether the whole amount would fit right now (a simulation; nothing changes) */
        boolean fits(String item, long amount);

        /** Stores the whole amount; the caller has checked {@link #fits} just before. */
        void put(String item, long amount);
    }

    /** What one cycle will do: planned at the start, paid for while it runs, committed at its end. */
    public static final class Plan {

        /** The recipe. */
        public final Recipe recipe;
        /** Runs planned (recipes done at once). */
        public final int runs;
        /** Overclocks applied. */
        public final int overclocks;
        /** Cycle length in ticks. */
        public final int ticks;
        /** EU/t the whole cycle draws. */
        public final long eutPerTick;

        Plan(Recipe recipe, int runs, int overclocks, int ticks, long eutPerTick) {
            this.recipe = recipe;
            this.runs = runs;
            this.overclocks = overclocks;
            this.ticks = ticks;
            this.eutPerTick = eutPerTick;
        }
    }

    /**
     * Overclocks the Works applies to a fill recipe, as GT's standard overclock does: one per voltage tier of the
     * hatches above the recipe's tier (each halves the duration and quadruples the EU/t), never so many that the
     * duration drops under one tick.
     *
     * @param hatchTier  the Works' voltage tier (LV is 1)
     * @param recipeTier the recipe's tier (LV is 1)
     * @param baseTicks  the duration before overclocking, belt included
     * @return the overclock count, at least 0
     */
    public static int overclocks(int hatchTier, int recipeTier, double baseTicks) {
        int oc = Math.max(0, hatchTier - recipeTier);
        while (oc > 0 && baseTicks / (1L << oc) < 1.0) {
            oc--;
        }
        return oc;
    }

    /**
     * @param baseTicks  the duration before overclocking, belt included
     * @param overclocks the overclock count
     * @return the cycle length: {@code ceil(baseTicks / 2^overclocks)}, at least 1
     */
    public static int cycleTicks(double baseTicks, int overclocks) {
        return Math.max(1, (int) Math.ceil(baseTicks / (1L << overclocks)));
    }

    /**
     * Plans one cycle of one recipe.
     *
     * <p>
     * Runs are the least of the parallel cap, the empty magazines on hand divided by the magazines per run, the rounds
     * on hand divided by the rounds per run, the runs the power supply can carry ({@code maxEut / (eut x 4^oc)}), and
     * the runs whose output fits the bench's Output Buses.
     *
     * @param recipe    the recipe
     * @param parallels the Works' parallel count
     * @param hatchTier the Works' voltage tier (LV is 1)
     * @param belt      the belt's duration multiplier ({@link AmmoWorksShape#speedMultiplier})
     * @param maxEut    the EU/t the Energy Hatches can supply
     * @param empties   the bench Input Buses
     * @param rounds    the station Output Buses
     * @param out       the bench Output Buses
     * @return the plan, or null when not even one run is possible
     */
    public static Plan plan(Recipe recipe, int parallels, int hatchTier, double belt, long maxEut, Store empties,
        Store rounds, Store out) {
        double baseTicks = recipe.ticks * belt;
        int oc = overclocks(hatchTier, recipe.tierIndex, baseTicks);
        long eutPerRun = recipe.eut << (2 * oc);
        long runs = Math.min(parallels, empties.count(recipe.empty) / recipe.magazinesPerRun);
        runs = Math.min(runs, rounds.count(recipe.round) / recipe.roundsPerRun);
        runs = Math.min(runs, maxEut / Math.max(1, eutPerRun));
        if (runs <= 0) {
            return null;
        }
        int fitted = fittingRuns((int) runs, recipe, out);
        if (fitted <= 0) {
            return null;
        }
        return new Plan(recipe, fitted, oc, cycleTicks(baseTicks, oc), eutPerRun * fitted);
    }

    /**
     * Commits a cycle at its end. The stores may have changed since the plan, so the runs are worked out again, capped
     * by the plan; items are removed only after the output is known to fit, and whatever cannot be moved is given
     * back.
     *
     * @param plan    the finished cycle's plan
     * @param empties the bench Input Buses
     * @param rounds  the station Output Buses
     * @param out     the bench Output Buses
     * @return the runs done, 0 or more
     */
    public static int commit(Plan plan, Store empties, Store rounds, Store out) {
        Recipe r = plan.recipe;
        long runs = Math.min(plan.runs, empties.count(r.empty) / r.magazinesPerRun);
        runs = Math.min(runs, rounds.count(r.round) / r.roundsPerRun);
        int done = fittingRuns((int) Math.max(0, runs), r, out);
        if (done <= 0) {
            return 0;
        }
        long wantEmpties = (long) done * r.magazinesPerRun;
        long wantRounds = (long) done * r.roundsPerRun;
        long gotEmpties = empties.take(r.empty, wantEmpties);
        long gotRounds = rounds.take(r.round, wantRounds);
        if (gotEmpties < wantEmpties || gotRounds < wantRounds) {
            // A store gave less than it counted: put it all back and do nothing (cannot happen with honest stores).
            if (gotEmpties > 0) {
                empties.put(r.empty, gotEmpties);
            }
            if (gotRounds > 0) {
                rounds.put(r.round, gotRounds);
            }
            return 0;
        }
        out.put(r.loaded, wantEmpties);
        return done;
    }

    /** The most runs, up to {@code max}, whose magazines the output accepts. Binary search; fit is monotone. */
    private static int fittingRuns(int max, Recipe r, Store out) {
        int lo = 0;
        int hi = max;
        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            if (out.fits(r.loaded, (long) mid * r.magazinesPerRun)) {
                lo = mid;
            } else {
                hi = mid - 1;
            }
        }
        return lo;
    }
}
