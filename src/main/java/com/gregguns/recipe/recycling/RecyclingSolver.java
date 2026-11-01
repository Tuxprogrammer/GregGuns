package com.gregguns.recipe.recycling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;

/**
 * Derives the material content of items from the recipes that make them, the way GT derives the recycling data of its
 * own crafted machines: the sum of every consumed input's material data, divided by the number of items the recipe
 * makes. Plain Java, no Minecraft or GT types, so it is unit tested; {@link RecyclingDerivation} feeds it the live
 * recipes and turns the result into GT {@code ItemData}.
 *
 * <p>
 * The rules, each GT's own:
 * <ul>
 * <li><b>Sum of the inputs.</b> GT's reversible crafting recipes and {@code addItemDataFromInputs} merge the
 * {@code ItemData} of every ingredient into one composition. Inputs with no data (circuits, whose prefix amount is
 * negative, programmed circuits, tools that stay in the grid) add nothing.</li>
 * <li><b>Per item, rounded down.</b> {@code setItemData} divides each material by the output stack size with integer
 * division.</li>
 * <li><b>The cheapest recipe.</b> GT gives its machine hulls the Assembler recipe's inputs, not the dearer crafting
 * recipe's, because recycling for more than the cheapest recipe costs is a duplication loop. Here every recipe of an
 * item is costed and the one with the least material total wins (ties: the first, in the order given).</li>
 * </ul>
 * One rule is GregGuns' own, passed in as {@code keep}: materials that must not come back (radioactive ones, GGS-06-46)
 * or that GT can return in no form are left out before costing.
 *
 * <p>
 * Items made from other items still being derived wait for them. When only a cycle is left, the one item whose
 * cheapest recipe with known inputs is the lowest of all settles first (as in a shortest-path search), and the search
 * starts again; when no recipe can be costed, unknown inputs count as empty, which can only lower a result.
 */
// Spec: docs/gregguns-spec/06-items/recycling.md (GGS-06-50, GGS-06-53)
// Source:
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTModHandler.java#L1154-L1160
// (a REVERSIBLE crafting recipe gives its output new ItemData(ingredient data...))
// Source:
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/objects/ItemData.java#L59-L128
// (the merge: amounts of the same material add up, only positive amounts count, the largest becomes the main material)
// Source:
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTOreDictUnificator.java#L385-L397
// (setItemData divides every amount by the output stack size)
// Source:
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/load/MTERecipeLoader.java#L2484-L2600
// (hull recycling comes from the Assembler inputs; the crafting recipe "would create an exploit loop")
public final class RecyclingSolver {

    private RecyclingSolver() {}

    /** One input of a recipe: either material data that is already known, or another item being derived. */
    public static final class Input {

        private final String target;
        private final Map<String, Long> known;
        private final int count;

        private Input(String target, Map<String, Long> known, int count) {
            this.target = target;
            this.known = known;
            this.count = count;
        }

        /**
         * @param composition material name to amount (GT units) of one input item; may be empty
         * @param count       how many of the item the recipe consumes
         * @return an input whose material content is known
         */
        public static Input known(Map<String, Long> composition, int count) {
            return new Input(null, new LinkedHashMap<>(composition), count);
        }

        /**
         * @param key   the key of another item being derived
         * @param count how many of it the recipe consumes
         * @return an input whose content comes from that item's own result
         */
        public static Input target(String key, int count) {
            return new Input(key, null, count);
        }

        /** @return the key of the item being derived, or null for a known input */
        public String target() {
            return target;
        }
    }

    /** One recipe that makes an item. */
    public static final class Recipe {

        private final String source;
        private final List<Input> inputs;
        private final int outputCount;

        /**
         * @param source      a readable name of the recipe, for the report (for example {@code assembler})
         * @param inputs      the consumed inputs
         * @param outputCount how many items one run makes; at least 1
         */
        public Recipe(String source, List<Input> inputs, int outputCount) {
            if (outputCount < 1) {
                throw new IllegalArgumentException("output count must be at least 1: " + outputCount);
            }
            this.source = source;
            this.inputs = new ArrayList<>(inputs);
            this.outputCount = outputCount;
        }

        /** @return the readable name of the recipe */
        public String source() {
            return source;
        }
    }

    /** The derived content of one item and the recipe it came from. */
    public static final class Derived {

        private final Map<String, Long> composition;
        private final String source;

        Derived(Map<String, Long> composition, String source) {
            this.composition = Collections.unmodifiableMap(composition);
            this.source = source;
        }

        /** @return material name to amount per item, largest first (ties by name); empty when nothing is recovered */
        public Map<String, Long> composition() {
            return composition;
        }

        /** @return the readable name of the recipe the content came from */
        public String source() {
            return source;
        }

        /** @return the sum of all amounts */
        public long total() {
            long total = 0;
            for (long amount : composition.values()) {
                total += amount;
            }
            return total;
        }
    }

