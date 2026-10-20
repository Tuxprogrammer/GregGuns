package com.gregguns.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.gregguns.api.Tier;
import com.gregguns.recipe.GateRule;
import com.gregguns.recipe.ProductionTable;

class AmmoPressTierTest {

    @Test
    void everyTierHasAUniqueIdInTheReservedBlockAndIsListedInAll() {
        // GGS-08-48, GGS-08-53
        Set<Integer> listed = new HashSet<>();
        for (int id : GregGunsMTEIDs.all()) {
            listed.add(id);
        }
        Set<Integer> seen = new HashSet<>();
        for (AmmoPressTier press : AmmoPressTier.values()) {
            assertTrue(press.mteId() >= GregGunsMTEIDs.BLOCK_FIRST && press.mteId() <= GregGunsMTEIDs.BLOCK_LAST);
            assertTrue(seen.add(press.mteId()), "duplicate " + press);
            assertTrue(listed.contains(press.mteId()), press + " missing from GregGunsMTEIDs.all()");
        }
        assertEquals(5, seen.size());
    }

    @Test
    void idsAreContiguousFromSteamToEv() {
        int expected = 20100;
        for (AmmoPressTier press : AmmoPressTier.values()) {
            assertEquals(expected++, press.mteId());
        }
    }

    @Test
    void tierParametersMatchTheRoster() {
        assertEquals(Tier.STEAM, AmmoPressTier.STEAM.tier());
        assertEquals(Tier.LV, AmmoPressTier.LV.tier());
        assertEquals(Tier.MV, AmmoPressTier.MV.tier());
        assertEquals(Tier.HV, AmmoPressTier.HV.tier());
        assertEquals(Tier.EV, AmmoPressTier.EV.tier());
        assertTrue(AmmoPressTier.STEAM.isSteam());
        assertFalse(AmmoPressTier.LV.isSteam());
    }

    @Test
    void aPressRunsRecipesAtOrBelowItsTierOnly() {
        assertTrue(AmmoPressTier.STEAM.canRun(Tier.STEAM));
        // GT steam machines run recipes up to LV (MTEBasicMachineBronze#checkRecipe), no higher.
        assertTrue(AmmoPressTier.STEAM.canRun(Tier.LV));
        assertFalse(AmmoPressTier.STEAM.canRun(Tier.MV));
        assertEquals(Tier.LV, AmmoPressTier.STEAM.recipeCeiling());
        assertEquals(Tier.HV, AmmoPressTier.HV.recipeCeiling());
        assertTrue(AmmoPressTier.HV.canRun(Tier.LV));
        assertTrue(AmmoPressTier.HV.canRun(Tier.HV));
        assertFalse(AmmoPressTier.MV.canRun(Tier.HV));
    }

    @Test
    void lowestForFindsTheCheapestPress() {
        assertEquals(AmmoPressTier.STEAM, AmmoPressTier.lowestFor(Tier.STEAM));
        assertEquals(AmmoPressTier.MV, AmmoPressTier.lowestFor(Tier.MV));
        assertEquals(AmmoPressTier.EV, AmmoPressTier.lowestFor(Tier.EV));
        assertNull(AmmoPressTier.lowestFor(Tier.IV));
    }

    @Test
    void theHullMetalIsTheTierGateMaterial() {
        // recipes.md: Bronze, Steel, Aluminium, StainlessSteel, Titanium hulls are exactly the D1 gate materials.
        assertEquals("Bronze", GateRule.gateMaterial(AmmoPressTier.STEAM.tier()));
        assertEquals("Steel", GateRule.gateMaterial(AmmoPressTier.LV.tier()));
        assertEquals("Aluminium", GateRule.gateMaterial(AmmoPressTier.MV.tier()));
        assertEquals("StainlessSteel", GateRule.gateMaterial(AmmoPressTier.HV.tier()));
        assertEquals("Titanium", GateRule.gateMaterial(AmmoPressTier.EV.tier()));
    }

    @Test
    void pressRowHelperBuildsAnAmmoMapRowRunnableOnlyFromItsTier() {
        ProductionTable.Row row = ProductionTable
            .pressRow("T-1", "ammo_test", 2, Tier.HV, 4, ProductionTable.Input.gt("plate", "StainlessSteel", 1));
        assertEquals(ProductionTable.Target.AMMO_PRESS, row.target());
        assertEquals(0, row.circuit());
        assertEquals(Tier.HV, row.tier());
        assertTrue(AmmoPressTier.HV.canRun(row.tier()));
        assertFalse(AmmoPressTier.MV.canRun(row.tier()));
        ProductionTable.Input one = ProductionTable.Input.mod("a", 1);
        assertThrows(
            IllegalArgumentException.class,
            () -> ProductionTable.pressRow("T-2", "x", 1, Tier.LV, 1, one, one, one, one, one, one, one));
    }

    @Test
    void steamRunsEveryRowUpToLvAndEveryElectricPressRunsTheLvRows() {
        // The Steam press runs every ammo row at or below LV, steam-tagged or not (GT's steam machines do).
        List<ProductionTable.Row> steam = ProductionTable.pressRowsFor(AmmoPressTier.STEAM);
        List<ProductionTable.Row> expected = new java.util.ArrayList<>();
        for (ProductionTable.Row row : ProductionTable.ROWS) {
            if (row.target() == ProductionTable.Target.AMMO_PRESS && row.tier()
                .ordinal() <= Tier.LV.ordinal()) {
                expected.add(row);
            }
        }
        assertEquals(expected, steam);
        assertTrue(
            steam.stream()
                .anyMatch(r -> r.tier() == Tier.LV),
            "an LV row, which is not steam-tagged");
        assertTrue(
            steam.stream()
                .anyMatch(r -> r.tier() == Tier.STEAM),
            "the ULV stone bullet");
        assertTrue(
            steam.stream()
                .noneMatch(
                    r -> r.tier()
                        .ordinal() > Tier.LV.ordinal()));
        int lvRows = ProductionTable.pressRowsFor(AmmoPressTier.LV)
            .size();
        assertTrue(lvRows >= 3);
        // The MV and HV rows (the SMG and Assault Rifle magazines, the 40 mm grenade, the rocket) are the only ones
        // above LV so far; the EV press runs all.
        long upperRows = ProductionTable.ROWS.stream()
            .filter(
                r -> r.target() == ProductionTable.Target.AMMO_PRESS
                    && (r.tier() == com.gregguns.api.Tier.MV || r.tier() == com.gregguns.api.Tier.HV
                        || r.tier() == com.gregguns.api.Tier.EV))
            .count();
        assertEquals(
            lvRows + upperRows,
            ProductionTable.pressRowsFor(AmmoPressTier.EV)
                .size());
        assertEquals(
            lvRows,
            ProductionTable.pressRowsFor(AmmoPressTier.LV)
                .size());
    }
}
