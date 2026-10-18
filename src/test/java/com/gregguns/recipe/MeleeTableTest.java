package com.gregguns.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.gregguns.api.Tier;

class MeleeTableTest {

    @Test
    void everyRowIsAnLvAssemblerRecipe() {
        for (ProductionTable.Row row : MeleeTable.ROWS) {
            assertEquals(Tier.LV, row.tier(), row.id());
            assertEquals(ProductionTable.Target.ASSEMBLER, row.target(), row.id());
            assertEquals(10, row.seconds(), row.id());
        }
    }

    @Test
    void rowsMatchTheSheet() {
        // docs/gregguns-spec/02-guns/recipes.md, LV table.
        assertEquals(5, MeleeTable.COMBAT_KNIFE.circuit());
        assertEquals(6, MeleeTable.CROWBAR.circuit());
        assertEquals(7, MeleeTable.STIELGRANATE.circuit());
        assertEquals(16, MeleeTable.STIELGRANATE.outputCount());
        assertEquals(
            3,
            MeleeTable.COMBAT_KNIFE.inputs()
                .size());
        assertEquals(
            "stickLong",
            MeleeTable.CROWBAR.inputs()
                .get(0)
                .prefix());
        assertEquals(
            "Gunpowder",
            MeleeTable.STIELGRANATE.inputs()
                .get(1)
                .material());
    }

    @Test
    void everyRowMeetsTheSteelGate() {
        Map<String, ProductionTable.Row> all = ProductionTable.byOutput();
        for (ProductionTable.Row row : MeleeTable.ROWS) {
            assertTrue(GateRule.satisfied(row, all, ProductionTable.GATE_EXEMPT), row.id());
        }
    }
}
