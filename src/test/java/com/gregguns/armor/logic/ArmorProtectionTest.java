package com.gregguns.armor.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.gregguns.combat.DamageModel;
import com.gregguns.combat.DamageType;

/** Protection math against the tables in docs/techguns-spec/05-armor/armor-catalog.md section 2. */
class ArmorProtectionTest {

    private static final double EPS = 1e-9;

    private static ArmorMaterialDef combat() {
        return ArmorCatalog.T1_COMBAT.material();
    }

    @Test
    void slotSharesAddUpToOne() {
        double sum = 0;
        for (ArmorSlot s : ArmorSlot.values()) {
            sum += s.share();
        }
        assertEquals(1.0, sum, EPS);
    }

    @Test
    void t1CombatPieceValuesAreTotalTimesSlotShare() {
        // Table row T1_COMBAT: PHYS 15, ENERGY 11.25.
        assertEquals(3.75, combat().pieceValue(DamageType.PHYSICAL, ArmorSlot.HELMET), EPS);
        assertEquals(4.5, combat().pieceValue(DamageType.PHYSICAL, ArmorSlot.CHESTPLATE), EPS);
        assertEquals(3.75, combat().pieceValue(DamageType.PHYSICAL, ArmorSlot.LEGGINGS), EPS);
        assertEquals(3.0, combat().pieceValue(DamageType.PHYSICAL, ArmorSlot.BOOTS), EPS);
        assertEquals(11.25, combat().total(DamageType.ENERGY), EPS);
        assertEquals(0.0, combat().total(DamageType.POISON), EPS);
        assertEquals(0.0, combat().total(DamageType.RADIATION), EPS);
    }

    @Test
    void uniformMaterialFollowsTheTechgunsConstructor() {
        ArmorMaterialDef m = ArmorMaterialDef.uniform("x", 60, 13.0);
        assertEquals(13.0, m.total(DamageType.PHYSICAL), EPS);
        assertEquals(13.0, m.total(DamageType.PROJECTILE), EPS);
        for (DamageType t : new DamageType[] { DamageType.FIRE, DamageType.EXPLOSION, DamageType.ENERGY, DamageType.ICE,
            DamageType.LIGHTNING }) {
            assertEquals(9.75, m.total(t), EPS, t.name());
        }
        assertEquals(0.0, m.total(DamageType.POISON), EPS);
        assertEquals(0.0, m.total(DamageType.UNRESISTABLE), EPS);
    }

    @Test
    void durabilityIsThirteenPointSevenFiveTimesBase() {
        // Table: baseDur 60, 72, 96, 240, 360 give 825, 990, 1320, 3300, 4950.
        int[][] rows = { { 60, 825 }, { 72, 990 }, { 96, 1320 }, { 240, 3300 }, { 360, 4950 }, { 200, 2750 },
            { 300, 4125 } };
        for (int[] row : rows) {
            assertEquals(
                row[1],
                ArmorMaterialDef.uniform("x", row[0], 1.0)
                    .pieceDurability(),
                "base " + row[0]);
        }
        for (SetDef set : java.util.Arrays
            .asList(ArmorCatalog.T1_SCOUT, ArmorCatalog.T1_MINER, ArmorCatalog.T1_COMBAT)) {
            assertEquals(
                825,
                set.material()
                    .pieceDurability(),
                set.id());
        }
    }

    @Test
    void vanillaPointsMatchTheTable() {
        // Table, last column: T1_COMBAT 4/5/4/3, T1_SCOUT and T1_MINER 3/4/3/3.
        assertPoints(combat(), 4, 5, 4, 3);
        assertPoints(ArmorCatalog.T1_SCOUT.material(), 3, 4, 3, 3);
        assertPoints(ArmorCatalog.T1_MINER.material(), 3, 4, 3, 3);
        // Other rows of the table, to pin the rounding rule (half up) for the sets later waves add.
        assertPoints(ArmorMaterialDef.uniform("t3_power", 360, 24), 6, 7, 6, 5);
        assertPoints(ArmorMaterialDef.uniform("t3_combat", 240, 22), 6, 7, 6, 4);
        assertPoints(ArmorMaterialDef.uniform("t2_commando", 72, 18), 5, 5, 5, 4);
        assertPoints(ArmorMaterialDef.uniform("t1_steam", 200, 20), 5, 6, 5, 4);
    }

    private static void assertPoints(ArmorMaterialDef m, int h, int c, int l, int b) {
        assertEquals(h, ArmorProtection.vanillaPoints(m, ArmorSlot.HELMET), m.id() + " helmet");
        assertEquals(c, ArmorProtection.vanillaPoints(m, ArmorSlot.CHESTPLATE), m.id() + " chest");
        assertEquals(l, ArmorProtection.vanillaPoints(m, ArmorSlot.LEGGINGS), m.id() + " legs");
        assertEquals(b, ArmorProtection.vanillaPoints(m, ArmorSlot.BOOTS), m.id() + " boots");
    }

