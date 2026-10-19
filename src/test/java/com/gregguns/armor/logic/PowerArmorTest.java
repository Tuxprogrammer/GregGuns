package com.gregguns.armor.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.gregguns.api.Tier;
import com.gregguns.combat.DamageType;
import com.gregguns.recipe.ArmorTable;
import com.gregguns.recipe.GateRule;
import com.gregguns.recipe.ProductionTable;

/**
 * The IV Power Armor and Exo Suit (M3 wave C) against armor-catalog.md sections 2, 5.9, 5.11 and 6, the hazard flag
 * table, the D4 protection numbers, and the IV recipe rows of recipes.md.
 */
class PowerArmorTest {

    private static final double EPS = 1e-9;

    private static final DamageType[] ORDER = { DamageType.PHYSICAL, DamageType.PROJECTILE, DamageType.EXPLOSION,
        DamageType.ENERGY, DamageType.FIRE, DamageType.ICE, DamageType.LIGHTNING, DamageType.POISON,
        DamageType.RADIATION };

    private static void assertMaterial(SetDef set, int durability, double[] totals, double pen, int[] vanilla) {
        ArmorMaterialDef m = set.material();
        assertEquals(durability, m.pieceDurability(), set.id() + " durability");
        for (int i = 0; i < ORDER.length; i++) {
            assertEquals(totals[i], m.total(ORDER[i]), EPS, set.id() + " " + ORDER[i]);
        }
        assertEquals(pen, m.penetrationResistance(), EPS, set.id() + " penetration resistance");
        for (ArmorSlot slot : set.pieces()
            .keySet()) {
            assertEquals(vanilla[slot.ordinal()], ArmorProtection.vanillaPoints(m, slot), set.id() + " bar " + slot);
        }
    }

    private static List<SetEvaluator.Worn> worn(SetDef set, boolean powered) {
        List<SetEvaluator.Worn> out = new ArrayList<>();
        for (PieceDef p : set.pieces()
            .values()) {
            out.add(new SetEvaluator.Worn(set, p, false, powered));
        }
        return out;
    }

    @Test
    void powerArmorMaterialIsTheT3PowerRow() {
        assertMaterial(
            ArmorCatalog.T3_POWER,
            4950,
            new double[] { 24, 24, 22, 20, 20, 20, 20, 15, 10 },
            0.25,
            new int[] { 6, 7, 6, 5 });
    }

    @Test
    void exoSuitMaterialIsTheT3ExoRow() {
        assertMaterial(
            ArmorCatalog.T3_EXO,
            4125,
            new double[] { 22, 22, 18, 18, 18, 18, 18, 10, 6 },
            0.15,
            new int[] { 6, 7, 6, 4 });
    }

    @Test
    void bothSetsAreIvElectricWithTheTechgunsCapacities() {
        assertEquals(Tier.IV, ArmorCatalog.T3_POWER.tier());
        assertEquals(Tier.IV, ArmorCatalog.T3_EXO.tier());
        assertEquals(
            PowerSpec.PowerKind.ELECTRIC,
            ArmorCatalog.T3_POWER.power()
                .kind());
        assertEquals(
            PowerSpec.PowerKind.ELECTRIC,
            ArmorCatalog.T3_EXO.power()
                .kind());
        assertEquals(
            3600,
            ArmorCatalog.T3_POWER.power()
                .capacityUnits());
        assertEquals(
            7200,
            ArmorCatalog.T3_EXO.power()
                .capacityUnits());
    }

    @Test
    void ivSetsCostTenEuPerTickPerWalkingPieceAndFitAnIvCharger() {
        for (SetDef set : new SetDef[] { ArmorCatalog.T3_POWER, ArmorCatalog.T3_EXO }) {
            PowerSpec power = set.power();
            double euPerTick = power.euPerUnit() * PowerRules.DRAIN_PER_PIECE_PER_SECOND / 20.0;
            assertEquals(10.0, euPerTick, EPS, set.id());
            // A full set walking draws 40 EU/t, far below one IV amp (8192 EU/t), so a charger never limits it.
            assertTrue(4 * euPerTick < 8192.0);
            // The full store charges in under a minute at one IV amp.
            double seconds = power.capacityEu() / 8192.0 / 20.0;
            assertTrue(seconds < 60.0, set.id() + " charges in " + seconds + " s");
        }
        assertEquals(
            720000.0,
            ArmorCatalog.T3_POWER.power()
                .capacityEu(),
            EPS);
        assertEquals(
            1440000.0,
            ArmorCatalog.T3_EXO.power()
                .capacityEu(),
            EPS);
    }

