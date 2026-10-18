package com.gregguns.armor.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import com.gregguns.api.Tier;
import com.gregguns.combat.DamageType;

/**
 * The T2 and T3 sets and steam armor against armor-catalog.md sections 2, 5 and 6, the hazard flag table, and the
 * powered-bonus rule.
 */
class ArmorTiersTest {

    private static final double EPS = 1e-9;

    private static final DamageType[] ORDER = { DamageType.PHYSICAL, DamageType.PROJECTILE, DamageType.EXPLOSION,
        DamageType.ENERGY, DamageType.FIRE, DamageType.ICE, DamageType.LIGHTNING, DamageType.POISON,
        DamageType.RADIATION };

    /** One row of the armor-catalog section 2 table: durability, nine totals, penetration resistance, vanilla bar. */
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

    @Test
    void t2CombatMaterialIsTheSpecRow() {
        assertMaterial(
            ArmorCatalog.T2_COMBAT,
            990,
            new double[] { 19, 19, 14.25, 14.25, 14.25, 14.25, 14.25, 0, 0 },
            0.0,
            new int[] { 5, 6, 5, 4 });
    }

    @Test
    void t2CommandoMaterialIsTheSpecRow() {
        assertMaterial(
            ArmorCatalog.T2_COMMANDO,
            990,
            new double[] { 18, 18, 15, 15, 15, 15, 15, 10, 5 },
            0.0,
            new int[] { 5, 5, 5, 4 });
    }

    @Test
    void t2RiotMaterialIsTheSpecRow() {
        assertMaterial(
            ArmorCatalog.T2_RIOT,
            1320,
            new double[] { 20, 20, 18, 18, 18, 17, 17, 16, 16 },
            0.10,
            new int[] { 5, 6, 5, 4 });
    }

    @Test
    void t3CombatMaterialIsTheSpecRow() {
        assertMaterial(
            ArmorCatalog.T3_COMBAT,
            3300,
            new double[] { 22, 22, 18, 18, 18, 18, 18, 10, 6 },
            0.15,
            new int[] { 6, 7, 6, 4 });
    }

    @Test
    void hevMaterialIsTheSpecRowCalledT3Miner() {
        assertMaterial(
            ArmorCatalog.T3_HEV,
            4125,
            new double[] { 21, 21, 21, 21, 21, 21, 21, 21, 21 },
            0.15,
            new int[] { 5, 6, 5, 4 });
    }

    @Test
    void steamMaterialIsTheSpecRowCalledT1Steam() {
        assertMaterial(
            ArmorCatalog.STEAM,
            2750,
            new double[] { 20, 20, 18, 16, 18, 16, 16, 10, 6 },
            0.10,
            new int[] { 5, 6, 5, 4 });
    }

    @Test
    void theBeretIsOneHelmetWithTheT2BeretRow() {
        SetDef beret = ArmorCatalog.T2_BERET;
        assertEquals(1, beret.pieceCount());
        assertEquals("t2_beret", beret.pieceName(ArmorSlot.HELMET));
        assertEquals(
            825,
            beret.material()
                .pieceDurability());
        assertEquals(
            8.0,
            beret.material()
                .total(DamageType.PHYSICAL),
            EPS);
        assertEquals(
            6.0,
            beret.material()
                .total(DamageType.FIRE),
            EPS);
        assertEquals(2, ArmorProtection.vanillaPoints(beret.material(), ArmorSlot.HELMET));
    }

    @Test
    void setsHaveTheTiersOfTheTierMapAndDesignRecord0010() {
        assertEquals(Tier.MV, ArmorCatalog.T2_COMBAT.tier());
        assertEquals(Tier.MV, ArmorCatalog.T2_COMMANDO.tier());
        assertEquals(Tier.MV, ArmorCatalog.T2_RIOT.tier());
        assertEquals(Tier.MV, ArmorCatalog.T2_BERET.tier());
        assertEquals(Tier.EV, ArmorCatalog.T3_COMBAT.tier());
        assertEquals(Tier.EV, ArmorCatalog.STEAM.tier());
        assertEquals(Tier.EV, ArmorCatalog.T3_HEV.tier());
    }

    private static List<SetEvaluator.Worn> worn(SetDef set, boolean powered) {
        List<SetEvaluator.Worn> out = new ArrayList<>();
        for (PieceDef p : set.pieces()
            .values()) {
            out.add(new SetEvaluator.Worn(set, p, false, powered));
        }
        return out;
    }

