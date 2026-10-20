package com.gregguns.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.gregguns.api.Tier;
import com.gregguns.item.ammo.AmmoTypes;

class ProductionTableTest {

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
    void rowsMatchTheSheetCountsAndDurations() {
        // docs/gregguns-spec/06-items/recipes.md, RC-05, RC-11, RC-28, RC-37, RC-44.
        assertEquals(1, row("RC-04").outputCount());
        assertEquals(5, row("RC-04").seconds());
        assertEquals(1, row("RC-05").outputCount());
        assertEquals(6, row("RC-05").seconds());
        assertEquals(2, row("RC-11").outputCount());
        assertEquals(5, row("RC-11").seconds());
        assertEquals(16, row("RC-28").outputCount());
        assertEquals(5, row("RC-28").seconds());
        assertEquals(8, row("RC-37").outputCount());
        assertEquals(4, row("RC-37").seconds());
        assertEquals(4, row("RC-44").outputCount());
        assertEquals(2, row("RC-44").seconds());
    }

    @Test
    void ironReceiverAndRifleBulletRowsMatchTheSheet() {
        // docs/gregguns-spec/06-items/recipes.md, RC-13 and RC-30.
        ProductionTable.Row receiver = row("RC-13");
        assertEquals("part_receiver_iron", receiver.output());
        assertEquals(1, receiver.outputCount());
        assertEquals(5, receiver.seconds());
        assertEquals(ProductionTable.Target.ASSEMBLER, receiver.target());
        assertEquals(
            3,
            receiver.inputs()
                .size());
        ProductionTable.Row rifle = row("RC-30");
        assertEquals("ammo_rifle_bullet", rifle.output());
        assertEquals(16, rifle.outputCount());
        assertEquals(6, rifle.seconds());
        assertEquals(Tier.LV, rifle.tier());
        assertEquals(ProductionTable.Target.AMMO_PRESS, rifle.target());
        assertEquals(
            "round.Steel",
            rifle.inputs()
                .get(1)
                .prefix() + "."
                + rifle.inputs()
                    .get(1)
                    .material());
        assertFalse(
            ProductionTable.GATE_EXEMPT.contains("ammo_rifle_bullet"),
            "the Steel rounds are the gate, so the rifle bullet is not exempt");
        assertTrue(GateRule.satisfied(rifle, ProductionTable.byOutput(), ProductionTable.GATE_EXEMPT));
    }

    @Test
    void everyRowIsLvSoTheRecipeCannotRunBelowItsTierExceptTheSteamStoneBullet() {
        for (ProductionTable.Row row : ProductionTable.ROWS) {
            // The SMG and Assault Rifle magazine families and the 40 mm grenade are MV (Aluminium gate, 0001), the
            // rocket is HV (stainless steel gate); everything else here is LV.
            String out = row.output();
            Tier expected = out.equals(ProductionTable.STONE_BULLET) ? Tier.STEAM
                : out.equals("ammo_advanced_bullet") || out.startsWith("mag_advanced")
                    || out.startsWith("mag_minigun_drum")
                        ? Tier.EV
                        : out.equals("ammo_rocket") || out.equals("ammo_high_caliber_round")
                            || out.startsWith("mag_as50")
                            || out.startsWith("mag_lmg") ? Tier.HV
                                : out.startsWith("mag_smg") || out.startsWith("mag_assault_rifle")
                                    || out.equals("ammo_40mm_grenade") ? Tier.MV : Tier.LV;
            assertEquals(expected, row.tier(), row.id());
        }
    }

    @Test
    void partsGoOnTheAssemblerAndAmmoGoOnTheAmmoMap() {
        // GGS-06-38: loose ammo and magazines are on gregguns.ammoPress only.
        assertEquals(ProductionTable.Target.ASSEMBLER, row("RC-05").target());
        assertEquals(ProductionTable.Target.ASSEMBLER, row("RC-11").target());
        assertEquals(ProductionTable.Target.AMMO_PRESS, row("RC-28").target());
        assertEquals(ProductionTable.Target.AMMO_PRESS, row("RC-37").target());
        assertEquals(ProductionTable.Target.AMMO_PRESS, row("RC-44").target());
    }

    @Test
    void ammoRowsNeedNoProgrammedCircuitBecauseTheMapIsSeparate() {
        for (ProductionTable.Row row : ProductionTable.ROWS) {
            if (row.target() == ProductionTable.Target.AMMO_PRESS) {
                assertEquals(0, row.circuit(), row.id());
            }
        }
    }

    @Test
    void bulletInputsAreGtRoundFoilAndGunpowderOnly() {
        // GGS-06-38: no casing item of our own, slugs and cases come from GT parts.
        Set<String> parts = new HashSet<>();
        for (ProductionTable.Input input : row("RC-28").inputs()) {
            assertFalse(input.isGregGuns(), "the bullet takes GT parts only");
            parts.add(input.prefix() + "." + input.material());
        }
        Set<String> expected = new HashSet<>();
        expected.add("foil.Brass");
        expected.add("round.Lead");
        expected.add("dust.Gunpowder");
        assertEquals(expected, parts);
    }

    @Test
    void bulletRecipeUsesOneFoilAndOneRoundPerBullet() {
        ProductionTable.Row bullets = row("RC-28");
        assertEquals(
            bullets.outputCount(),
            bullets.inputs()
                .get(0)
                .count());
        assertEquals(
            bullets.outputCount(),
            bullets.inputs()
                .get(1)
                .count());
    }

