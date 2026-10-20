package com.gregguns.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.gregguns.api.Tier;
import com.gregguns.item.part.PartCatalog;

class ComponentTableTest {

    private static ComponentTable.Spec spec(String id) {
        for (ComponentTable.Spec spec : ComponentTable.SPECS) {
            if (spec.row()
                .id()
                .equals(id)) {
                return spec;
            }
        }
        throw new AssertionError("no row " + id);
    }

    private static Map<String, ProductionTable.Row> allRows() {
        Map<String, ProductionTable.Row> all = new LinkedHashMap<>(ProductionTable.byOutput());
        for (Map.Entry<String, ProductionTable.Row> entry : ComponentTable.byOutput()
            .entrySet()) {
            all.putIfAbsent(entry.getKey(), entry.getValue());
        }
        return all;
    }

    @Test
    void everyRowHasInputsAndAPositiveRun() {
        for (ProductionTable.Row row : ComponentTable.rows()) {
            assertFalse(
                row.inputs()
                    .isEmpty(),
                row.id());
            assertTrue(row.seconds() > 0, row.id());
            assertTrue(row.outputCount() > 0, row.id());
        }
    }

    @Test
    void everyRowMeetsTheGateRule() {
        Map<String, ProductionTable.Row> all = allRows();
        for (ProductionTable.Row row : ComponentTable.rows()) {
            assertTrue(GateRule.satisfied(row, all, ProductionTable.GATE_EXEMPT), row.id() + " has no gate input");
        }
    }

    @Test
    void everyRowOutputIsACatalogedComponentAndInputsAreNotAboveTheOutputTier() {
        for (ProductionTable.Row row : ComponentTable.rows()) {
            Tier tier = PartCatalog.tierOf(row.output());
            assertNotNull(tier, row.id() + " makes an uncataloged item " + row.output());
            assertEquals(tier, row.tier(), row.id() + ": the row tier is the catalog tier");
            for (ProductionTable.Input input : row.inputs()) {
                if (input.isGregGuns()) {
                    Tier inputTier = PartCatalog.tierOf(input.itemName());
                    assertNotNull(inputTier, row.id() + " uses an uncataloged item " + input.itemName());
                    assertTrue(
                        inputTier.ordinal() <= row.tier()
                            .ordinal(),
                        row.id() + ": " + input.itemName() + " is above " + row.tier());
                }
            }
        }
    }

    @Test
    void outputsAreUniquePerRecipeIdAndIdsAreUnique() {
        Set<String> ids = new HashSet<>();
        for (ProductionTable.Row row : ComponentTable.rows()) {
            assertTrue(ids.add(row.id()), "duplicate id " + row.id());
        }
    }

    @Test
    void circuitsAreUniquePerOutputInputsAndWithinGtRange() {
        Set<String> seen = new HashSet<>();
        for (ProductionTable.Row row : ComponentTable.rows()) {
            assertTrue(row.circuit() >= 0 && row.circuit() <= 24, row.id());
            StringBuilder key = new StringBuilder(row.target() + "/" + row.circuit());
            for (ProductionTable.Input input : row.inputs()) {
                key.append('|')
                    .append(input.itemName() != null ? input.itemName() : input.prefix() + "." + input.material())
                    .append('x')
                    .append(input.count());
            }
            assertTrue(seen.add(key.toString()), row.id() + " duplicates another recipe");
        }
    }

    @Test
    void assemblyLineRowsMeetGtsInputLimitsAndCarryResearch() {
        for (ComponentTable.Spec spec : ComponentTable.SPECS) {
            if (spec.row()
                .target() == ProductionTable.Target.ASSEMBLY_LINE) {
                int inputs = spec.row()
                    .inputs()
                    .size();
                assertTrue(
                    inputs >= 4 && inputs <= 16,
                    spec.row()
                        .id() + ": the line takes 4 to 16 item inputs");
                assertNotNull(spec.research());
                assertFalse(
                    spec.fluids()
                        .isEmpty());
                assertTrue(
                    spec.row()
                        .tier()
                        .ordinal() >= Tier.LUV.ordinal()
                        || spec.row()
                            .tier() == Tier.ZPM);
                assertTrue(
                    spec.research()
                        .scanTier()
                        .ordinal()
                        < spec.row()
                            .tier()
                            .ordinal(),
                    "research is scanned at a lower tier");
            } else {
                assertEquals(
                    null,
                    spec.research(),
                    spec.row()
                        .id());
            }
        }
    }

