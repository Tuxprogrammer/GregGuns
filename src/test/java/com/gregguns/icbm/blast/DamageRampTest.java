package com.gregguns.icbm.blast;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.gregguns.icbm.explosive.ExplosiveDef;
import com.gregguns.icbm.explosive.ExplosiveTier;

/**
 * The conventional damage targets (D34) against Infernal-scale health pools: max health is base x modifiers x 1.8, so
 * an Infernal blaze or enderman (base 40, 15 modifiers) has 1,080 and a modded mini-boss (base 100) 2,700.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/blast-engine.md (damage); docs/design/0018-icbm-program.md section 5
class DamageRampTest {

    private static double pool(double base, int modifiers) {
        return base * modifiers * 1.8;
    }

    @Test
    void t1KillsAnInfernalBlazeAtGroundZero() {
        ExplosiveDef t1 = ExplosiveDef.builder("t1", ExplosiveTier.T1)
            .maxFill(5)
            .build();
        double hit = DamageRamp.at(t1.damage(5), t1.radius(5), 0.0);
        assertEquals(1200.0, hit, 1e-9);
        assertTrue(hit >= pool(40, 15), "1,200 beats a 1,080 pool");
    }

    @Test
    void t2KillsAnInfernalModdedMiniBossAtGroundZero() {
        ExplosiveDef t2 = ExplosiveDef.builder("t2", ExplosiveTier.T2)
            .maxFill(20)
            .build();
        double hit = DamageRamp.at(t2.damage(20), t2.radius(20), 0.0);
        assertEquals(3000.0, hit, 1e-9);
        assertTrue(hit >= pool(100, 15), "3,000 beats a 2,700 pool");
    }

    @Test
    void t3ClearsEveryInfernalPoolWithMargin() {
        assertTrue(ExplosiveTier.T3.minCenterDamage() > 2.5 * pool(100, 15) / 2.0);
        assertTrue(ExplosiveTier.T3.minCenterDamage() >= 8000.0);
    }

    @Test
    void damageIsFlatInTheCoreFallsLinearlyAndIsZeroAtTheEdge() {
        double r = 20.0;
        assertEquals(1200.0, DamageRamp.at(1200.0, r, 0.0), 1e-9);
        assertEquals(1200.0, DamageRamp.at(1200.0, r, r * DamageRamp.CORE_SHARE), 1e-9);
        double mid = DamageRamp.at(1200.0, r, r * 0.55);
        assertEquals(600.0, mid, 1e-6, "halfway between core edge and radius");
        assertEquals(0.0, DamageRamp.at(1200.0, r, r), 0.0);
        assertEquals(0.0, DamageRamp.at(1200.0, r, r + 5.0), 0.0);
        assertEquals(0.0, DamageRamp.at(1200.0, r, Double.NaN), 0.0);
        double prev = Double.MAX_VALUE;
        for (double d = 0.0; d <= r; d += 0.5) {
            double v = DamageRamp.at(1200.0, r, d);
            assertTrue(v <= prev + 1e-9, "never rises with distance");
            prev = v;
        }
    }

    @Test
    void aPartialFillFallsShortOfTheTargetBySqrt() {
        ExplosiveDef t1 = ExplosiveDef.builder("t1", ExplosiveTier.T1)
            .maxFill(5)
            .build();
        assertTrue(t1.damage(1) < 1200.0);
        assertEquals(1200.0 * Math.sqrt(0.2), t1.damage(1), 1e-9);
    }
}
