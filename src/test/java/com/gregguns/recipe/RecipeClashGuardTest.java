package com.gregguns.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Guards against two GregGuns machine rows with the same map, inputs, circuit and fluids: the machine would run
 * whichever it matched first, so the other output would be unreachable. The pack-wide check against GT's own recipes
 * is a recipe dump; this test catches the self-clashes (the T3 boots once did) and pins the casing circuits that keep
 * the two Stainless Steel casings off GT's own.
 */
class RecipeClashGuardTest {

    private static List<ProductionTable.Row> allRows() {
        List<ProductionTable.Row> all = new ArrayList<>();
        all.addAll(ProductionTable.ROWS);
        all.addAll(GunTable.ROWS);
        all.addAll(AmmoLineTable.rows());
        all.addAll(ComponentTable.rows());
        all.addAll(EquipmentTable.ROWS);
        all.addAll(FlightTable.ROWS);
        all.addAll(GrenadeTable.ROWS);
        all.addAll(MeleeTable.ROWS);
        all.addAll(ArmorTable.COMBAT);
        all.addAll(ArmorTable.T3_COMBAT);
        all.addAll(ArmorTable.T3_HEV);
        all.addAll(GunLineTable.rows());
        return all;
    }

    private static String key(ProductionTable.Row row) {
        StringBuilder key = new StringBuilder(row.target() + "/c" + row.circuit());
        List<String> inputs = new ArrayList<>();
        for (ProductionTable.Input input : row.inputs()) {
            String name = input.itemName() != null ? input.itemName()
                : input.oreName() != null ? "ore:" + input.oreName()
                    : input.special() != null ? "special:" + input.special() : input.prefix() + "." + input.material();
            inputs.add(name + "x" + input.count());
        }
        java.util.Collections.sort(inputs);
        key.append(inputs);
        List<String> fluids = new ArrayList<>();
        for (ComponentTable.FluidInput fluid : row.fluids()) {
            fluids.add(fluid.kind() + ":" + fluid.material() + "x" + fluid.amount());
        }
        java.util.Collections.sort(fluids);
        return key.append(fluids)
            .toString();
    }

    @Test
    void noTwoMachineRowsShareMapInputsCircuitAndFluids() {
        Map<String, String> seen = new HashMap<>();
        List<String> clashes = new ArrayList<>();
        for (ProductionTable.Row row : allRows()) {
            String other = seen.put(key(row), row.id());
            if (other != null && !other.equals(row.id())) {
                clashes.add(other + " and " + row.id());
            }
        }
        assertTrue(clashes.isEmpty(), "rows with identical recipes: " + clashes);
    }

    @Test
    void theT3BootsDiffer() {
        ProductionTable.Row combat = ArmorTable.T3_COMBAT.get(3);
        ProductionTable.Row hev = ArmorTable.T3_HEV.get(3);
        assertNotEquals(key(combat), key(hev));
    }

    @Test
    void theHevBootsAreOnTheirOwnCircuitSoTheCombatBootsAreNotASubset() {
        // S2 of the recipe-clash review: the Combat boots (epoxy and titanium, circuit 4) are a subset of the HEV
        // boots.
        assertEquals(
            4,
            ArmorTable.T3_COMBAT.get(3)
                .circuit());
        assertEquals(
            5,
            ArmorTable.T3_HEV.get(3)
                .circuit());
    }

    @Test
    void theTurretPlatesAndCarbonBastionAvoidSubsetCircuits() {
        // S6/S7/S8: circuit 0 matched about 25 pack Assembler rows; Carbon Bastion on 4 sat under GT's Concrete row.
        assertEquals(12, com.gregguns.utility.TurretPlate.CARBON.circuit());
        assertEquals(14, com.gregguns.utility.TurretPlate.STEEL.circuit());
        assertEquals(15, com.gregguns.utility.TurretPlate.ALUMINUM.circuit());
        assertEquals(10, com.gregguns.utility.rampart.BastionPlating.CARBON.circuit());
    }

    @Test
    void theLaserFocusTakesNoDiamondSoGtsWhiteLensRowDoesNotMatchIt() {
        // S4: GT's engraver row (white lens plus 3 diamonds) is a subset of a recipe that holds diamonds and the lens.
        for (ProductionTable.Row row : ComponentTable.rows()) {
            if (row.id()
                .equals("RC-64")) {
                for (ProductionTable.Input input : row.inputs()) {
                    assertNotEquals("Diamond", input.material());
                }
            }
        }
    }

    @Test
    void casingCircuitsAvoidGtsStainlessCasingAndEachOther() {
        assertEquals(1, CasingCircuits.GT_STAINLESS_CASING);
        assertNotEquals(CasingCircuits.GT_STAINLESS_CASING, CasingCircuits.WORKS_CASING);
        assertNotEquals(CasingCircuits.GT_STAINLESS_CASING, CasingCircuits.GANTRY_CASING);
        assertNotEquals(CasingCircuits.WORKS_CASING, CasingCircuits.GANTRY_CASING);
        assertTrue(CasingCircuits.WORKS_CASING >= 1 && CasingCircuits.WORKS_CASING <= 24);
        assertTrue(CasingCircuits.GANTRY_CASING >= 1 && CasingCircuits.GANTRY_CASING <= 24);
    }
}
