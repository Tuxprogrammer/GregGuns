package com.gregguns.recipe.recycling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

import com.gregguns.recipe.recycling.RecyclingSolver.Derived;
import com.gregguns.recipe.recycling.RecyclingSolver.Input;
import com.gregguns.recipe.recycling.RecyclingSolver.Recipe;

/**
 * GT's recycling rules as {@link RecyclingSolver} applies them: the inputs' sum, per item rounded down, the cheapest
 * recipe, the GregGuns material filter, and items built from items still being derived. Amounts are stand-in units
 * (a plate is 100).
 */
class RecyclingSolverTest {

    private static final long PLATE = 100;

    private static Map<String, Long> of(Object... pairs) {
        Map<String, Long> out = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            out.put((String) pairs[i], ((Number) pairs[i + 1]).longValue());
        }
        return out;
    }

    private static Recipe recipe(String source, int outputs, Input... inputs) {
        return new Recipe(source, Arrays.asList(inputs), outputs);
    }

    private static Map<String, Derived> solve(Map<String, List<Recipe>> recipes) {
        return RecyclingSolver.solve(recipes, material -> !material.equals("Uranium235"));
    }

    private static Map<String, List<Recipe>> one(String key, Recipe... recipes) {
        Map<String, List<Recipe>> out = new TreeMap<>();
        out.put(key, new ArrayList<>(Arrays.asList(recipes)));
        return out;
    }

    @Test
    void sumsEveryInputTimesItsCount() {
        Derived d = solve(
            one(
                "gun",
                recipe("crafting", 1, Input.known(of("Steel", PLATE), 3), Input.known(of("Rubber", PLATE / 2), 2))))
                    .get("gun");
        assertEquals(of("Steel", 300L, "Rubber", 100L), d.composition());
        assertEquals("crafting", d.source());
        assertEquals(400, d.total());
    }

    @Test
    void mergesTheSameMaterialFromSeveralInputs() {
        // A plate and a screw of steel, and an input carrying steel as a byproduct, merge into one steel figure.
        Derived d = solve(
            one(
                "part",
                recipe(
                    "assembler",
                    1,
                    Input.known(of("Steel", PLATE), 1),
                    Input.known(of("Steel", 12), 2),
                    Input.known(of("Aluminium", PLATE, "Steel", 6), 1)))).get("part");
        assertEquals(of("Steel", 130L, "Aluminium", 100L), d.composition());
    }

    @Test
    void dividesPerItemRoundingDownLikeGtSetItemData() {
        // Six plates make four casings: 600 / 4 = 150 each. 7 / 4 rounds down to 1; 3 / 4 rounds down to nothing.
        Derived d = solve(
            one(
                "casing",
                recipe(
                    "assembler",
                    4,
                    Input.known(of("StainlessSteel", PLATE), 6),
                    Input.known(of("Tin", 7), 1),
                    Input.known(of("Lead", 3), 1)))).get("casing");
        assertEquals(of("StainlessSteel", 150L, "Tin", 1L), d.composition());
    }

    @Test
    void ordersLargestFirstThenByName() {
        Derived d = solve(
            one(
                "x",
                recipe(
                    "crafting",
                    1,
                    Input.known(of("Zinc", 50), 1),
                    Input.known(of("Copper", 50), 1),
                    Input.known(of("Iron", 200), 1)))).get("x");
        assertEquals(
            Arrays.asList("Iron", "Copper", "Zinc"),
            new ArrayList<>(
                d.composition()
                    .keySet()));
    }

    @Test
    void picksTheCheapestRecipeSoRecyclingNeverPaysMoreThanACraft() {
        // Like GT's hulls: the dearer crafting recipe must not set what the Assembler version recycles into.
        Derived d = solve(
            one(
                "machine",
                recipe("crafting", 1, Input.known(of("Steel", PLATE), 8)),
                recipe("assembler", 1, Input.known(of("Steel", PLATE), 5)))).get("machine");
        assertEquals("assembler", d.source());
        assertEquals(of("Steel", 500L), d.composition());
    }

    @Test
    void tiesKeepTheFirstRecipe() {
        Derived d = solve(
            one(
                "x",
                recipe("first", 1, Input.known(of("Steel", PLATE), 1)),
                recipe("second", 1, Input.known(of("Iron", PLATE), 1)))).get("x");
        assertEquals("first", d.source());
    }

    @Test
    void inputsWithoutMaterialAddNothing() {
        // Circuits (negative prefix amount), programmed circuits and tools come in as empty or non-positive data.
        Derived d = solve(
            one(
                "x",
                recipe(
                    "assembler",
                    1,
                    Input.known(of("Steel", PLATE), 2),
                    Input.known(of("Advanced", -1), 2),
                    Input.known(Collections.<String, Long>emptyMap(), 1)))).get("x");
        assertEquals(of("Steel", 200L), d.composition());
    }

    @Test
    void filteredMaterialsNeverComeBack() {
        Derived d = solve(
            one(
                "cell",
                recipe("assembler", 1, Input.known(of("Uranium235", PLATE), 1), Input.known(of("Lead", PLATE), 1))))
                    .get("cell");
        assertEquals(of("Lead", 100L), d.composition());
    }

    @Test
    void aRecipeWithNothingRecoverableGivesAnEmptyResult() {
        Derived d = solve(one("chip", recipe("assembler", 1, Input.known(of("Data", -1), 4)))).get("chip");
        assertTrue(
            d.composition()
                .isEmpty());
        assertEquals(0, d.total());
    }

    @Test
    void anEmptyRecipeCountsAsTheCheapest() {
        // Crafting it for nothing and recycling it must not return the other recipe's metal.
        Derived d = solve(
            one(
                "x",
                recipe("assembler", 1, Input.known(of("Steel", PLATE), 2)),
                recipe("crafting", 1, Input.known(of("Glass", 0), 1)))).get("x");
        assertTrue(
            d.composition()
                .isEmpty());
    }

    @Test
    void itemsBuiltFromDerivedItemsUseTheirResult() {
        Map<String, List<Recipe>> recipes = new TreeMap<>();
        recipes.put(
            "a_gun",
            Collections.singletonList(
                recipe("crafting", 1, Input.target("z_receiver", 1), Input.known(of("Steel", PLATE), 2))));
        recipes.put("z_receiver", Collections.singletonList(recipe("crafting", 2, Input.known(of("Iron", PLATE), 4))));
        Map<String, Derived> out = solve(recipes);
        assertEquals(
            of("Iron", 200L),
            out.get("z_receiver")
                .composition());
        assertEquals(
            of("Steel", 200L, "Iron", 200L),
            out.get("a_gun")
                .composition());
    }

    @Test
    void waitsForEveryRecipeBeforeChoosing() {
        // The cheap recipe needs another derived item; the item must not settle early on the dear one.
        Map<String, List<Recipe>> recipes = new TreeMap<>();
        recipes.put(
            "a",
            Arrays.asList(
                recipe("dear", 1, Input.known(of("Steel", PLATE), 9)),
                recipe("cheap", 1, Input.target("b", 1))));
        recipes.put("b", Collections.singletonList(recipe("crafting", 1, Input.target("c", 1))));
        recipes.put("c", Collections.singletonList(recipe("crafting", 1, Input.known(of("Steel", PLATE), 1))));
        assertEquals(
            "cheap",
            solve(recipes).get("a")
                .source());
        assertEquals(
            of("Steel", 100L),
            solve(recipes).get("a")
                .composition());
    }

    @Test
    void aCycleSettlesFromTheRecipesThatCanBeCosted() {
        // a <-> b convert into each other; each also has a plain recipe.
        Map<String, List<Recipe>> recipes = new TreeMap<>();
        recipes.put(
            "a",
            Arrays.asList(
                recipe("plain", 1, Input.known(of("Steel", PLATE), 3)),
                recipe("from_b", 1, Input.target("b", 1))));
        recipes.put(
            "b",
            Arrays.asList(
                recipe("plain", 1, Input.known(of("Steel", PLATE), 2)),
                recipe("from_a", 1, Input.target("a", 1))));
        Map<String, Derived> out = solve(recipes);
        assertEquals(
            of("Steel", 200L),
            out.get("b")
                .composition());
        // b settles first (cheapest); a then costs the lower of its plain recipe and b, so a b -> a craft gains
        // nothing.
        assertEquals(
            of("Steel", 200L),
            out.get("a")
                .composition());
        assertEquals(
            "from_b",
            out.get("a")
                .source());
    }

    @Test
    void aClosedCycleCountsTheUnknownInputAsEmpty() {
        // An upgrade that consumes the item itself and nothing else that settles.
        Map<String, List<Recipe>> recipes = one(
            "x",
            recipe("upgrade", 1, Input.target("x", 1), Input.known(of("Gold", 50), 1)));
        assertEquals(
            of("Gold", 50L),
            solve(recipes).get("x")
                .composition());
    }

    @Test
    void anInputThatNoRecipeMakesCountsAsEmpty() {
        Map<String, List<Recipe>> recipes = new TreeMap<>();
        recipes.put("loot", new ArrayList<>());
        recipes.put(
            "x",
            Collections
                .singletonList(recipe("crafting", 1, Input.target("loot", 1), Input.known(of("Steel", PLATE), 1))));
        Map<String, Derived> out = solve(recipes);
        assertFalse(out.containsKey("loot"), "an item with no recipe has no result");
        assertEquals(
            of("Steel", 100L),
            out.get("x")
                .composition());
        assertEquals(
            "crafting",
            out.get("x")
                .source());
    }

    @Test
    void outputCountMustBePositive() {
        assertThrows(IllegalArgumentException.class, () -> new Recipe("x", new ArrayList<>(), 0));
    }
}