    /**
     * @param recipes every item to derive, mapped to the recipes that make it (an item with no recipe may be listed
     *                with an empty list; it is left out of the result)
     * @param keep    whether a material may come back from recycling
     * @return the derived content of each item that has at least one recipe, in key order
     */
    public static Map<String, Derived> solve(Map<String, List<Recipe>> recipes, Predicate<String> keep) {
        Map<String, Derived> done = new TreeMap<>();
        Map<String, List<Recipe>> pending = new TreeMap<>();
        for (Map.Entry<String, List<Recipe>> entry : recipes.entrySet()) {
            if (!entry.getValue()
                .isEmpty()) {
                pending.put(entry.getKey(), entry.getValue());
            }
        }
        while (!pending.isEmpty()) {
            // 1. Items whose every recipe can be costed: their cheapest recipe is final.
            if (settle(pending, done, keep, true, false)) {
                continue;
            }
            // 2. A cycle: settle only the cheapest item that has a recipe with known inputs, then look again, so an
            // item
            // that converts into another settles at the lower of the two.
            if (settleCheapest(pending, done, keep)) {
                continue;
            }
            // 3. Nothing can be costed: unknown inputs count as empty.
            settle(pending, done, keep, false, true);
        }
        return done;
    }

    /** @return whether anything was settled */
    private static boolean settle(Map<String, List<Recipe>> pending, Map<String, Derived> done, Predicate<String> keep,
        boolean requireAll, boolean unknownAsEmpty) {
        Map<String, Derived> settled = new TreeMap<>();
        for (Map.Entry<String, List<Recipe>> entry : pending.entrySet()) {
            Derived best = null;
            boolean all = true;
            for (Recipe recipe : entry.getValue()) {
                Derived cost = cost(recipe, done, pending, keep, unknownAsEmpty);
                if (cost == null) {
                    all = false;
                    continue;
                }
                if (best == null || cost.total() < best.total()) {
                    best = cost;
                }
            }
            if (best != null && (all || !requireAll)) {
                settled.put(entry.getKey(), best);
            }
        }
        for (Map.Entry<String, Derived> entry : settled.entrySet()) {
            pending.remove(entry.getKey());
            done.put(entry.getKey(), entry.getValue());
        }
        return !settled.isEmpty();
    }

    /** @return whether an item was settled: the one whose cheapest costable recipe is the lowest of all */
    private static boolean settleCheapest(Map<String, List<Recipe>> pending, Map<String, Derived> done,
        Predicate<String> keep) {
        String bestKey = null;
        Derived best = null;
        for (Map.Entry<String, List<Recipe>> entry : pending.entrySet()) {
            for (Recipe recipe : entry.getValue()) {
                Derived cost = cost(recipe, done, pending, keep, false);
                if (cost != null && (best == null || cost.total() < best.total())) {
                    best = cost;
                    bestKey = entry.getKey();
                }
            }
        }
        if (bestKey == null) {
            return false;
        }
        pending.remove(bestKey);
        done.put(bestKey, best);
        return true;
    }

    /** @return the per-item content of one recipe, or null when an input is still being derived */
    private static Derived cost(Recipe recipe, Map<String, Derived> done, Map<String, List<Recipe>> pending,
        Predicate<String> keep, boolean unknownAsEmpty) {
        Map<String, Long> sum = new TreeMap<>();
        for (Input input : recipe.inputs) {
            Map<String, Long> unit;
            if (input.target == null) {
                unit = input.known;
            } else {
                Derived derived = done.get(input.target);
                // An item no recipe makes never settles, so it counts as empty at once.
                if (derived == null && !unknownAsEmpty && pending.containsKey(input.target)) {
                    return null;
                }
                unit = derived == null ? Collections.<String, Long>emptyMap() : derived.composition();
            }
            for (Map.Entry<String, Long> material : unit.entrySet()) {
                if (material.getValue() > 0 && keep.test(material.getKey())) {
                    sum.merge(material.getKey(), material.getValue() * input.count, Long::sum);
                }
            }
        }
        List<Map.Entry<String, Long>> perItem = new ArrayList<>();
        for (Map.Entry<String, Long> material : sum.entrySet()) {
            long amount = material.getValue() / recipe.outputCount;
            if (amount > 0) {
                perItem.add(new java.util.AbstractMap.SimpleEntry<>(material.getKey(), amount));
            }
        }
        // Largest first, as GT's ItemData merge orders them; names break ties so the result is stable.
        perItem.sort(
            (a, b) -> a.getValue()
                .equals(b.getValue())
                    ? a.getKey()
                        .compareTo(b.getKey())
                    : Long.compare(b.getValue(), a.getValue()));
        Map<String, Long> composition = new LinkedHashMap<>();
        for (Map.Entry<String, Long> material : perItem) {
            composition.put(material.getKey(), material.getValue());
        }
        return new Derived(composition, recipe.source);
    }
}
