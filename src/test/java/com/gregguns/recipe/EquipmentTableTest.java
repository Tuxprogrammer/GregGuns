package com.gregguns.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.gregguns.api.Tier;

/** The special-slot equipment recipes against docs/gregguns-spec/05-armor/recipes.md and the D1 gate rule. */
class EquipmentTableTest {

    private static ProductionTable.Row row(String output) {
        ProductionTable.Row row = EquipmentTable.byOutput()
            .get(output);
        assertNotNull(row, output);
        return row;
    }

    @Test
    void everyItemHasARecipe() {
        for (String name : new String[] { "part_gas_mask_filter", "gas_mask", "night_vision_goggles", "ammo_pouch",
            "oxygen_mask", "tank_air", "oxygen_tanks", "tactical_mask" }) {
            row(name);
        }
    }

    @Test
    void tiersAreTheSheetTiers() {
        // GGS-05-33 with 0010 decision 5: Tactical Mask is HV, not the MV hazard-protection.md lists.
        assertEquals(Tier.MV, row("gas_mask").tier());
        assertEquals(Tier.MV, row("night_vision_goggles").tier());
        assertEquals(Tier.MV, row("part_gas_mask_filter").tier());
        assertEquals(Tier.HV, row("oxygen_mask").tier());
        assertEquals(Tier.HV, row("oxygen_tanks").tier());
        assertEquals(Tier.HV, row("tactical_mask").tier());
        for (ProductionTable.Row r : EquipmentTable.ROWS) {
            assertEquals(ProductionTable.Target.ASSEMBLER, r.target(), r.id());
        }
    }

    @Test
    void sheetAmountsAndTimes() {
        ProductionTable.Row gas = row("gas_mask");
        assertEquals(18, gas.seconds());
        assertEquals(1, gas.circuit());
        assertEquals(
            5,
            gas.inputs()
                .get(0)
                .count());
        assertEquals(
            "Rubber",
            gas.inputs()
                .get(0)
                .material());
        assertEquals(2, row("night_vision_goggles").circuit());
        assertEquals(16, row("oxygen_tanks").seconds());
        assertEquals(18, row("tactical_mask").seconds());
        assertEquals(10, row("oxygen_mask").seconds());
    }

    @Test
    void everyRowMeetsTheGateRule() {
        Map<String, ProductionTable.Row> all = new HashMap<>(ProductionTable.byOutput());
        all.putAll(EquipmentTable.byOutput());
        for (ProductionTable.Row r : EquipmentTable.ROWS) {
            assertTrue(GateRule.satisfied(r, all, ProductionTable.GATE_EXEMPT), r.id() + " " + r.output());
        }
    }

    @Test
    void noRecipeUsesATechgunsMaterial() {
        // GGS-05-31
        Set<String> banned = new HashSet<>(Arrays.asList("ObsidianSteel", "Carbon", "Iron"));
        for (ProductionTable.Row r : EquipmentTable.ROWS) {
            for (ProductionTable.Input in : r.inputs()) {
                if (!in.isGregGuns() && in.special() == null) {
                    assertFalse(banned.contains(in.material()), r.id());
                }
            }
        }
    }

    @Test
    void noJetpackIsProduced() {
        // GGS-05-38
        for (ProductionTable.Row r : EquipmentTable.ROWS) {
            assertFalse(
                r.output()
                    .contains("jetpack"),
                r.id());
        }
    }

    @Test
    void tacticalMaskConsumesTheGearItCombines() {
        Set<String> inputs = new HashSet<>();
        for (ProductionTable.Input in : row("tactical_mask").inputs()) {
            if (in.isGregGuns()) {
                inputs.add(in.itemName());
            }
        }
        assertTrue(inputs.contains("night_vision_goggles"));
        assertTrue(inputs.contains("gas_mask"));
        assertTrue(inputs.contains("oxygen_mask"));
    }

    @Test
    void specialInputsAreTaggedAndNotGregGuns() {
        ProductionTable.Input sensor = ProductionTable.Input.itemList("Sensor_MV", 1);
        assertEquals("gt:Sensor_MV", sensor.special());
        assertFalse(sensor.isGregGuns());
        assertEquals(
            "mc:glass",
            ProductionTable.Input.vanilla("glass", 2)
                .special());
    }
}
