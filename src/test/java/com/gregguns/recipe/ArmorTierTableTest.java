package com.gregguns.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.gregguns.api.Tier;
import com.gregguns.armor.logic.ArmorCatalog;
import com.gregguns.armor.logic.ArmorSlot;
import com.gregguns.armor.logic.SetDef;

/** The T2, T3 and steam armor recipes against docs/gregguns-spec/05-armor/recipes.md and the gate rule. */
class ArmorTierTableTest {

    private static int count(ProductionTable.Row row, String prefix, String material) {
        for (ProductionTable.Input in : row.inputs()) {
            if (!in.isGregGuns() && in.special() == null
                && prefix.equals(in.prefix())
                && material.equals(in.material())) {
                return in.count();
            }
        }
        return 0;
    }

    private static int countMod(ProductionTable.Row row, String name) {
        for (ProductionTable.Input in : row.inputs()) {
            if (in.isGregGuns() && name.equals(in.itemName())) {
                return in.count();
            }
        }
        return 0;
    }

    private static void assertSheetRows(List<ProductionTable.Row> rows, SetDef set, Tier tier, int[] seconds) {
        assertEquals(4, rows.size());
        for (int i = 0; i < 4; i++) {
            ProductionTable.Row row = rows.get(i);
            assertEquals(set.pieceName(ArmorSlot.values()[i]), row.output());
            assertEquals(tier, row.tier());
            assertEquals(ProductionTable.Target.ASSEMBLER, row.target());
            assertEquals(i + 1, row.circuit(), "circuit helmet 1 to boots 4");
            assertEquals(seconds[i], row.seconds());
        }
    }

    @Test
    void t2CombatRowsMatchTheSheet() {
        assertSheetRows(ArmorTable.T2_COMBAT, ArmorCatalog.T2_COMBAT, Tier.MV, new int[] { 10, 16, 14, 10 });
        int[] al = { 3, 6, 4, 2 };
        int[] hc = { 2, 2, 3, 2 };
        for (int i = 0; i < 4; i++) {
            ProductionTable.Row row = ArmorTable.T2_COMBAT.get(i);
            assertEquals(al[i], count(row, "plate", "Aluminium"));
            assertEquals(hc[i], countMod(row, "part_heavy_cloth"));
        }
    }

    @Test
    void t2CommandoRowsMatchTheSheet() {
        assertSheetRows(ArmorTable.T2_COMMANDO, ArmorCatalog.T2_COMMANDO, Tier.MV, new int[] { 12, 16, 14, 10 });
        int[] al = { 2, 4, 3, 2 };
        int[] rubber = { 3, 4, 4, 2 };
        for (int i = 0; i < 4; i++) {
            ProductionTable.Row row = ArmorTable.T2_COMMANDO.get(i);
            assertEquals(al[i], count(row, "plate", "Aluminium"));
            assertEquals(rubber[i], count(row, "plate", "Rubber"));
        }
        assertEquals(
            3,
            ArmorTable.T2_COMMANDO.get(0)
                .inputs()
                .size(),
            "the headgear also takes a glass pane");
    }

    @Test
    void riotRowsUpgradeTheMatchingCombatPieceAtAnyDamage() {
        assertSheetRows(ArmorTable.T2_RIOT, ArmorCatalog.T2_RIOT, Tier.MV, new int[] { 10, 16, 14, 10 });
        int[] lead = { 2, 3, 2, 2 };
        int[] leather = { 2, 4, 4, 2 };
        for (int i = 0; i < 4; i++) {
            ProductionTable.Row row = ArmorTable.T2_RIOT.get(i);
            ProductionTable.Input base = row.inputs()
                .get(0);
            assertEquals(ArmorCatalog.T2_COMBAT.pieceName(ArmorSlot.values()[i]), base.itemName());
            assertTrue(base.anyDamage(), "GGS-05-35: the Combat piece matches at any damage");
            assertEquals(1, base.count());
            assertEquals(lead[i], count(row, "plate", "Lead"));
            assertEquals(leather[i], countMod(row, "part_treated_leather"));
        }
    }