    @Test
    void loadedMagazineRunUsesTheMagazineTypesRoundCount() {
        int perMagazine = AmmoTypes.NINE_MM_PISTOL.roundsPerMagazine();
        ProductionTable.Row loaded = row("RC-44");
        assertEquals(
            ProductionTable.LOADED_PER_RUN,
            loaded.inputs()
                .get(0)
                .count());
        assertEquals(
            ProductionTable.LOADED_PER_RUN * perMagazine,
            loaded.inputs()
                .get(1)
                .count());
        assertEquals(perMagazine, ProductionTable.PISTOL_REFILL.roundCount());
    }

    @Test
    void refillIsOneEmptyMagazinePlusThreeBullets() {
        // RC-51: mag_9mm_pistol_empty + ammo_9mm_bullet x3.
        ProductionTable.Refill refill = ProductionTable.PISTOL_REFILL;
        assertEquals("mag_9mm_pistol", refill.output());
        assertEquals("mag_9mm_pistol_empty", refill.emptyMagazine());
        assertEquals("ammo_9mm_bullet", refill.round());
        assertEquals(3, refill.roundCount());
    }

    @Test
    void stoneBulletPressRowRunsOnTheSteamPressAndIsBronzeGated() {
        // RC-27b: 24 bullets from cobblestone x3, gunpowder x1 and a Bronze nugget, ULV, Steam Ammo Press.
        ProductionTable.Row stone = row("RC-27b");
        assertEquals("ammo_stone_bullet", stone.output());
        assertEquals(24, stone.outputCount());
        assertEquals(Tier.STEAM, stone.tier());
        assertEquals(ProductionTable.Target.AMMO_PRESS, stone.target());
        assertEquals(5, stone.seconds());
        assertTrue(
            stone.inputs()
                .size() <= 4,
            "the Steam press has 4 input slots");
        assertEquals(
            "cobblestone",
            stone.inputs()
                .get(0)
                .oreName());
        assertEquals(
            3,
            stone.inputs()
                .get(0)
                .count());
        assertTrue(
            GateRule.satisfied(stone, ProductionTable.byOutput(), ProductionTable.GATE_EXEMPT),
            "the Bronze nugget is the gate");
        assertTrue(
            ProductionTable.pressRowsFor(com.gregguns.machine.AmmoPressTier.STEAM)
                .contains(stone),
            "the Steam press can run it");
    }

    @Test
    void inputsAreUniquePerMapAndCircuit() {
        // GGS-06-37: (map, inputs, circuit) is unique.
        Set<String> seen = new HashSet<>();
        for (ProductionTable.Row row : ProductionTable.ROWS) {
            StringBuilder key = new StringBuilder(row.target() + "|" + row.circuit());
            for (ProductionTable.Input input : row.inputs()) {
                key.append('|')
                    .append(
                        input.isGregGuns() ? input.itemName()
                            : input.oreName() != null ? "ore:" + input.oreName()
                                : input.prefix() + "." + input.material())
                    .append('x')
                    .append(input.count());
            }
            assertTrue(seen.add(key.toString()), "duplicate recipe key for " + row.id());
        }
    }

    @Test
    void everyGregGunsInputIsMadeByARowOfTheTable() {
        Map<String, ProductionTable.Row> made = ProductionTable.byOutput();
        for (ProductionTable.Row row : ProductionTable.ROWS) {
            for (ProductionTable.Input input : row.inputs()) {
                if (input.isGregGuns() && !ProductionTable.CRAFTED_PARTS.contains(input.itemName())
                    && !madeByComponentTable(input.itemName())) {
                    assertNotNull(made.get(input.itemName()), row.id() + " input " + input.itemName());
                }
            }
        }
    }

    /** Parts such as Mechanical Parts T3 (RC-12) are made by the component table, not the ammo table. */
    private static boolean madeByComponentTable(String item) {
        for (ProductionTable.Row row : ComponentTable.rows()) {
            if (row.output()
                .equals(item)) {
                return true;
            }
        }
        return false;
    }

    @Test
    void everyAmmoRowRunsTheMultiplierTimesLongerAndOtherRowsDoNot() {
        assertEquals(4, ProductionTable.AMMO_DURATION_MULTIPLIER, "the owner asked for 4 times");
        int ammoRows = 0;
        for (ProductionTable.Row row : ProductionTable.ROWS) {
            if (row.target() == ProductionTable.Target.AMMO_PRESS) {
                assertTrue(row.isAmmo(), row.id());
                assertEquals(row.seconds() * 20 * ProductionTable.AMMO_DURATION_MULTIPLIER, row.ticks(), row.id());
                ammoRows++;
            } else {
                assertEquals(row.seconds() * 20, row.ticks(), row.id() + " is not an ammo row");
            }
        }
        assertTrue(ammoRows > 0);
        // The Assembly Line ammo rows (AmmoLineTable) take the same multiplier; the sheet seconds stay the base value.
        for (ProductionTable.Row row : AmmoLineTable.rows()) {
            assertTrue(row.isAmmo(), row.id());
            assertEquals(row.seconds() * 20 * ProductionTable.AMMO_DURATION_MULTIPLIER, row.ticks(), row.id());
        }
        // A fluid copy of a row keeps the flag.
        ProductionTable.Row bath = AmmoLineTable.rows()
            .get(0)
            .withFluid(ComponentTable.FluidInput.molten("Lead", 144));
        assertTrue(bath.isAmmo());
    }
}
