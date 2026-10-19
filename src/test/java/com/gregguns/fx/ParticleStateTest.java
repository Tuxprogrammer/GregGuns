package com.gregguns.fx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ParticleStateTest {

    private static ParticleSpec spec(int lifetime) {
        ParticleSpec s = new ParticleSpec();
        s.lifetime = lifetime;
        s.size = 1F;
        s.sizeRate = 0F;
        s.sizeRateDamping = 1F;
        s.animationSpeed = 1F;
        s.velocityDamping = 1F;
        return s;
    }

    @Test
    void livesExactlyItsLifetimeThenDies() {
        ParticleState p = new ParticleState(
            ParticleSystemDef.builder("t")
                .build(),
            spec(3));
        assertTrue(p.tick());
        assertTrue(p.tick());
        assertTrue(p.tick());
        assertFalse(p.tick());
        assertTrue(p.isDead());
    }

    @Test
    void gravityIsAConstantDriftNotAnAcceleration() {
        // Techguns subtracts Gravity from a velocity it re-reads each tick, so the fall speed does not build up.
        ParticleSystemDef d = ParticleSystemDef.builder("t")
            .gravity(0.1F)
            .build();
        ParticleState p = new ParticleState(d, spec(100));
        for (int i = 0; i < 10; i++) {
            p.tick();
        }
        assertEquals(-1.0, p.y(), 1.0E-5);
    }

    @Test
    void velocityIsDampedEachTickAfterTheMove() {
        ParticleSystemDef d = ParticleSystemDef.builder("t")
            .gravity(0F)
            .build();
        ParticleSpec s = spec(100);
        s.vx = 1.0;
        s.velocityDamping = 0.5F;
        ParticleState p = new ParticleState(d, s);
        p.tick();
        p.tick();
        p.tick();
        assertEquals(1.0 + 0.5 + 0.25, p.x(), 1.0E-9);
    }

    @Test
    void sizeGrowsByTheRateAndTheRateIsDamped() {
        ParticleSystemDef d = ParticleSystemDef.builder("t")
            .build();
        ParticleSpec s = spec(100);
        s.size = 2F;
        s.sizeRate = 1F;
        s.sizeRateDamping = 0.5F;
        ParticleState p = new ParticleState(d, s);
        p.tick();
        p.tick();
        assertEquals(3.5F, p.size(1F), 1.0E-6);
    }

    @Test
    void sizeNeverGoesNegative() {
        ParticleSystemDef d = ParticleSystemDef.builder("t")
            .build();
        ParticleSpec s = spec(100);
        s.sizeRate = -5F;
        ParticleState p = new ParticleState(d, s);
        p.tick();
        assertEquals(0F, p.size(1F));
    }

    @Test
    void framesAnimateOverTheLifetimeAndLoopWithAnimationSpeed() {
        ParticleSystemDef d = ParticleSystemDef.builder("t")
            .columns(2)
            .rows(2)
            .frames(4)
            .build();
        ParticleSpec s = spec(8);
        ParticleState p = new ParticleState(d, s);
        assertEquals(0, p.frame(0F));
        for (int i = 0; i < 4; i++) {
            p.tick();
        }
        assertEquals(2, p.frame(0F)); // halfway: floor(4 * 0.5)
        s.animationSpeed = 2F;
        assertEquals(0, p.frame(0F)); // floor(4 * 0.5 * 2) = 4, mod 4
    }

    @Test
    void aVariationParticleKeepsOneFrameForLife() {
        ParticleSystemDef d = ParticleSystemDef.builder("t")
            .columns(3)
            .rows(3)
            .frames(6)
            .hasVariations(true)
            .build();
        ParticleSpec s = spec(10);
        s.variationFrame = 4;
        ParticleState p = new ParticleState(d, s);
        assertEquals(4, p.frame(0F));
        for (int i = 0; i < 9; i++) {
            p.tick();
        }
        assertEquals(4, p.frame(0.5F));
    }

    @Test
    void progressStaysInZeroToOneEvenPastTheLifetime() {
        ParticleState p = new ParticleState(
            ParticleSystemDef.builder("t")
                .build(),
            spec(2));
        p.tick();
        p.tick();
        assertEquals(1F, p.progress(1F));
        assertEquals(1F, p.progress(5F));
    }

    @Test
    void positionInterpolatesBetweenTicks() {
        ParticleSystemDef d = ParticleSystemDef.builder("t")
            .gravity(0F)
            .build();
        ParticleSpec s = spec(10);
        s.vx = 2.0;
        ParticleState p = new ParticleState(d, s);
        p.tick();
        assertEquals(1.0, p.x(0.5F), 1.0E-9);
    }

    @Test
    void alphaAndColorFollowTheKeys() {
        ParticleSystemDef d = ParticleSystemDef.builder("t")
            .alphaKey(0F, 0F)
            .alphaKey(1F, 0.5F)
            .alphaKey(0F, 1F)
            .colorKey(255, 0, 0, 0F)
            .colorKey(0, 0, 255, 1F)
            .build();
        ParticleState p = new ParticleState(d, spec(4));
        assertEquals(0F, p.alpha(0F), 1.0E-6);
        assertEquals(0xFF0000, p.rgb(0F));
        p.tick();
        p.tick();
        assertEquals(1F, p.alpha(0F), 1.0E-6);
    }
}
