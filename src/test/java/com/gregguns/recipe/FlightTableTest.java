package com.gregguns.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.gregguns.api.Tier;

/** The flight gear recipes against docs/gregguns-spec/05-armor/recipes.md and the D1 gate rule. */
class FlightTableTest {

    private static ProductionTable.Row row(String output) {
        ProductionTable.Row row = FlightTable.byOutput()
            .get(output);
        assertNotNull(row, output);
        return row;
    }

    @Test
    void tiersAndTimesAreTheSheetValues() {
        assertEquals(Tier.HV, row("part_glider_backpack").tier());
        assertEquals(Tier.HV, row("part_glider_wing").tier());
        assertEquals(Tier.HV, row("glider").tier());
        assertEquals(Tier.HV, row("jump_pack").tier());
        assertEquals(Tier.IV, row("anti_gravity_device").tier());
        assertEquals(18, row("part_glider_backpack").seconds());
        assertEquals(18, row("part_glider_wing").seconds());
        assertEquals(10, row("glider").seconds());
        assertEquals(18, row("jump_pack").seconds());
        assertEquals(18, row("anti_gravity_device").seconds());
        assertEquals(3, row("glider").circuit());
        for (ProductionTable.Row r : FlightTable.ROWS) {
            assertEquals(ProductionTable.Target.ASSEMBLER, r.target(), r.id());
        }
    }

    @Test
    void everyRowMeetsTheGateRule() {
        Map<String, ProductionTable.Row> all = new HashMap<>(ProductionTable.byOutput());
        all.putAll(EquipmentTable.byOutput());
        all.putAll(FlightTable.byOutput());
        for (ProductionTable.Row r : FlightTable.ROWS) {
            assertTrue(GateRule.satisfied(r, all, ProductionTable.GATE_EXEMPT), r.id() + " " + r.output());
        }
    }

    @Test
    void jumpPackAndAntiGravityNeedTheGliderBackpack() {
        for (String output : new String[] { "jump_pack", "anti_gravity_device" }) {
            Set<String> inputs = new HashSet<>();
            for (ProductionTable.Input in : row(output).inputs()) {
                if (in.isGregGuns()) {
                    inputs.add(in.itemName());
                }
            }
            assertTrue(inputs.contains("part_glider_backpack"), output);
        }
    }

    @Test
    void theAntiGravityCoreOnlyGoesIntoTheIvDevice() {
        // Input tiers are at most the output tier: the IV core cannot be in an HV recipe.
        for (ProductionTable.Row r : FlightTable.ROWS) {
            boolean usesCore = r.inputs()
                .stream()
                .anyMatch(in -> FlightTable.ANTI_GRAVITY_CORE.equals(in.itemName()));
            assertTrue(!usesCore || r.tier() == Tier.IV, r.id());
        }
    }

    @Test
    void noJetpackIsProduced() {
        // GGS-05-38
        for (ProductionTable.Row r : FlightTable.ROWS) {
            assertFalse(
                r.output()
                    .contains("jetpack"),
                r.id());
        }
    }
}
