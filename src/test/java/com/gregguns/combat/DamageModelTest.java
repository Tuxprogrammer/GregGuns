package com.gregguns.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.DoubleUnaryOperator;

import org.junit.jupiter.api.Test;

class DamageModelTest {

    private static final double EPS = 1e-9;

    /** A mob's armour step: vanilla's formula on a total armour value. */
    private static DoubleUnaryOperator mobArmour(double points) {
        return share -> share * DamageModel.vanillaArmourFactor(points);
    }

    /** A player's armour step: four pieces whose absorb ratios sum, as Forge's ApplyArmor does (no caps hit here). */
    private static DoubleUnaryOperator playerArmour(double... piecePoints) {
        double ratio = 0;
        for (double p : piecePoints) {
            ratio += DamageModel.pieceAbsorbRatio(p);
        }
        double total = Math.min(1.0, ratio);
        return share -> share * (1.0 - total);
    }

    // --- worked examples (damage-system.md 5.7)

    @Test
    void workedExample_mobInIronWithoutPenetrationTakes3_2() {
        // M4 bullet, 8 damage, pen 0 (what Techguns used for every vanilla mob), 15 armour.
        assertEquals(3.2, DamageModel.resolve(DamageType.PROJECTILE, 8, 0.0, mobArmour(15)), EPS);
    }

    @Test
    void workedExample_npcStyleTargetWithPenetrationTakes3_92() {
        // The same bullet at 15% penetration: 8 * 0.15 + 8 * 0.85 * 10/25.
        double pen = DamageModel.effectivePenetration(0.15, 1.0, 0.0);
        assertEquals(3.92, DamageModel.resolve(DamageType.PROJECTILE, 8, pen, mobArmour(15)), EPS);
    }

    @Test
    void workedExample_playerInSetWithProjectile15Takes3_92() {
        // Total 15 split 0.25/0.30/0.25/0.20 over the pieces, ratio sum 0.6, pen 0.15 once.
        DoubleUnaryOperator set = playerArmour(3.75, 4.5, 3.75, 3.0);
        double pen = DamageModel.effectivePenetration(0.15, 1.0, 0.0);
        assertEquals(3.92, DamageModel.resolve(DamageType.PROJECTILE, 8, pen, set), EPS);
    }

    @Test
    void doesNotReproduceGUN34_penetrationAppliesToOrdinaryMobs() {
        // Techguns gave vanilla mobs pen 0 (3.2). The model applies the bullet's penetration to every target.
        double withPen = DamageModel.resolve(DamageType.PROJECTILE, 8, 0.15, mobArmour(15));
        double withoutPen = DamageModel.resolve(DamageType.PROJECTILE, 8, 0.0, mobArmour(15));
        assertTrue(withPen > withoutPen);
        assertEquals(3.92, withPen, EPS);
    }

    @Test
    void doesNotReproduceGUN39_penetrationIsNotCompoundedAcrossPieces() {
        // Techguns recomputed penetration for every worn piece and wrote it back, so a scale below 1 (the PvP
        // setting) shrank it once per piece: 0.15 * 0.5 four times is almost nothing. The model scales once per hit.
        double compounding = 0.15;
        for (int piece = 0; piece < 4; piece++) {
            compounding = DamageModel.effectivePenetration(compounding, 0.5, 0.0);
        }
        double oncePerHit = DamageModel.effectivePenetration(0.15, 0.5, 0.0);
        assertEquals(0.075, oncePerHit, EPS);
        assertNotEquals(oncePerHit, compounding, EPS);
        // Resistance is the set's total, subtracted once.
        assertEquals(0.07, DamageModel.effectivePenetration(0.15, 1.0, 4 * 0.02), EPS);
    }

    @Test
    void doesNotReproduceARM02_armourStepRunsOncePerHit() {
        AtomicInteger calls = new AtomicInteger();
        DoubleUnaryOperator counting = share -> {
            calls.incrementAndGet();
            return share;
        };
        DamageModel.resolve(DamageType.PROJECTILE, 8, 0.15, counting);
        assertEquals(1, calls.get());
    }

    @Test
    void doesNotReproduceGUN40_armourAboveTheCapDoesNotInvertDamage() {
        assertEquals(0.04, DamageModel.vanillaArmourFactor(30), EPS);
        assertEquals(0.04, DamageModel.vanillaArmourFactor(24), EPS);
        double lost = DamageModel.resolve(DamageType.PROJECTILE, 8, 0.0, mobArmour(60));
        assertEquals(0.32, lost, EPS);
        assertTrue(lost > 0);
    }