    private static void assertTotals(BonusMap b, double speed, double jump, double kb, double mine, double waterMine) {
        assertEquals(speed, b.get(ArmorBonus.SPEED), EPS, "speed");
        assertEquals(jump, b.get(ArmorBonus.JUMP), EPS, "jump");
        assertEquals(kb, b.get(ArmorBonus.KNOCKBACK_RESISTANCE), EPS, "knockback");
        assertEquals(mine, b.get(ArmorBonus.BREAK_SPEED), EPS, "mining");
        assertEquals(waterMine, b.get(ArmorBonus.BREAK_SPEED_WATER), EPS, "underwater mining");
    }

    @Test
    void fullSetTotalsAreTheSection6Table() {
        BonusMap combat = SetEvaluator.evaluate(worn(ArmorCatalog.T2_COMBAT, false))
            .bonuses();
        assertTotals(combat, 0.40, 0.10, 0.60, 0, 0);

        BonusMap commando = SetEvaluator.evaluate(worn(ArmorCatalog.T2_COMMANDO, false))
            .bonuses();
        assertTotals(commando, 0.40, 0.10, 0.40, 0, 5.0);
        assertEquals(0.20, commando.get(ArmorBonus.GUN_ACCURACY), EPS);
        assertEquals(1.0, commando.get(ArmorBonus.OXYGEN_GEAR), EPS);
        assertEquals(0.2, commando.get(ArmorBonus.FALL_REDUCTION), EPS);
        assertEquals(1.0, commando.get(ArmorBonus.FREE_FALL_HEIGHT), EPS);

        assertTotals(
            SetEvaluator.evaluate(worn(ArmorCatalog.T2_RIOT, false))
                .bonuses(),
            0.50,
            0.15,
            0.70,
            0,
            0);
        assertTotals(
            SetEvaluator.evaluate(worn(ArmorCatalog.T3_COMBAT, false))
                .bonuses(),
            0.40,
            0.10,
            0.80,
            0,
            0);
        assertEquals(
            0.10,
            SetEvaluator.evaluate(worn(ArmorCatalog.T2_BERET, false))
                .bonuses()
                .get(ArmorBonus.SPEED),
            EPS);
    }

    @Test
    void steamTotalsAreTheSpecTableWhilePowered() {
        BonusMap b = SetEvaluator.evaluate(worn(ArmorCatalog.STEAM, true))
            .bonuses();
        assertTotals(b, 0.20, 0.24, 0.80, 0.20, 0);
        assertEquals(5.0, b.get(ArmorBonus.EXTRA_HEALTH), EPS);
        assertEquals(1.0, b.get(ArmorBonus.STEP_ASSIST), EPS);
        assertEquals(0.2, b.get(ArmorBonus.FALL_REDUCTION), EPS);
        assertEquals(1.0, b.get(ArmorBonus.FREE_FALL_HEIGHT), EPS);
    }

    @Test
    void hevTotalsAreTheSpecTableWhilePowered() {
        BonusMap b = SetEvaluator.evaluate(worn(ArmorCatalog.T3_HEV, true))
            .bonuses();
        assertTotals(b, 0.40, 0.51, 0.60, 0.40, 5.0);
        assertEquals(5.0, b.get(ArmorBonus.SPEED_WATER), EPS);
        assertEquals(1.0, b.get(ArmorBonus.OXYGEN_GEAR), EPS);
        assertEquals(1.0, b.get(ArmorBonus.COOLING_SYSTEM), EPS);
        assertEquals(1.0, b.get(ArmorBonus.STEP_ASSIST), EPS);
        assertEquals(0.5, b.get(ArmorBonus.FALL_REDUCTION), EPS);
        assertEquals(3.0, b.get(ArmorBonus.FREE_FALL_HEIGHT), EPS);
        assertEquals(
            0.30,
            ArmorCatalog.T3_HEV.pieces()
                .get(ArmorSlot.HELMET)
                .poweredBonuses()
                .get(ArmorBonus.JUMP),
            EPS);
    }

    @Test
    void poweredBonusesNeedAPoweredChestplate() {
        // Unpowered: a steam or HEV set gives no bonus at all (every unpowered value is 0 in Techguns).
        assertTrue(
            SetEvaluator.evaluate(worn(ArmorCatalog.STEAM, false))
                .bonuses()
                .isEmpty());
        assertTrue(
            SetEvaluator.evaluate(worn(ArmorCatalog.T3_HEV, false))
                .bonuses()
                .isEmpty());
        // The set is still detected as complete without power (armor values never depend on power).
        assertTrue(
            SetEvaluator.evaluate(worn(ArmorCatalog.STEAM, false))
                .completeSets()
                .contains("steam"));
    }