    @Test
    void powerArmorTotalsAreTheSpecTableWhilePowered() {
        BonusMap b = SetEvaluator.evaluate(worn(ArmorCatalog.T3_POWER, true))
            .bonuses();
        assertEquals(0.20, b.get(ArmorBonus.SPEED), EPS);
        assertEquals(0.24, b.get(ArmorBonus.JUMP), EPS);
        assertEquals(1.00, b.get(ArmorBonus.KNOCKBACK_RESISTANCE), EPS);
        assertEquals(0.20, b.get(ArmorBonus.BREAK_SPEED), EPS);
        assertEquals(5.0, b.get(ArmorBonus.EXTRA_HEALTH), EPS);
        assertEquals(1.0, b.get(ArmorBonus.STEP_ASSIST), EPS);
        assertEquals(0.2, b.get(ArmorBonus.FALL_REDUCTION), EPS);
        assertEquals(1.0, b.get(ArmorBonus.FREE_FALL_HEIGHT), EPS);
        assertEquals(0.0, b.get(ArmorBonus.COOLING_SYSTEM), EPS);
        assertEquals(0.0, b.get(ArmorBonus.OXYGEN_GEAR), EPS);
    }

    @Test
    void exoSuitTotalsAreTheSpecTableWhilePowered() {
        BonusMap b = SetEvaluator.evaluate(worn(ArmorCatalog.T3_EXO, true))
            .bonuses();
        assertEquals(0.80, b.get(ArmorBonus.SPEED), EPS);
        assertEquals(0.60, b.get(ArmorBonus.JUMP), EPS);
        assertEquals(0.80, b.get(ArmorBonus.KNOCKBACK_RESISTANCE), EPS);
        assertEquals(0.30, b.get(ArmorBonus.BREAK_SPEED), EPS);
        assertEquals(0.0, b.get(ArmorBonus.EXTRA_HEALTH), EPS);
        assertEquals(1.0, b.get(ArmorBonus.STEP_ASSIST), EPS);
        assertEquals(0.5, b.get(ArmorBonus.FALL_REDUCTION), EPS);
        assertEquals(5.0, b.get(ArmorBonus.FREE_FALL_HEIGHT), EPS);
    }

    @Test
    void bonusesNeedAPoweredChestplateButTheSetStillCounts() {
        for (SetDef set : new SetDef[] { ArmorCatalog.T3_POWER, ArmorCatalog.T3_EXO }) {
            SetEvaluator.Result unpowered = SetEvaluator.evaluate(worn(set, false));
            assertTrue(
                unpowered.bonuses()
                    .isEmpty(),
                set.id());
            assertTrue(
                unpowered.completeSets()
                    .contains(set.id()));
        }
    }

    @Test
    void protectionNeverDependsOnPowerAndAChestplateCarriesThirtyPercent() {
        // D4: a full set carries the material total; the chestplate share is 30% (7.2 of 24 physical).
        assertEquals(
            24.0,
            ArmorCatalog.T3_POWER.material()
                .total(DamageType.PROJECTILE),
            EPS);
        assertEquals(
            22.0,
            ArmorCatalog.T3_EXO.material()
                .total(DamageType.PROJECTILE),
            EPS);
        assertEquals(
            7.2,
            ArmorCatalog.T3_POWER.material()
                .total(DamageType.PHYSICAL) * 0.30,
            EPS);
    }

    @Test
    void hazardFlagsAreTheProposalTable() {
        for (SetDef set : new SetDef[] { ArmorCatalog.T3_POWER, ArmorCatalog.T3_EXO }) {
            for (PieceDef p : set.pieces()
                .values()) {
                assertTrue(
                    p.partialHazards()
                        .isEmpty(),
                    set.id() + " has no passive cover");
                assertTrue(
                    p.fullHazards()
                        .isEmpty(),
                    set.id());
                assertEquals(
                    EnumSet.of(HazardFlag.HEAT, HazardFlag.FROST, HazardFlag.ELECTRICAL),
                    p.poweredPartialHazards(),
                    set.id());
            }
        }
    }

    @Test
    void camosAndTextureLayoutsFollowTheSpec() {
        assertEquals(3, ArmorCatalog.T3_POWER.camoCount());
        assertEquals(SetDef.TextureLayout.SINGLE, ArmorCatalog.T3_POWER.layout());
        assertEquals(
            "gregguns:textures/models/armor/t3_power_dark2.png",
            ArmorCatalog.T3_POWER.layerTexture(ArmorSlot.LEGGINGS, 2));
        assertEquals(3, ArmorCatalog.T3_EXO.camoCount());
        assertEquals(SetDef.TextureLayout.SINGLE, ArmorCatalog.T3_EXO.layout());
        assertEquals(
            "gregguns:textures/models/armor/t3_exo_silver.png",
            ArmorCatalog.T3_EXO.layerTexture(ArmorSlot.LEGGINGS, 1));
        assertEquals(
            "gregguns:textures/models/armor/t3_exo.png",
            ArmorCatalog.T3_EXO.layerTexture(ArmorSlot.HELMET, 99),
            "an out-of-range camo falls back to 0 (ARM-12)");
    }