    @Test
    void beretRowMatchesTheSheet() {
        ProductionTable.Row row = ArmorTable.BERET;
        assertEquals("t2_beret", row.output());
        assertEquals(Tier.MV, row.tier());
        assertEquals(10, row.seconds());
        assertEquals(1, row.circuit());
        assertEquals(2, countMod(row, "part_heavy_cloth"));
        assertEquals(1, count(row, "ring", "Aluminium"));
    }

    @Test
    void t3CombatRowsMatchTheSheet() {
        assertSheetRows(ArmorTable.T3_COMBAT, ArmorCatalog.T3_COMBAT, Tier.EV, new int[] { 10, 16, 14, 10 });
        int[] epoxy = { 3, 6, 4, 2 };
        int[] ti = { 2, 2, 3, 2 };
        for (int i = 0; i < 4; i++) {
            ProductionTable.Row row = ArmorTable.T3_COMBAT.get(i);
            assertEquals(epoxy[i], count(row, "plate", "EpoxidFiberReinforced"));
            assertEquals(ti[i], count(row, "plate", "Titanium"));
        }
    }

    @Test
    void hevRowsMatchTheSheet() {
        assertSheetRows(ArmorTable.T3_HEV, ArmorCatalog.T3_HEV, Tier.EV, new int[] { 12, 16, 14, 10 });
        int[] epoxy = { 2, 3, 3, 2 };
        int[] ti = { 2, 4, 2, 2 };
        int[] circuits = { 1, 1, 2, 1 }; // boots: one circuit so they differ from the Combat boots
        for (int i = 0; i < 4; i++) {
            ProductionTable.Row row = ArmorTable.T3_HEV.get(i);
            assertEquals(epoxy[i], count(row, "plate", "EpoxidFiberReinforced"));
            assertEquals(ti[i], count(row, "plate", "Titanium"));
            assertEquals(circuits[i], count(row, "circuit", "Data"));
        }
    }

    @Test
    void steamRowsAndPlatingMatchTheSheet() {
        assertSheetRows(ArmorTable.STEAM, ArmorCatalog.STEAM, Tier.EV, new int[] { 10, 16, 14, 10 });
        int[] sap = { 5, 8, 7, 4 };
        for (int i = 0; i < 4; i++) {
            ProductionTable.Row row = ArmorTable.STEAM.get(i);
            assertEquals(
                1,
                row.inputs()
                    .size());
            assertEquals(sap[i], countMod(row, "part_steam_armor_plating"));
        }
        ProductionTable.Row plating = ArmorTable.STEAM_PLATING;
        assertEquals("RC-23", plating.id());
        assertEquals(Tier.EV, plating.tier());
        assertEquals(6, plating.seconds());
        assertEquals(2, count(plating, "plate", "Titanium"));
        assertEquals(1, count(plating, "plate", "Bronze"));
        assertEquals(1, count(plating, "plate", "Steel"));
    }

    @Test
    void everyMachineRowMeetsTheGateRuleAndRunsAtItsTier() {
        Map<String, ProductionTable.Row> byOutput = new HashMap<>();
        for (ProductionTable.Row row : ArmorTable.MACHINE) {
            byOutput.put(row.output(), row);
        }
        for (ProductionTable.Row row : ArmorTable.MACHINE) {
            assertTrue(GateRule.satisfied(row, byOutput, ProductionTable.GATE_EXEMPT), row.output());
            assertTrue(
                row.tier()
                    .compareTo(Tier.LV) >= 0,
                row.output());
        }
    }

    @Test
    void noRowUsesATechgunsMaterial() {
        // GGS-05-31
        for (ProductionTable.Row row : ArmorTable.MACHINE) {
            for (ProductionTable.Input in : row.inputs()) {
                String text = (in.isGregGuns() ? in.itemName() : in.prefix() + "." + in.material()).toLowerCase();
                assertFalse(text.contains("obsidian") || text.contains("carbon") || text.contains("techguns"), text);
            }
        }
    }

    @Test
    void tierMaterialFollowsTheConsumingItemTier() {
        // 0010 decision 1: Aluminium at MV, Titanium at EV.
        for (ProductionTable.Row row : ArmorTable.T2_COMBAT) {
            assertTrue(count(row, "plate", "Aluminium") > 0);
        }
        for (ProductionTable.Row row : ArmorTable.T3_COMBAT) {
            assertTrue(count(row, "plate", "Titanium") > 0);
        }
    }
}