    @Test
    void hazardFlagsAreTheProposalTable() {
        for (SetDef set : new SetDef[] { ArmorCatalog.T2_COMBAT, ArmorCatalog.T2_BERET, ArmorCatalog.T3_COMBAT }) {
            for (PieceDef p : set.pieces()
                .values()) {
                assertTrue(
                    p.partialHazards()
                        .isEmpty(),
                    set.id());
                assertTrue(
                    p.fullHazards()
                        .isEmpty(),
                    set.id());
            }
        }
        for (PieceDef p : ArmorCatalog.T2_COMMANDO.pieces()
            .values()) {
            assertEquals(java.util.Collections.singleton(HazardFlag.ELECTRICAL), p.partialHazards());
        }
        for (PieceDef p : ArmorCatalog.T2_RIOT.pieces()
            .values()) {
            assertEquals(java.util.Collections.singleton(HazardFlag.RADIOLOGICAL), p.partialHazards());
        }
        for (PieceDef p : ArmorCatalog.STEAM.pieces()
            .values()) {
            assertEquals(java.util.Collections.singleton(HazardFlag.HEAT), p.partialHazards());
            assertTrue(
                p.poweredPartialHazards()
                    .isEmpty());
        }
        for (PieceDef p : ArmorCatalog.T3_HEV.pieces()
            .values()) {
            assertEquals(
                java.util.EnumSet.of(HazardFlag.BIOLOGICAL, HazardFlag.GAS, HazardFlag.RADIOLOGICAL),
                p.partialHazards());
            assertEquals(
                java.util.EnumSet.of(HazardFlag.HEAT, HazardFlag.FROST, HazardFlag.ELECTRICAL),
                p.poweredPartialHazards());
        }
    }

    @Test
    void noNewSetFullyCoversAnythingOnItsOwn() {
        // Only the masks cover a hazard alone (GGS-05-09); armor sets are partial so they need the full-set rule.
        for (SetDef set : ArmorCatalog.ALL) {
            for (PieceDef p : set.pieces()
                .values()) {
                assertTrue(
                    p.fullHazards()
                        .isEmpty(),
                    set.id());
            }
        }
    }

    @Test
    void commandoHasNoRepairAndOthersMatchTheCatalogRatios() {
        for (PieceDef p : ArmorCatalog.T2_COMMANDO.pieces()
            .values()) {
            assertNull(p.repair());
        }
        PieceDef legs = ArmorCatalog.T2_COMBAT.pieces()
            .get(ArmorSlot.LEGGINGS);
        assertEquals(
            3,
            legs.repair()
                .totalMats());
        assertEquals(
            1.0 / 3.0,
            legs.repair()
                .metalRatio(),
            EPS);
        assertEquals(
            "Aluminium",
            legs.repair()
                .metalMaterial());
        assertEquals(
            "part_treated_leather",
            ArmorCatalog.T2_RIOT.pieces()
                .get(ArmorSlot.BOOTS)
                .repair()
                .clothItem());
        // Steam armor is repaired with its own plating only (metal ratio 1.0 of plating in Techguns).
        RepairSpec steam = ArmorCatalog.STEAM.pieces()
            .get(ArmorSlot.CHESTPLATE)
            .repair();
        assertEquals(4, steam.totalMats());
        assertNull(steam.metalPrefix());
        assertEquals("part_steam_armor_plating", steam.clothItem());
    }

    @Test
    void singleSheetSetsPointAtOneFilePerCamo() {
        assertEquals(
            "gregguns:textures/models/armor/steam_rusty.png",
            ArmorCatalog.STEAM.layerTexture(ArmorSlot.LEGGINGS, 1));
        assertEquals(
            "gregguns:textures/models/armor/t2_beret_green.png",
            ArmorCatalog.T2_BERET.layerTexture(ArmorSlot.HELMET, 2));
        assertEquals(
            "gregguns:textures/models/armor/t3_hev_2.png",
            ArmorCatalog.T3_HEV.layerTexture(ArmorSlot.LEGGINGS, 0));
    }

    private static boolean exists(String path) {
        return ArmorTiersTest.class.getResource("/assets/gregguns/" + path) != null;
    }

