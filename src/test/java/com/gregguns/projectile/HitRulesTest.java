package com.gregguns.projectile;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HitRulesTest {

    @Test
    void shooterIsSafeInTheFirstTicksOnly() {
        for (int tick = 0; tick <= HitRules.SHOOTER_GRACE_TICKS; tick++) {
            assertFalse(HitRules.mayHitShooter(tick), "tick " + tick);
        }
        assertTrue(HitRules.mayHitShooter(HitRules.SHOOTER_GRACE_TICKS + 1));
    }

    @Test
    void doesNotReproducePFX23_aNearerBlockHidesTheEntityBehindIt() {
        assertFalse(HitRules.entityBeforeBlock(0.8, 0.3));
        assertTrue(HitRules.entityBeforeBlock(0.3, 0.8));
        assertTrue(HitRules.entityBeforeBlock(0.99, HitRules.NO_BLOCK));
        assertFalse(HitRules.entityBeforeBlock(0.5, 0.5));
    }

    @Test
    void doesNotReproducePFX27_passableBlocksDoNotStopABullet() {
        assertFalse(HitRules.blockStopsBullet(true));
        assertTrue(HitRules.blockStopsBullet(false));
    }
}
