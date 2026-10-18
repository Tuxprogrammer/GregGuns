package com.gregguns.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class KnockbackTest {

    // A vanilla hit on a standing target: horizontal motion 0 -> 0.4 away from the shooter.
    private static final double BEFORE = 0.0;
    private static final double AFTER = 0.4;

    @Test
    void doesNotReproduceGUN38_theMultiplierChangesTheStrength() {
        // Techguns passed damage x multiplier to a vanilla knockBack that ignores its strength, so x1.5, x2 and x3
        // all pushed the same. GregGuns scales the measured velocity change instead.
        double one = Knockback.scale(BEFORE, AFTER, 1.0);
        double two = Knockback.scale(BEFORE, AFTER, 2.0);
        double three = Knockback.scale(BEFORE, AFTER, 3.0);
        assertEquals(0.4, one, 1e-12);
        assertEquals(0.8, two, 1e-12);
        assertEquals(1.2, three, 1e-12);
        assertTrue(two > one && three > two);
    }

    @Test
    void zeroCancelsThePushAndKeepsTheMotionTheTargetHad() {
        assertEquals(0.1, Knockback.scale(0.1, 0.5, 0.0), 1e-12);
    }

    @Test
    void theChangeIsScaledNotTheWholeVelocity() {
        // A target already moving at 0.3 and pushed to 0.7 gains 0.4; doubled it gains 0.8 on top of its own 0.3.
        assertEquals(1.1, Knockback.scale(0.3, 0.7, 2.0), 1e-12);
    }

    @Test
    void negativeMultipliersAreTreatedAsZero() {
        assertEquals(BEFORE, Knockback.scale(BEFORE, AFTER, -2.0), 1e-12);
    }

    @Test
    void onlyANonVanillaMultiplierNeedsAdjusting() {
        assertFalse(Knockback.needsAdjustment(1.0));
        assertTrue(Knockback.needsAdjustment(0.0));
        assertTrue(Knockback.needsAdjustment(1.5));
    }
}