    @Test
    void powerArmorRowsMatchTheSheet() {
        assertRows(ArmorTable.T3_POWER, ArmorCatalog.T3_POWER, new int[] { 14, 20, 18, 10 });
        int[] pp = { 3, 5, 4, 2 };
        int[] circuits = { 2, 1, 1, 0 };
        int[] motors = { 0, 2, 2, 2 };
        int[] tungsten = { 1, 2, 2, 1 };
        for (int i = 0; i < 4; i++) {
            ProductionTable.Row row = ArmorTable.T3_POWER.get(i);
            assertEquals(pp[i], countMod(row, "part_power_plating"));
            assertEquals(circuits[i], countGt(row, "circuit", "Elite"));
            assertEquals(motors[i], countSpecial(row, "gt:Electric_Motor_IV"));
            assertEquals(tungsten[i], countGt(row, "plate", "TungstenSteel"));
        }
        assertEquals(1, countSpecial(ArmorTable.T3_POWER.get(0), "mc:glass_pane"));
    }

    @Test
    void exoRowsUpgradeTheMatchingAdvancedCombatPieceAtAnyDamage() {
        assertRows(ArmorTable.T3_EXO, ArmorCatalog.T3_EXO, new int[] { 10, 12, 12, 10 });
        int[] tungsten = { 1, 2, 2, 1 };
        for (int i = 0; i < 4; i++) {
            ProductionTable.Row row = ArmorTable.T3_EXO.get(i);
            ProductionTable.Input base = row.inputs()
                .get(0);
            assertEquals(ArmorCatalog.T3_COMBAT.pieceName(ArmorSlot.values()[i]), base.itemName());
            assertTrue(base.anyDamage(), "GGS-05-35");
            assertEquals(1, countMod(row, "part_mechanical_3"));
            assertEquals(1, countGt(row, "circuit", "Elite"));
            assertEquals(1, countSpecial(row, "gt:Electric_Motor_IV"));
            assertEquals(tungsten[i], countGt(row, "plate", "TungstenSteel"));
        }
    }

    @Test
    void ivRowsAreOnlyDeferredNeverInTheAlwaysOnList() {
        assertEquals(8, ArmorTable.IV_DEFERRED.size());
        for (ProductionTable.Row row : ArmorTable.IV_DEFERRED) {
            assertFalse(ArmorTable.MACHINE.contains(row), row.id());
        }
    }

    @Test
    void ivRowsMeetTheGateRuleAndRunAtIv() {
        Map<String, ProductionTable.Row> byOutput = new HashMap<>();
        for (ProductionTable.Row row : ArmorTable.MACHINE) {
            byOutput.put(row.output(), row);
        }
        for (ProductionTable.Row row : ArmorTable.IV_DEFERRED) {
            byOutput.put(row.output(), row);
        }
        for (ProductionTable.Row row : ArmorTable.IV_DEFERRED) {
            assertEquals(Tier.IV, row.tier(), row.id());
            assertTrue(GateRule.satisfied(row, byOutput, ProductionTable.GATE_EXEMPT), row.output());
            assertTrue(countGt(row, "plate", "TungstenSteel") > 0, row.id() + " names the tier gate plate");
        }
    }

    @Test
    void rowIdsDoNotCollide() {
        List<String> ids = new ArrayList<>();
        for (ProductionTable.Row row : ArmorTable.MACHINE) {
            ids.add(row.id());
        }
        for (ProductionTable.Row row : ArmorTable.IV_DEFERRED) {
            assertFalse(ids.contains(row.id()), row.id());
            ids.add(row.id());
        }
    }

    private static void assertRows(List<ProductionTable.Row> rows, SetDef set, int[] seconds) {
        assertEquals(4, rows.size());
        for (int i = 0; i < 4; i++) {
            ProductionTable.Row row = rows.get(i);
            assertEquals(set.pieceName(ArmorSlot.values()[i]), row.output());
            assertEquals(Tier.IV, row.tier());
            assertEquals(ProductionTable.Target.ASSEMBLER, row.target());
            assertEquals(i + 1, row.circuit());
            assertEquals(seconds[i], row.seconds());
        }
    }

    private static int countMod(ProductionTable.Row row, String name) {
        for (ProductionTable.Input in : row.inputs()) {
            if (in.isGregGuns() && name.equals(in.itemName())) {
                return in.count();
            }
        }
        return 0;
    }

    private static int countGt(ProductionTable.Row row, String prefix, String material) {
        for (ProductionTable.Input in : row.inputs()) {
            if (!in.isGregGuns() && in.special() == null
                && prefix.equals(in.prefix())
                && material.equals(in.material())) {
                return in.count();
            }
        }
        return 0;
    }

    private static int countSpecial(ProductionTable.Row row, String special) {
        for (ProductionTable.Input in : row.inputs()) {
            if (special.equals(in.special())) {
                return in.count();
            }
        }
        return 0;
    }
}
