package com.gregguns.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import com.gregguns.api.Tier;
import com.gregguns.gun.def.GunDefinitions;

/** Row ids are unique across every recipe table, and a row's fluid inputs plumb through the {@code Row} builder. */
class RowIdsAndFluidsTest {

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
        // Gun Assembly Line rows (specs, not GunDefinition rows): Nether Blaster, Nuke Launcher, Nuclear Death Ray.
        all.addAll(GunLineTable.rows());
        return all;
    }

    @Test
    void noRowIdIsUsedTwoWaysAcrossAllRecipeTables() {
        Set<String> seen = new HashSet<>();
        Set<String> clashes = new TreeSet<>();
        for (ProductionTable.Row row : allRows()) {
            if (!seen.add(row.id())) {
                clashes.add(row.id());
            }
        }
        assertTrue(clashes.isEmpty(), "row ids used by more than one row: " + clashes);
    }

    @Test
    void aRowWithoutFluidHasNone() {
        ProductionTable.Row row = ProductionTable.Row
            .assembler("T-1", "x", 1, Tier.LV, 5, 1, ProductionTable.Input.gt("plate", "Steel", 1));
        assertTrue(
            row.fluids()
                .isEmpty());
    }

    @Test
    void moltenAndFluidReturnCopiesCarryingTheFluid() {
        ProductionTable.Row base = ProductionTable.Row
            .assembler("T-2", "x", 1, Tier.EV, 5, 1, ProductionTable.Input.gt("plate", "Titanium", 2));
        ProductionTable.Row with = base.molten("SolderingAlloy", 144)
            .fluid("Lubricant", 250);
        assertTrue(
            base.fluids()
                .isEmpty(),
            "the original row is unchanged");
        assertEquals(
            2,
            with.fluids()
                .size());
        assertEquals(
            "SolderingAlloy",
            with.fluids()
                .get(0)
                .material());
        assertEquals(
            ComponentTable.FluidKind.MOLTEN,
            with.fluids()
                .get(0)
                .kind());
        assertEquals(
            144,
            with.fluids()
                .get(0)
                .amount());
        assertEquals(
            ComponentTable.FluidKind.FLUID,
            with.fluids()
                .get(1)
                .kind());
        assertEquals(base.id(), with.id());
        assertEquals(
            base.inputs()
                .size(),
            with.inputs()
                .size());
        assertEquals(base.seconds(), with.seconds());
        assertEquals(base.tier(), with.tier());
        assertEquals(base.circuit(), with.circuit());
    }

    @Test
    void sheetSolderRowsCarryTheirAmounts() {
        String[] ev = { "laser_rifle", "pulse_rifle", "tesla_rifle", "sonic_rifle" };
        String[] iv = { "blaster_rifle", "deatomizer_pistol", "scatter_laser_rifle" };
        for (String gun : ev) {
            assertSolder(gun, 144);
        }
        for (String gun : iv) {
            assertSolder(gun, 288);
        }
        assertSame(
            0,
            GunDefinitions.byName("pdw")
                .assembly()
                .fluids()
                .size());
    }

    private static void assertSolder(String gun, int amount) {
        ProductionTable.Row row = GunDefinitions.byName(gun)
            .assembly();
        assertEquals(
            1,
            row.fluids()
                .size(),
            gun);
        assertEquals(
            "SolderingAlloy",
            row.fluids()
                .get(0)
                .material(),
            gun);
        assertEquals(
            ComponentTable.FluidKind.MOLTEN,
            row.fluids()
                .get(0)
                .kind(),
            gun);
        assertEquals(
            amount,
            row.fluids()
                .get(0)
                .amount(),
            gun);
    }

    @Test
    void m5SheetFluidsCarryTheirAmounts() {
        ProductionTable.Row alien = GunDefinitions.byName("alien_blaster")
            .assembly();
        assertEquals(
            2,
            alien.fluids()
                .size());
        assertEquals(
            576,
            alien.fluids()
                .get(0)
                .amount());
        assertEquals(
            "Lubricant",
            alien.fluids()
                .get(1)
                .material());
        assertEquals(
            250,
            alien.fluids()
                .get(1)
                .amount());
        ProductionTable.Row grim = GunDefinitions.byName("grim_reaper")
            .assembly();
        assertEquals(
            1,
            grim.fluids()
                .size());
        assertEquals(
            "Lubricant",
            grim.fluids()
                .get(0)
                .material());
        assertEquals(
            250,
            grim.fluids()
                .get(0)
                .amount());
    }
}
