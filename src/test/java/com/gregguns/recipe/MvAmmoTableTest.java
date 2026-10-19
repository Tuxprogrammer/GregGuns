package com.gregguns.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.gregguns.api.Tier;
import com.gregguns.item.ammo.AmmoTypes;

/** The SMG and Assault Rifle magazine rows (RC-38, RC-39, RC-45, RC-46) against recipes.md. */
// Spec: docs/gregguns-spec/06-items/recipes.md (RC-38, RC-39, RC-45, RC-46, RC-51)
class MvAmmoTableTest {

    private static ProductionTable.Row row(String id) {
        for (ProductionTable.Row row : ProductionTable.ROWS) {
            if (row.id()
                .equals(id)) {
                return row;
            }
        }
        throw new AssertionError("no row " + id);
    }

    @Test
    void emptyMagazineRowsAreMvAluminiumPressRows() {
        ProductionTable.Row smg = row("RC-38");
        assertEquals("mag_smg_empty", smg.output());
        assertEquals(8, smg.outputCount());
        assertEquals(Tier.MV, smg.tier());
        assertEquals(4, smg.seconds());
        assertEquals(ProductionTable.Target.AMMO_PRESS, smg.target());
        ProductionTable.Row ar = row("RC-39");
        assertEquals("mag_assault_rifle_empty", ar.output());
        assertEquals(8, ar.outputCount());
        assertEquals(Tier.MV, ar.tier());
        assertEquals(5, ar.seconds());
        assertEquals(
            3,
            ar.inputs()
                .get(0)
                .count());
        assertEquals(
            "Aluminium",
            ar.inputs()
                .get(0)
                .material());
    }

    @Test
    void loadedMagazineRowsTakeFourEmptiesAndTheirRounds() {
        ProductionTable.Row smg = row("RC-45");
        assertEquals("mag_smg", smg.output());
        assertEquals(4, smg.outputCount());
        assertEquals(2, smg.seconds());
        assertEquals(
            "mag_smg_empty",
            smg.inputs()
                .get(0)
                .itemName());
        assertEquals(
            4,
            smg.inputs()
                .get(0)
                .count());
        assertEquals(
            "ammo_9mm_bullet",
            smg.inputs()
                .get(1)
                .itemName());
        assertEquals(
            8,
            smg.inputs()
                .get(1)
                .count());
        ProductionTable.Row ar = row("RC-46");
        assertEquals("mag_assault_rifle", ar.output());
        assertEquals(
            "ammo_rifle_bullet",
            ar.inputs()
                .get(1)
                .itemName());
        assertEquals(
            12,
            ar.inputs()
                .get(1)
                .count());
    }

    @Test
    void everyNewRowMeetsTheGateRule() {
        Map<String, ProductionTable.Row> all = ProductionTable.byOutput();
        for (String id : new String[] { "RC-38", "RC-39", "RC-45", "RC-46" }) {
            assertTrue(GateRule.satisfied(row(id), all, ProductionTable.GATE_EXEMPT), id);
        }
    }

    @Test
    void magazinesAreIndexedAndCarryTheirRounds() {
        assertEquals(
            2,
            AmmoTypes.magazine("smg")
                .roundsPerMagazine());
        assertEquals(
            3,
            AmmoTypes.magazine("assault_rifle")
                .roundsPerMagazine());
        assertEquals(
            Tier.MV,
            AmmoTypes.magazine("smg")
                .tier());
        assertEquals(
            AmmoTypes.NINE_MM,
            AmmoTypes.magazine("smg")
                .ammo());
        assertEquals(
            AmmoTypes.RIFLE,
            AmmoTypes.magazine("assault_rifle")
                .ammo());
    }
}
