package com.gregguns.armor.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.gregguns.api.Tier;

/** Bonus summing and full-set detection. */
class SetEvaluatorTest {

    private static final double EPS = 1e-9;

    private static List<SetEvaluator.Worn> worn(SetDef set, boolean... broken) {
        List<SetEvaluator.Worn> out = new ArrayList<>();
        int i = 0;
        for (PieceDef p : set.pieces()
            .values()) {
            out.add(new SetEvaluator.Worn(set, p, broken.length > i && broken[i]));
            i++;
        }
        return out;
    }

    @Test
    void fullScoutSumsTheSheetValues() {
        // armor-catalog.md 5.2: speed 0.125 x4, jump 0.02 x3 + 0.10, fall 0.2, free height 1.0.
        SetEvaluator.Result r = SetEvaluator.evaluate(worn(ArmorCatalog.T1_SCOUT));
        assertEquals(
            0.5,
            r.bonuses()
                .get(ArmorBonus.SPEED),
            EPS);
        assertEquals(
            0.16,
            r.bonuses()
                .get(ArmorBonus.JUMP),
            EPS);
        assertEquals(
            0.2,
            r.bonuses()
                .get(ArmorBonus.FALL_REDUCTION),
            EPS);
        assertEquals(
            1.0,
            r.bonuses()
                .get(ArmorBonus.FREE_FALL_HEIGHT),
            EPS);
        assertTrue(
            r.completeSets()
                .contains("t1_scout"));
    }

    @Test
    void fullMinerSumsTheSheetValues() {
        SetEvaluator.Result r = SetEvaluator.evaluate(worn(ArmorCatalog.T1_MINER));
        assertEquals(
            0.32,
            r.bonuses()
                .get(ArmorBonus.SPEED),
            EPS);
        assertEquals(
            0.20,
            r.bonuses()
                .get(ArmorBonus.BREAK_SPEED),
            EPS);
        assertEquals(
            0.10,
            r.bonuses()
                .get(ArmorBonus.JUMP),
            EPS);
    }

    @Test
    void fullCombatGivesKnockbackResistanceOnly() {
        SetEvaluator.Result r = SetEvaluator.evaluate(worn(ArmorCatalog.T1_COMBAT));
        assertEquals(
            0.40,
            r.bonuses()
                .get(ArmorBonus.KNOCKBACK_RESISTANCE),
            EPS);
        assertEquals(
            1,
            r.bonuses()
                .asMap()
                .size());
    }

    @Test
    void nothingWornMeansNoBonusesAndNoSets() {
        SetEvaluator.Result r = SetEvaluator.evaluate(Collections.<SetEvaluator.Worn>emptyList());
        assertTrue(
            r.bonuses()
                .isEmpty());
        assertTrue(
            r.completeSets()
                .isEmpty());
    }

    @Test
    void threePiecesAreNotACompleteSet() {
        List<SetEvaluator.Worn> three = worn(ArmorCatalog.T1_SCOUT).subList(0, 3);
        SetEvaluator.Result r = SetEvaluator.evaluate(three);
        assertFalse(
            r.completeSets()
                .contains("t1_scout"));
        assertEquals(3, r.intactPieces("t1_scout"));
        assertEquals(
            0.375,
            r.bonuses()
                .get(ArmorBonus.SPEED),
            EPS);
    }

    @Test
    void aBrokenPieceContributesNothingAndBreaksTheSet() {
        SetEvaluator.Result r = SetEvaluator.evaluate(worn(ArmorCatalog.T1_SCOUT, false, false, false, true));
        assertFalse(
            r.completeSets()
                .contains("t1_scout"));
        assertEquals(3, r.intactPieces("t1_scout"));
        assertEquals(
            0.0,
            r.bonuses()
                .get(ArmorBonus.FALL_REDUCTION),
            EPS);
    }

    @Test
    void mixedSetsAddTheirPieceBonusesButNeitherIsComplete() {
        List<SetEvaluator.Worn> mixed = new ArrayList<>();
        mixed.add(worn(ArmorCatalog.T1_SCOUT).get(0));
        mixed.add(worn(ArmorCatalog.T1_MINER).get(1));
        SetEvaluator.Result r = SetEvaluator.evaluate(mixed);
        assertEquals(
            0.125 + 0.08,
            r.bonuses()
                .get(ArmorBonus.SPEED),
            EPS);
        assertTrue(
            r.completeSets()
                .isEmpty());
    }

    @Test
    void aFullSetBonusIsAddedOnceWhenTheSetIsCompleteAndRemovedWhenNot() {
        SetDef set = new SetDef(
            "test_set",
            Tier.EV,
            ArmorMaterialDef.uniform("test_set", 60, 10),
            Collections.singletonList("t1_combat"),
            BonusMap.of(ArmorBonus.STEP_ASSIST, 1.0),
            PieceDef.builder(ArmorSlot.HELMET, "H")
                .bonuses(BonusMap.of(ArmorBonus.SPEED, 0.1))
                .build(),
            PieceDef.builder(ArmorSlot.BOOTS, "B")
                .bonuses(BonusMap.of(ArmorBonus.SPEED, 0.1))
                .build());
        SetEvaluator.Result full = SetEvaluator.evaluate(worn(set));
        assertEquals(
            1.0,
            full.bonuses()
                .get(ArmorBonus.STEP_ASSIST),
            EPS);
        assertEquals(
            0.2,
            full.bonuses()
                .get(ArmorBonus.SPEED),
            EPS);
        SetEvaluator.Result half = SetEvaluator.evaluate(worn(set).subList(0, 1));
        assertEquals(
            0.0,
            half.bonuses()
                .get(ArmorBonus.STEP_ASSIST),
            EPS);
        assertEquals(
            0.1,
            half.bonuses()
                .get(ArmorBonus.SPEED),
            EPS);
        SetEvaluator.Result brokenBoots = SetEvaluator.evaluate(worn(set, false, true));
        assertEquals(
            0.0,
            brokenBoots.bonuses()
                .get(ArmorBonus.STEP_ASSIST),
            EPS);
    }

    @Test
    void bonusMapBasics() {
        BonusMap a = BonusMap.of(ArmorBonus.SPEED, 0.5, ArmorBonus.JUMP, 0.0);
        assertEquals(
            1,
            a.asMap()
                .size());
        assertEquals(a, BonusMap.of(ArmorBonus.SPEED, 0.5));
        assertEquals(
            0.0,
            a.with(ArmorBonus.SPEED, 0.0)
                .get(ArmorBonus.SPEED));
        assertTrue(
            a.with(ArmorBonus.SPEED, 0.0)
                .isEmpty());
        assertEquals(
            1.0,
            a.plus(a)
                .get(ArmorBonus.SPEED),
            EPS);
        assertEquals(0.5, a.get(ArmorBonus.SPEED), EPS);
    }

    @Test
    void fallMath() {
        // Scout boots: 0.2 reduction, 1 block free. 5 blocks -> (5-1)*0.8.
        assertEquals(3.2, FallMath.adjust(5.0, 0.2, 1.0), EPS);
        assertEquals(0.0, FallMath.adjust(0.9, 0.2, 1.0), EPS);
        assertEquals(5.0, FallMath.adjust(5.0, 0.0, 0.0), EPS);
        assertEquals(0.0, FallMath.adjust(5.0, 3.0, 0.0), EPS);
    }
}
