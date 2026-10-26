package com.gregguns.icbm.warhead;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.gregguns.icbm.explosive.ExplosiveDef;

class WarheadMathTest {

    @Test
    void fullFillGivesTheFullRadiusAndDamage() {
        assertEquals(9.0, WarheadMath.radius(9.0, 5, 5), 1e-9);
        assertEquals(1200.0, WarheadMath.centerDamage(1200.0, 5, 5), 1e-9);
    }

    @Test
    void radiusAndDamageNeverDecreaseWithFill() {
        double lastR = 0;
        double lastD = 0;
        for (int fill = 1; fill <= 20; fill++) {
            double r = WarheadMath.radius(14.0, fill, 20);
            double d = WarheadMath.centerDamage(3000.0, fill, 20);
            assertTrue(r >= lastR && d >= lastD, "monotonic at fill " + fill);
            assertTrue(r <= 14.0 && d <= 3000.0, "bounded at fill " + fill);
            lastR = r;
            lastD = d;
        }
    }

    @Test
    void theRadiusIsSubLinear() {
        // Half the fill gives well over half the radius: the cube-root curve.
        double half = WarheadMath.radius(20.0, 10, 20);
        assertTrue(half > 10.0 && half < 20.0);
        assertEquals(20.0 * Math.cbrt(0.5), half, 1e-9);
    }

    @Test
    void theRadiusNeverFallsBelowTheFloor() {
        assertEquals(WarheadMath.MIN_RADIUS, WarheadMath.radius(3.5, 1, 100), 1e-9);
    }

    @Test
    void fillIsClampedBeforeUse() {
        assertEquals(WarheadMath.radius(9.0, 5, 5), WarheadMath.radius(9.0, 99, 5), 1e-9);
        assertEquals(WarheadMath.radius(9.0, 1, 5), WarheadMath.radius(9.0, -4, 5), 1e-9);
    }

    @Test
    void theEngineDefinitionMatchesTheCurve() {
        // The registry stores baseRadius * fill^exponent; it must equal the pure curve at every fill.
        for (WarheadBlast blast : WarheadBlast.values()) {
            int cap = blast.fillCap();
            double base = WarheadMath.baseRadius(blast.fullRadius(), cap);
            ExplosiveDef def = ExplosiveDef.builder(blast.id(), blast.tier())
                .baseRadius(base)
                .radiusExponent(WarheadMath.RADIUS_EXPONENT)
                .maxFill(cap)
                .build();
            for (int fill = 1; fill <= cap; fill++) {
                assertEquals(
                    WarheadMath.radius(blast.fullRadius(), fill, cap),
                    Math.max(WarheadMath.MIN_RADIUS, def.radius(fill)),
                    1e-9,
                    blast.id() + " fill " + fill);
            }
        }
    }
}
