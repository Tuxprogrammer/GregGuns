package com.gregguns.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.gregguns.api.Tier;

class GunTableTest {

    @Test
    void pistolRowMatchesTheSheet() {
        // docs/gregguns-spec/02-guns/recipes.md, LV table, 9mm Pistol: 15 s, cfg 1, on the Assembler at LV.
        ProductionTable.Row pistol = GunTable.PISTOL;
        assertEquals("pistol_9mm", pistol.output());
        assertEquals(1, pistol.outputCount());
        assertEquals(Tier.LV, pistol.tier());
        assertEquals(ProductionTable.Target.ASSEMBLER, pistol.target());
        assertEquals(15, pistol.seconds());
        assertEquals(1, pistol.circuit());
        assertEquals(
            6,
            pistol.inputs()
                .size());
    }

    @Test
    void pistolInputsAreTheSheetsParts() {
        ProductionTable.Row pistol = GunTable.PISTOL;
        assertEquals(
            "part_barrel_hardened",
            pistol.inputs()
                .get(0)
                .itemName());
        assertEquals(
            "part_mechanical_2",
            pistol.inputs()
                .get(1)
                .itemName());
        assertGt(
            pistol.inputs()
                .get(2),
            "plate",
            "Steel",
            2);
        assertGt(
            pistol.inputs()
                .get(3),
            "plate",
            "Rubber",
            2);
        assertGt(
            pistol.inputs()
                .get(4),
            "screw",
            "Steel",
            4);
        assertGt(
            pistol.inputs()
                .get(5),
            "springSmall",
            "Steel",
            1);
    }

    @Test
    void revolverRowMatchesTheSheet() {
        // docs/gregguns-spec/02-guns/recipes.md, LV table, Revolver: 15 s, cfg 2, Assembler, LV.
        ProductionTable.Row r = GunTable.REVOLVER;
        assertEquals("revolver", r.output());
        assertEquals(Tier.LV, r.tier());
        assertEquals(ProductionTable.Target.ASSEMBLER, r.target());
        assertEquals(15, r.seconds());
        assertEquals(2, r.circuit());
        assertEquals(
            6,
            r.inputs()
                .size());
        assertEquals(
            "part_barrel_iron",
            r.inputs()
                .get(0)
                .itemName());
        assertEquals(
            "part_mechanical_1",
            r.inputs()
                .get(1)
                .itemName());
        assertEquals(
            "part_stock_wooden",
            r.inputs()
                .get(2)
                .itemName());
        assertGt(
            r.inputs()
                .get(3),
            "plate",
            "Steel",
            2);
        assertGt(
            r.inputs()
                .get(4),
            "screw",
            "Steel",
            4);
        assertGt(
            r.inputs()
                .get(5),
            "springSmall",
            "Steel",
            1);
    }

    @Test
    void sawedOffRowMatchesTheSheet() {
        // docs/gregguns-spec/02-guns/recipes.md, LV table, Sawed Off: 10 s, cfg 3, Assembler, LV.
        ProductionTable.Row r = GunTable.SAWED_OFF;
        assertEquals("sawed_off", r.output());
        assertEquals(Tier.LV, r.tier());
        assertEquals(ProductionTable.Target.ASSEMBLER, r.target());
        assertEquals(10, r.seconds());
        assertEquals(3, r.circuit());
        assertEquals(
            5,
            r.inputs()
                .size());
        assertEquals(
            "part_barrel_iron",
            r.inputs()
                .get(0)
                .itemName());
        assertEquals(
            "part_stock_wooden",
            r.inputs()
                .get(1)
                .itemName());
        assertGt(
            r.inputs()
                .get(2),
            "plate",
            "Steel",
            1);
        assertGt(
            r.inputs()
                .get(3),
            "screw",
            "Steel",
            2);
        assertGt(
            r.inputs()
                .get(4),
            "springSmall",
            "Steel",
            1);
    }

    @Test
    void sawedOffRecyclingSumsTheDirectInputs() {
        assertEquals(100 + 2 * 10 + 5, GunComposition.sawedOffDirectSteel(100, 10, 5));
    }

    @Test
    void shellPressRowMatchesTheSheet() {
        // docs/gregguns-spec/06-items/recipes.md RC-29: 8 shells, LV press, 6 s, brass foil, lead round, gunpowder.
        ProductionTable.Row r = null;
        for (ProductionTable.Row row : ProductionTable.ROWS) {
            if (row.id()
                .equals("RC-29")) {
                r = row;
            }
        }
        assertTrue(r != null, "RC-29 exists");
        assertEquals("ammo_shotgun_shell", r.output());
        assertEquals(8, r.outputCount());
        assertEquals(Tier.LV, r.tier());
        assertEquals(ProductionTable.Target.AMMO_PRESS, r.target());
        assertEquals(6, r.seconds());
        assertEquals(
            3,
            r.inputs()
                .size());
        assertGt(
            r.inputs()
                .get(0),
            "foil",
            "Brass",
            8);
        assertGt(
            r.inputs()
                .get(1),
            "round",
            "Lead",
            24);
        assertGt(
            r.inputs()
                .get(2),
            "dust",
            "Gunpowder",
            4);
    }

