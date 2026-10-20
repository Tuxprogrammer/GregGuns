package com.gregguns.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.gregguns.api.Tier;
import com.gregguns.recipe.GateRule;

import gregtech.api.enums.TierEU;

/** GGS-09-29, GGS-09-30, GGS-09-35 and GGS-09-38: the turret armor plate data. */
class TurretPlateTest {

    @Test
    void armorValuesAreFiveTenFifteenTwenty() {
        assertEquals(5, TurretPlate.IRON.armorValue());
        assertEquals(10, TurretPlate.STEEL.armorValue());
        assertEquals(15, TurretPlate.ALUMINUM.armorValue());
        assertEquals(20, TurretPlate.CARBON.armorValue());
    }

    @Test
    void assemblerPlatesHaveTheirOwnCircuits() {
        // S6/S7 of the recipe-clash review: circuit 0 matched about 25 pack Assembler rows holding the same plates.
        assertEquals(0, TurretPlate.IRON.circuit());
        assertEquals(14, TurretPlate.STEEL.circuit());
        assertEquals(15, TurretPlate.ALUMINUM.circuit());
        assertEquals(12, TurretPlate.CARBON.circuit());
    }

    @Test
    void onlyTheIronPlateIsMadeByHand() {
        // GGS-09-29
        for (TurretPlate p : TurretPlate.values()) {
            assertEquals(p == TurretPlate.IRON, p.handMade(), p.name());
        }
    }

    @Test
    void assemblerRecipesRunAtTheirTierVoltage() {
        // GGS-09-29 and GGS-09-38: the recipe voltage is the plate's own tier, never above it.
        assertEquals(TierEU.RECIPE_LV, TurretPlate.STEEL.recipeEut());
        assertEquals(TierEU.RECIPE_MV, TurretPlate.ALUMINUM.recipeEut());
        assertEquals(TierEU.RECIPE_EV, TurretPlate.CARBON.recipeEut());
        for (TurretPlate p : TurretPlate.values()) {
            if (!p.handMade()) {
                assertTrue(
                    p.recipeEut() <= p.tier()
                        .voltage(),
                    p.name());
                assertTrue(
                    p.recipeEut() > p.tier()
                        .voltage() / 4,
                    p.name() + " is not a lower tier's recipe");
                assertEquals(200, p.durationTicks(), p.name());
            }
        }
    }

    @Test
    void everyRecipeHoldsItsTiersGateMaterial() {
        // GGS-09-30: the sheet's wrought iron is GT's Cast Iron alias; the others use GateRule's material.
        assertEquals("CastIron", TurretPlate.IRON.mainMaterial());
        assertEquals(GateRule.gateMaterial(Tier.LV), TurretPlate.STEEL.mainMaterial());
        assertEquals(GateRule.gateMaterial(Tier.MV), TurretPlate.ALUMINUM.mainMaterial());
        assertEquals(GateRule.gateMaterial(Tier.EV), TurretPlate.CARBON.extraMaterial());
        assertEquals("EpoxidFiberReinforced", TurretPlate.CARBON.mainMaterial());
    }

    @Test
    void inputCountsMatchTheSheet() {
        assertEquals(5, TurretPlate.IRON.mainCount());
        assertEquals(5, TurretPlate.STEEL.mainCount());
        assertEquals(5, TurretPlate.ALUMINUM.mainCount());
        assertEquals(3, TurretPlate.CARBON.mainCount());
        assertEquals(2, TurretPlate.CARBON.extraCount());
        assertNull(TurretPlate.IRON.extraMaterial());
        assertNull(TurretPlate.STEEL.extraMaterial());
    }

    @Test
    void registryNamesAreUniqueAndResolve() {
        for (TurretPlate p : TurretPlate.values()) {
            assertTrue(
                p.registryName()
                    .startsWith("turret_armor_"));
            assertEquals(p, TurretPlate.byRegistryName(p.registryName()));
        }
        assertNotNull(TurretPlate.byRegistryName("turret_armor_carbon"));
        assertNull(TurretPlate.byRegistryName("turret_armor_obsidian"));
        assertFalse(
            TurretPlate.ALUMINUM.registryName()
                .contains("obsidian"));
    }

    @Test
    void tiersRiseWithArmor() {
        TurretPlate[] ps = TurretPlate.values();
        for (int i = 1; i < ps.length; i++) {
            assertTrue(
                ps[i].tier()
                    .voltageIndex()
                    > ps[i - 1].tier()
                        .voltageIndex());
            assertTrue(ps[i].armorValue() > ps[i - 1].armorValue());
        }
    }
}