    @Test
    void doesNotReproduceGUN45_everyBlockableTypeSeesTheSameVanillaArmour() {
        double reference = DamageModel.resolve(DamageType.PHYSICAL, 8, 0.0, mobArmour(15));
        for (DamageType type : DamageType.values()) {
            if (type.bypassesArmour()) {
                continue;
            }
            assertEquals(
                reference,
                DamageModel.resolve(type, 8, 0.0, mobArmour(15)),
                EPS,
                type + " must not get a per-type vanilla armour adjustment");
        }
    }

    @Test
    void doesNotReproduceGUN35_absorptionIsNotPartOfTheModel() {
        // The model returns damage before absorption; vanilla subtracts absorption once afterwards.
        assertEquals(3.2, DamageModel.resolve(DamageType.PROJECTILE, 8, 0.0, mobArmour(15)), EPS);
    }

    // --- boundaries

    @Test
    void penetrationBoundaries() {
        assertEquals(0.0, DamageModel.effectivePenetration(0.0, 1.0, 0.0), EPS);
        assertEquals(1.0, DamageModel.effectivePenetration(1.0, 1.0, 0.0), EPS);
        assertEquals(1.0, DamageModel.effectivePenetration(2.0, 1.0, 0.0), EPS);
        assertEquals(0.0, DamageModel.effectivePenetration(0.1, 1.0, 0.5), EPS);
        assertEquals(0.0, DamageModel.effectivePenetration(-1.0, 1.0, 0.0), EPS);
        assertEquals(0.0, DamageModel.effectivePenetration(Double.NaN, 1.0, 0.0), EPS);
        assertEquals(0.3, DamageModel.effectivePenetration(0.6, 0.5, 0.0), EPS);
        assertEquals(0.1, DamageModel.effectivePenetration(0.6, 1.0, 0.5), EPS);
    }

    @Test
    void armourFactorBoundaries() {
        assertEquals(1.0, DamageModel.vanillaArmourFactor(0), EPS);
        assertEquals(1.0, DamageModel.vanillaArmourFactor(-5), EPS);
        assertEquals(0.4, DamageModel.vanillaArmourFactor(15), EPS);
        assertEquals(1.0, DamageModel.vanillaArmourFactor(Double.NaN), EPS);
    }

    @Test
    void pieceRatioBoundaries() {
        assertEquals(0.0, DamageModel.pieceAbsorbRatio(0), EPS);
        assertEquals(0.6, DamageModel.pieceAbsorbRatio(15), EPS);
        assertEquals(1.0, DamageModel.pieceAbsorbRatio(40), EPS);
        assertEquals(0.0, DamageModel.pieceAbsorbRatio(-3), EPS);
    }

    @Test
    void resolveBoundaries() {
        assertEquals(0.0, DamageModel.resolve(DamageType.PROJECTILE, 0, 0.5, mobArmour(10)), EPS);
        assertEquals(0.0, DamageModel.resolve(DamageType.PROJECTILE, -4, 0.5, mobArmour(10)), EPS);
        assertEquals(0.0, DamageModel.resolve(DamageType.PROJECTILE, Double.NaN, 0.5, mobArmour(10)), EPS);
        assertEquals(8.0, DamageModel.resolve(DamageType.PROJECTILE, 8, 1.0, mobArmour(24)), EPS);
        assertEquals(8.0, DamageModel.resolve(DamageType.PROJECTILE, 8, 0.0, mobArmour(0)), EPS);
        assertEquals(0.32, DamageModel.resolve(DamageType.PROJECTILE, 8, 0.0, mobArmour(24)), EPS);
    }

    @Test
    void fullPenetrationNeverCallsTheArmourStep() {
        AtomicInteger calls = new AtomicInteger();
        DamageModel.resolve(DamageType.PROJECTILE, 8, 1.0, share -> {
            calls.incrementAndGet();
            return 0;
        });
        assertEquals(0, calls.get());
    }

    @Test
    void unresistableSkipsArmourEntirely() {
        AtomicInteger calls = new AtomicInteger();
        double out = DamageModel.resolve(DamageType.UNRESISTABLE, 8, 0.0, share -> {
            calls.incrementAndGet();
            return 0;
        });
        assertEquals(8.0, out, EPS);
        assertEquals(0, calls.get());
    }

    @Test
    void anArmourStepCannotHealOrAmplify() {
        assertEquals(
            8.0 * 0.85 + 8 * 0.15,
            DamageModel.resolve(DamageType.PROJECTILE, 8, 0.15, share -> share * 5),
            EPS);
        assertEquals(1.2, DamageModel.resolve(DamageType.PROJECTILE, 8, 0.15, share -> -3), EPS);
    }
}