    @Test
    void fullSetTotalEqualsTheMaterialTotal() {
        for (SetDef set : ArmorCatalog.ALL) {
            for (DamageType t : DamageType.values()) {
                double expected = t.bypassesArmor() ? 0.0
                    : set.material()
                        .total(t);
                assertEquals(
                    expected,
                    ArmorProtection.total(set.material(), t, ArmorSlot.values()),
                    EPS,
                    set.id() + " " + t);
            }
        }
    }

    @Test
    void fullSetRatioSumGivesTheD4Reduction() {
        // Forge adds the four ratios; 15 points absorb 15/25 of the armored share, as vanillaArmorFactor says.
        double sum = 0;
        for (ArmorSlot s : ArmorSlot.values()) {
            sum += ArmorProtection.absorbRatio(combat(), s, DamageType.PROJECTILE, 0, 825);
        }
        assertEquals(15.0 / 25.0, sum, EPS);
        assertEquals(1.0 - sum, DamageModel.vanillaArmorFactor(15.0), EPS);
        // 8 damage at pen 0: 3.2 (the combat.penetration worked figure for 15 points).
        assertEquals(3.2, 8.0 * DamageModel.vanillaArmorFactor(15.0), 1e-9);
    }

    @Test
    void anUnresistableHitSeesNoArmor() {
        assertEquals(0.0, ArmorProtection.pieceArmor(combat(), ArmorSlot.CHESTPLATE, DamageType.UNRESISTABLE, 0, 825));
        assertEquals(0.0, ArmorProtection.total(combat(), DamageType.UNRESISTABLE, ArmorSlot.values()));
    }

    @Test
    void aPieceWithOneDurabilityPointLeftIsBroken() {
        assertFalse(ArmorProtection.isBroken(0, 825));
        assertFalse(ArmorProtection.isBroken(823, 825));
        assertTrue(ArmorProtection.isBroken(824, 825));
        assertTrue(ArmorProtection.isBroken(825, 825));
        assertEquals(0.0, ArmorProtection.pieceArmor(combat(), ArmorSlot.HELMET, DamageType.PROJECTILE, 824, 825));
        assertEquals(0, ArmorProtection.displayPoints(combat(), ArmorSlot.HELMET, 824, 825));
        assertEquals(4, ArmorProtection.displayPoints(combat(), ArmorSlot.HELMET, 0, 825));
        assertEquals(0.0, ArmorProtection.piecePenetrationResistance(steamLike(), ArmorSlot.HELMET, 824, 825));
    }

    @Test
    void wearNeverTakesTheLastPoint() {
        assertEquals(1, ArmorProtection.wear(0, 825, 1));
        assertEquals(824, ArmorProtection.wear(823, 825, 5));
        assertEquals(824, ArmorProtection.wear(824, 825, 100));
        assertEquals(10, ArmorProtection.wear(10, 825, -3));
    }

    @Test
    void maxAbsorbIsRemainingDurabilityPlusOne() {
        assertEquals(826, ArmorProtection.maxAbsorb(0, 825));
        assertEquals(2, ArmorProtection.maxAbsorb(824, 825));
    }

    private static ArmorMaterialDef steamLike() {
        return ArmorMaterialDef.uniform("t1_steam", 200, 20)
            .withPenetrationResistance(0.10);
    }

    @Test
    void penetrationResistanceOfAFullSetEqualsTheMaterialValue() {
        // doesNotReproduceARM02: Techguns subtracted each piece's full resistance from the shared source, compounding.
        // Here the four pieces carry shares that add up to the material value, summed once per hit.
        ArmorMaterialDef m = steamLike();
        double sum = 0;
        for (ArmorSlot s : ArmorSlot.values()) {
            sum += ArmorProtection.piecePenetrationResistance(m, s, 0, 2750);
        }
        assertEquals(0.10, sum, EPS);
    }

    @Test
    void doesNotReproduceARM02_penetrationResistanceDoesNotCompoundAcrossPieces() {
        ArmorMaterialDef m = steamLike();
        double sum = 0;
        for (ArmorSlot s : ArmorSlot.values()) {
            sum += ArmorProtection.piecePenetrationResistance(m, s, 0, 2750);
        }
        // 50% penetration against a set with 0.10 resistance: 0.40 once, not 0.5 - 4*0.10 = 0.10 (compounded).
        assertEquals(0.40, DamageModel.effectivePenetration(0.5, 1.0, sum), EPS);
    }

    @Test
    void t1SetsHaveNoPenetrationResistance() {
        for (SetDef set : java.util.Arrays
            .asList(ArmorCatalog.T1_SCOUT, ArmorCatalog.T1_MINER, ArmorCatalog.T1_COMBAT)) {
            assertEquals(
                0.0,
                set.material()
                    .penetrationResistance(),
                EPS,
                set.id());
        }
    }
}