    private static Properties lang() throws IOException {
        Properties p = new Properties();
        try (InputStream in = ArmorTiersTest.class.getResourceAsStream("/assets/gregguns/lang/en_US.lang")) {
            assertNotNull(in);
            p.load(new java.io.InputStreamReader(in, StandardCharsets.UTF_8));
        }
        return p;
    }

    @Test
    void everyNewCamoHasItsTextureAndName() throws IOException {
        Properties lang = lang();
        for (SetDef set : ArmorCatalog.ALL) {
            for (ArmorSlot slot : set.pieces()
                .keySet()) {
                for (int camo = 0; camo < set.camoCount(); camo++) {
                    String path = set.layerTexture(slot, camo)
                        .replace("gregguns:", "");
                    assertTrue(exists(path), path);
                }
                assertTrue(exists("textures/items/" + set.pieceName(slot) + ".png"), set.pieceName(slot));
            }
            if (set.camoCount() > 1) {
                for (int camo = 0; camo < set.camoCount(); camo++) {
                    assertNotNull(lang.getProperty("item.gregguns." + set.id() + ".camoname." + camo), set.id());
                }
            }
        }
        assertFalse(
            lang.getProperty("gregguns.armor.bonus.speed_water")
                .isEmpty());
    }

    // --- PowerRules ---------------------------------------------------------------------------------------------

    @Test
    void walkingDrainsOnePerPieceOncePerSecond() {
        assertEquals(4, PowerRules.drainThisTick(4, true, 40L));
        assertEquals(0, PowerRules.drainThisTick(4, true, 41L));
        assertEquals(0, PowerRules.drainThisTick(4, false, 40L));
        assertEquals(1, PowerRules.drainThisTick(1, true, 0L));
    }

    @Test
    void spendingNeverGoesBelowZero() {
        assertEquals(95, PowerRules.after(100, 5));
        assertEquals(0, PowerRules.after(3, 5));
        assertEquals(7, PowerRules.after(7, -2));
        assertTrue(PowerRules.isPowered(1));
        assertFalse(PowerRules.isPowered(0));
    }

    @Test
    void onlyFallsLongerThanThreeBlocksCostPower() {
        assertFalse(PowerRules.fallCosts(3.0));
        assertTrue(PowerRules.fallCosts(3.01));
        assertEquals(5, PowerRules.JUMP_COST);
        assertEquals(5, PowerRules.FALL_COST);
        assertEquals(1, PowerRules.COOLING_COST);
    }

    @Test
    void movingNeedsMoreThanAThousandthOfABlockPerTick() {
        assertFalse(PowerRules.isMoving(0.0, 0.0));
        assertFalse(PowerRules.isMoving(0.0009, 0.0));
        assertTrue(PowerRules.isMoving(0.0, 0.0011));
    }

    @Test
    void electricUnitsRoundDownAndMatchTheMechArmorRate() {
        assertEquals(10, PowerRules.unitsFromEu(405.0, 40.0));
        assertEquals(0, PowerRules.unitsFromEu(39.9, 40.0));
        assertEquals(0, PowerRules.unitsFromEu(100.0, 0.0));
        // 40 EU per unit: one piece walking costs 40 EU per second = 2 EU/t, GT Mech Armor's rate.
        PowerSpec hev = ArmorCatalog.T3_HEV.power();
        assertEquals(PowerSpec.PowerKind.ELECTRIC, hev.kind());
        assertEquals(2.0, hev.euPerUnit() * PowerRules.DRAIN_PER_PIECE_PER_SECOND / 20.0, EPS);
        assertEquals(400000.0, hev.capacityEu(), EPS);
        assertEquals(
            3600,
            ArmorCatalog.STEAM.power()
                .capacityUnits());
        assertEquals(
            PowerSpec.PowerKind.STEAM,
            ArmorCatalog.STEAM.power()
                .kind());
        assertEquals(
            PowerSpec.PowerKind.NONE,
            ArmorCatalog.T3_COMBAT.power()
                .kind());
    }

    @Test
    void aSteamChestplateLastsFifteenMinutesOfWalkingInAFullSet() {
        // 3600 units, 4 pieces drain 4 per second: 900 seconds.
        int units = 3600;
        int seconds = 0;
        while (units > 0) {
            units = PowerRules.after(units, PowerRules.drainThisTick(4, true, 20L));
            seconds++;
        }
        assertEquals(900, seconds);
    }
}