    @Test
    void boltActionPartsAreMadeByTheChainOrAtTheCraftingTable() {
        Map<String, ProductionTable.Row> all = ProductionTable.byOutput();
        for (ProductionTable.Input input : GunTable.BOLT_ACTION.inputs()) {
            if (input.isGregGuns()) {
                assertTrue(
                    all.containsKey(input.itemName()) || ProductionTable.CRAFTED_PARTS.contains(input.itemName()),
                    input.itemName());
            }
        }
    }

    @Test
    void boltActionRecyclingSumsTheDirectInputs() {
        assertEquals(2 * 100 + 4 * 10 + 5, GunComposition.boltActionDirectSteel(100, 10, 5));
        assertEquals(3 * 100, GunComposition.ironReceiverIron(100));
        assertEquals(2 * 10, GunComposition.ironReceiverSteel(10));
    }

    @Test
    void everyGunRowUsesADistinctCircuitSoRecipesNeverCollide() {
        java.util.Set<Integer> seen = new java.util.HashSet<>();
        for (ProductionTable.Row row : GunTable.ROWS) {
            assertTrue(seen.add(row.circuit()), row.id() + " reuses circuit " + row.circuit());
        }
    }

    @Test
    void revolverPartsAreMadeByTheChainOrAtTheCraftingTable() {
        Map<String, ProductionTable.Row> all = ProductionTable.byOutput();
        for (ProductionTable.Input input : GunTable.REVOLVER.inputs()) {
            if (input.isGregGuns()) {
                assertTrue(
                    all.containsKey(input.itemName()) || ProductionTable.CRAFTED_PARTS.contains(input.itemName()),
                    input.itemName());
            }
        }
    }

    @Test
    void revolverRecyclingSumsTheDirectInputs() {
        assertEquals(2 * 100 + 4 * 10 + 5, GunComposition.revolverDirectSteel(100, 10, 5));
        assertEquals(30 + 2 * 20, GunComposition.ironBarrelIron(30, 20));
        assertEquals(2 * 10, GunComposition.ironBarrelSteel(10));
        assertEquals(4 * 7 + 5, GunComposition.mechanical1Bronze(7, 5));
    }

    @Test
    void pistolMeetsTheD1GateRule() {
        // 0001: every LV recipe carries a Steel input; the pistol's steel plate is that gate.
        Map<String, ProductionTable.Row> all = ProductionTable.byOutput();
        for (ProductionTable.Row row : GunTable.ROWS) {
            assertTrue(GateRule.satisfied(row, all, ProductionTable.GATE_EXEMPT), row.id());
        }
    }

    @Test
    void pistolPartsAreMadeByTheProductionChain() {
        Map<String, ProductionTable.Row> all = ProductionTable.byOutput();
        for (ProductionTable.Input input : GunTable.PISTOL.inputs()) {
            if (input.isGregGuns()) {
                assertTrue(all.containsKey(input.itemName()), input.itemName());
            }
        }
    }

    @Test
    void recyclingSteelIsTheSumOfTheDirectInputs() {
        // 2 plates + 4 screws + 1 small spring, with made-up unit amounts.
        assertEquals(2 * 100 + 4 * 10 + 5, GunComposition.pistolDirectSteel(100, 10, 5));
    }

    private static void assertGt(ProductionTable.Input input, String prefix, String material, int count) {
        assertEquals(prefix, input.prefix());
        assertEquals(material, input.material());
        assertEquals(count, input.count());
    }

    @Test
    void boltActionRowMatchesTheSheet() {
        // docs/gregguns-spec/02-guns/recipes.md, LV table, Bolt Action Rifle: 20 s, cfg 4, Assembler, LV.
        ProductionTable.Row r = GunTable.BOLT_ACTION;
        assertEquals("bolt_action_rifle", r.output());
        assertEquals(Tier.LV, r.tier());
        assertEquals(ProductionTable.Target.ASSEMBLER, r.target());
        assertEquals(20, r.seconds());
        assertEquals(4, r.circuit());
        assertEquals(
            7,
            r.inputs()
                .size());
        assertEquals(
            "part_barrel_hardened",
            r.inputs()
                .get(0)
                .itemName());
        assertEquals(
            "part_receiver_iron",
            r.inputs()
                .get(1)
                .itemName());
        assertEquals(
            "part_stock_wooden",
            r.inputs()
                .get(2)
                .itemName());
        assertGt(
            r.inputs()
                .get(3),
            "lens",
            "Glass",
            1);
        assertGt(
            r.inputs()
                .get(4),
            "plate",
            "Steel",
            2);
        assertGt(
            r.inputs()
                .get(5),
            "screw",
            "Steel",
            4);
        assertGt(
            r.inputs()
                .get(6),
            "springSmall",
            "Steel",
            1);
    }
}