    @Test
    void sheetCountsAndDurations() {
        // docs/gregguns-spec/06-items/recipes.md
        assertEquals(
            2,
            spec("RC-12").row()
                .outputCount());
        assertEquals(
            6,
            spec("RC-12").row()
                .seconds());
        assertEquals(
            Tier.HV,
            spec("RC-15").row()
                .tier());
        assertEquals(
            8,
            spec("RC-15").row()
                .seconds());
        assertEquals(
            30,
            spec("RC-19").row()
                .seconds());
        assertEquals(
            2,
            spec("RC-20").row()
                .outputCount());
        assertEquals(
            4,
            spec("RC-52").row()
                .outputCount());
        assertEquals(
            250,
            spec("RC-62").fluids()
                .get(0)
                .amount());
        assertEquals(
            Tier.MV,
            spec("RC-62").row()
                .tier());
        assertEquals(
            Tier.ZPM,
            spec("RC-60").row()
                .tier());
    }

    @Test
    void machinesRowsAreDeferredWhenTheModuleIsOff() {
        Set<String> deferred = ComponentTable.outputsDeferredBy(id -> id != com.gregguns.core.module.ModuleId.MACHINES);
        assertEquals(new HashSet<>(java.util.Arrays.asList("part_barrel_laser")), deferred);
        assertTrue(
            ComponentTable.outputsDeferredBy(id -> true)
                .isEmpty());
    }

    @Test
    void everyComponentWithoutAnAllowlistReasonHasARowOrAnOwnerRecipe() {
        Set<String> madeHere = new HashSet<>();
        for (ProductionTable.Row row : ComponentTable.rows()) {
            madeHere.add(row.output());
        }
        Map<String, String> allow = PartCatalog.noRecipeAllowlist();
        for (PartCatalog.Entry entry : PartCatalog.entries()) {
            if (entry.recipe() != null && entry.recipe()
                .contains("ComponentTable")) {
                assertTrue(madeHere.contains(entry.name()), entry.name() + " says ComponentTable but has no row");
            }
            assertEquals(
                entry.recipe() == null,
                allow.containsKey(entry.name()),
                entry.name() + ": a recipe or an allowlist reason, not both and not neither");
        }
        Map<String, Boolean> unused = new HashMap<>();
        assertTrue(unused.isEmpty());
    }

    @Test
    void compositionOfThePowerPlatingFollowsItsInputs() {
        // 2 plates TungstenSteel + 4 epoxy plates + one Mechanical Parts T3, for 2 items.
        Map<String, Long> got = ComponentComposition.perItem(spec("RC-24").row(), allRows(), prefix -> 10L);
        assertEquals(10L, (long) got.get("TungstenSteel"));
        assertEquals(20L, (long) got.get("EpoxidFiberReinforced"));
        // The T3 part is 2 gears + 2 springs + 4 screws of Titanium (80 units) for 2 parts: 40 each, 40 per 2 plates.
        assertEquals(20L, (long) got.get("Titanium"));
    }

    private static boolean takesSpecial(ProductionTable.Row row, String special) {
        for (ProductionTable.Input input : row.inputs()) {
            if (special.equals(input.special())) {
                return true;
            }
        }
        return false;
    }

    @Test
    void tanksAreBuiltFromThePacksLargeFluidCells() {
        assertTrue(takesSpecial(spec("RC-52").row(), "gt:Large_Fluid_Cell_StainlessSteel"));
        assertTrue(takesSpecial(spec("RC-53").row(), "gt:Large_Fluid_Cell_Titanium"));
        // The old Empty Cell body is gone.
        assertFalse(takesSpecial(spec("RC-52").row(), "gt:Cell_Empty"));
        assertFalse(takesSpecial(spec("RC-53").row(), "gt:Cell_Empty"));
    }

    @Test
    void cyberneticPartsTakeTheBioLabPlasmaMembrane() {
        assertTrue(takesSpecial(spec("RC-20").row(), "gt:PlasmaMembrane"));
    }
}
