package com.gregguns.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.gregguns.api.Tier;
import com.gregguns.armor.logic.ArmorCatalog;
import com.gregguns.armor.logic.ArmorSlot;

/** The T1 armor recipes against docs/gregguns-spec/05-armor/recipes.md and the D1 gate rule. */
class ArmorTableTest {

    @Test
    void combatRowsMatchTheSheet() {
        // Soldier's Helmet/Vest/Pants/Boots: plate.Steel x3/3/3/2, Heavy Cloth x2/5/4/2, circuit 1-4, 10/16/14/10 s.
        int[] steel = { 3, 3, 3, 2 };
        int[] cloth = { 2, 5, 4, 2 };
        int[] seconds = { 10, 16, 14, 10 };
        assertEquals(4, ArmorTable.COMBAT.size());
        for (int i = 0; i < 4; i++) {
            ProductionTable.Row row = ArmorTable.COMBAT.get(i);
            assertEquals(ArmorCatalog.T1_COMBAT.pieceName(ArmorSlot.values()[i]), row.output());
            assertEquals(Tier.LV, row.tier());
            assertEquals(ProductionTable.Target.ASSEMBLER, row.target());
            assertEquals(i + 1, row.circuit());
            assertEquals(seconds[i], row.seconds());
            assertEquals(
                2,
                row.inputs()
                    .size());
            assertEquals(
                "plate",
                row.inputs()
                    .get(0)
                    .prefix());
            assertEquals(
                "Steel",
                row.inputs()
                    .get(0)
                    .material());
            assertEquals(
                steel[i],
                row.inputs()
                    .get(0)
                    .count());
            assertEquals(
                "part_heavy_cloth",
                row.inputs()
                    .get(1)
                    .itemName());
            assertEquals(
                cloth[i],
                row.inputs()
                    .get(1)
                    .count());
        }
    }

    @Test
    void everyCombatRowMeetsTheGateRule() {
        Map<String, ProductionTable.Row> byOutput = new HashMap<>();
        for (ProductionTable.Row row : ArmorTable.COMBAT) {
            byOutput.put(row.output(), row);
        }
        for (ProductionTable.Row row : ArmorTable.COMBAT) {
            assertTrue(GateRule.satisfied(row, byOutput, ProductionTable.GATE_EXEMPT), row.output());
        }
    }

    @Test
    void handRecipesCarryTheSteamGateMaterial() {
        // GGS-05-30 and 36: Scout has plate.Bronze, Miner plate.CastIron (GT's WroughtIron alias) (both Steam-tier gate
        // metals, 0001).
        for (ArmorTable.HandRow row : ArmorTable.SCOUT) {
            assertTrue(
                row.keys()
                    .containsValue(ArmorTable.PLATE_BRONZE),
                row.id());
        }
        for (ArmorTable.HandRow row : ArmorTable.MINER) {
            assertTrue(
                row.keys()
                    .containsValue(ArmorTable.PLATE_CAST_IRON)
                    || row.keys()
                        .containsValue(ArmorTable.NUGGET_CAST_IRON),
                row.id());
            assertEquals(Tier.STEAM, row.tier());
        }
        assertEquals("Bronze", GateRule.gateMaterial(Tier.STEAM));
    }

    @Test
    void handGridsAreTheSheetGrids() {
        assertEquals(
            java.util.Arrays.asList("ccc", "cBc"),
            ArmorTable.SCOUT.get(0)
                .pattern());
        assertEquals(
            java.util.Arrays.asList("cBc", "ccc", "ccc"),
            ArmorTable.SCOUT.get(1)
                .pattern());
        assertEquals(
            java.util.Arrays.asList("ccc", "cBc", "c c"),
            ArmorTable.SCOUT.get(2)
                .pattern());
        assertEquals(
            java.util.Arrays.asList("cBc", "c c"),
            ArmorTable.SCOUT.get(3)
                .pattern());
        assertEquals(
            java.util.Arrays.asList("iyi", "c c"),
            ArmorTable.MINER.get(0)
                .pattern());
        assertEquals(
            java.util.Arrays.asList("c c", "ccc", "ici"),
            ArmorTable.MINER.get(1)
                .pattern());
        assertEquals(
            java.util.Arrays.asList("ici", "c c", "c c"),
            ArmorTable.MINER.get(2)
                .pattern());
        assertEquals(
            java.util.Arrays.asList("c c", "n n"),
            ArmorTable.MINER.get(3)
                .pattern());
    }

    @Test
    void handGridsAreWellFormed() {
        for (ArmorTable.HandRow row : ArmorTable.HAND) {
            assertTrue(
                row.pattern()
                    .size() <= 3,
                row.id());
            Set<Character> used = new HashSet<>();
            int width = row.pattern()
                .get(0)
                .length();
            for (String line : row.pattern()) {
                assertEquals(width, line.length(), row.id() + " is not rectangular");
                assertTrue(width <= 3, row.id());
                for (char c : line.toCharArray()) {
                    if (c != ' ') {
                        used.add(c);
                    }
                }
            }
            assertEquals(
                used,
                row.keys()
                    .keySet(),
                row.id() + " keys do not match the grid symbols");
        }
    }

    @Test
    void noRecipeUsesATechgunsMaterial() {
        // GGS-05-31
        for (ArmorTable.HandRow row : ArmorTable.HAND) {
            for (String ingredient : row.keys()
                .values()) {
                String lower = ingredient.toLowerCase();
                assertFalse(lower.contains("obsidiansteel") || lower.contains("carbon") || lower.contains("techguns"));
                assertTrue(ingredient.startsWith("ore:") || ingredient.startsWith("mod:"), ingredient);
            }
        }
    }

    @Test
    void everyPieceOfEverySetHasExactlyOneRecipe() {
        Set<String> made = new HashSet<>();
        for (ArmorTable.HandRow row : ArmorTable.HAND) {
            assertTrue(made.add(row.output()), row.id());
        }
        for (ProductionTable.Row row : ArmorTable.MACHINE) {
            assertTrue(made.add(row.output()), row.id());
        }
        int pieces = 0;
        for (com.gregguns.armor.logic.SetDef set : ArmorCatalog.ALL) {
            for (ArmorSlot slot : set.pieces()
                .keySet()) {
                assertTrue(made.contains(set.pieceName(slot)), set.pieceName(slot));
                pieces++;
            }
        }
        // 12 T1 pieces + 4 + 4 + 4 + 1 + 4 + 4 + 4 = 37 pieces, and the one extra output is Steam Armor Plating.
        assertEquals(37, pieces);
        assertEquals(pieces + 1, made.size());
    }

    @Test
    void rowIdsAreUnique() {
        Set<String> ids = new HashSet<>();
        for (ArmorTable.HandRow row : ArmorTable.HAND) {
            assertTrue(ids.add(row.id()));
        }
        for (ProductionTable.Row row : ArmorTable.MACHINE) {
            assertTrue(ids.add(row.id()), row.id());
        }
    }
}
