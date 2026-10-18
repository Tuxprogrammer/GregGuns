package com.gregguns.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.gregguns.api.Tier;

class GrenadeTableTest {

    @Test
    void everyRowIsAnMvAssemblerRecipeOfSixteen() {
        for (ProductionTable.Row row : GrenadeTable.ROWS) {
            assertEquals(Tier.MV, row.tier(), row.id());
            assertEquals(ProductionTable.Target.ASSEMBLER, row.target(), row.id());
            assertEquals(15, row.seconds(), row.id());
            assertEquals(16, row.outputCount(), row.id());
        }
    }

    @Test
    void rowsMatchTheSheet() {
        // docs/gregguns-spec/02-guns/recipes.md, MV table.
        assertEquals(6, GrenadeTable.FRAG_GRENADE.circuit());
        assertEquals(7, GrenadeTable.FLASHBANG.circuit());
        assertEquals(
            "round",
            GrenadeTable.FRAG_GRENADE.inputs()
                .get(1)
                .prefix(),
            "the Steel rounds are the fragments (GGS-02-05)");
        assertEquals(
            16,
            GrenadeTable.FRAG_GRENADE.inputs()
                .get(1)
                .count());
        assertEquals(
            "Magnesium",
            GrenadeTable.FLASHBANG.inputs()
                .get(1)
                .material());
    }

    @Test
    void everyRowMeetsTheAluminiumGate() {
        Map<String, ProductionTable.Row> all = ProductionTable.byOutput();
        for (ProductionTable.Row row : GrenadeTable.ROWS) {
            assertTrue(GateRule.satisfied(row, all, ProductionTable.GATE_EXEMPT), row.id());
            assertEquals(
                "Aluminium",
                row.inputs()
                    .get(0)
                    .material());
        }
    }
}
