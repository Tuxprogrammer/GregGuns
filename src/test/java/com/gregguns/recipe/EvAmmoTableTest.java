package com.gregguns.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.gregguns.api.Tier;
import com.gregguns.gun.logic.GunCatalog.AmmoIds;
import com.gregguns.item.ammo.def.AdvancedAmmo;
import com.gregguns.item.ammo.def.AmmoDefinitions;
import com.gregguns.item.ammo.def.RifleAmmo;

/** The EV ammo rows (RC-32, RC-42, RC-43, RC-49, RC-50) against recipes.md. */
// Spec: docs/gregguns-spec/06-items/recipes.md (RC-32, RC-42, RC-43, RC-49, RC-50, RC-51)
class EvAmmoTableTest {

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
    void everyEvRowIsAnEvAmmoPressRowThatSatisfiesTheTitaniumGate() {
        for (String id : new String[] { "RC-32", "RC-42", "RC-43", "RC-49", "RC-50" }) {
            ProductionTable.Row row = row(id);
            assertEquals(Tier.EV, row.tier(), id);
            assertEquals(ProductionTable.Target.AMMO_PRESS, row.target(), id);
            assertEquals(0, row.circuit(), id);
            assertTrue(
                row.inputs()
                    .size() <= 6,
                id);
            assertTrue(GateRule.satisfied(row, ProductionTable.byOutput(), ProductionTable.GATE_EXEMPT), id);
        }
    }

    @Test
    void advancedBulletRowMakesSixteenFromTitaniumFoilRoundsAndTnt() {
        ProductionTable.Row row = row("RC-32");
        assertEquals("ammo_advanced_bullet", row.output());
        assertEquals(16, row.outputCount());
        assertEquals(4, row.seconds());
        assertEquals(
            "Titanium",
            row.inputs()
                .get(0)
                .material());
        assertEquals(
            "mc:tnt",
            row.inputs()
                .get(2)
                .special());
    }

    @Test
    void magazineAndDrumRowsMatchTheSheet() {
        assertEquals(8, row("RC-42").outputCount());
        assertEquals(5, row("RC-42").seconds());
        assertEquals(4, row("RC-43").outputCount());
        assertEquals(6, row("RC-43").seconds());
        ProductionTable.Row mag = row("RC-49");
        assertEquals("mag_advanced", mag.output());
        assertEquals(4, mag.outputCount());
        assertEquals(
            12,
            mag.inputs()
                .get(1)
                .count());
        ProductionTable.Row drum = row("RC-50");
        assertEquals("mag_minigun_drum", drum.output());
        assertEquals(
            "ammo_rifle_bullet",
            drum.inputs()
                .get(1)
                .itemName());
        assertEquals(
            64,
            drum.inputs()
                .get(1)
                .count());
    }

    @Test
    void refillsUseThreeAdvancedBulletsAndSixteenRifleBullets() {
        assertEquals(3, AdvancedAmmo.MAGAZINE.roundsPerMagazine());
        assertEquals(16, RifleAmmo.MINIGUN_DRUM.roundsPerMagazine());
        assertNotNull(AmmoDefinitions.byId("advanced"));
        assertEquals(AmmoIds.ADVANCED_BULLET, AdvancedAmmo.TYPE.roundItemName());
        assertEquals(AmmoIds.ADVANCED_MAGAZINE, AdvancedAmmo.MAGAZINE.loadedItemName());
        assertEquals(AmmoIds.ADVANCED_MAGAZINE_EMPTY, AdvancedAmmo.MAGAZINE.emptyItemName());
        assertEquals(AmmoIds.MINIGUN_DRUM, RifleAmmo.MINIGUN_DRUM.loadedItemName());
        assertEquals(AmmoIds.MINIGUN_DRUM_EMPTY, RifleAmmo.MINIGUN_DRUM.emptyItemName());
        assertEquals(Tier.EV, AdvancedAmmo.TYPE.tier());
    }
}
