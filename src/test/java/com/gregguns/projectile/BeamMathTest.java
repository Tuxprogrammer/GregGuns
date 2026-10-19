package com.gregguns.projectile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

class BeamMathTest {

    @Test
    void alphaFadesLinearlyFromOneToZero() {
        assertEquals(1.0F, BeamMath.alpha(0, 5, 0.0F), 1e-6);
        assertEquals(0.5F, BeamMath.alpha(2, 4, 0.0F), 1e-6);
        assertEquals(0.0F, BeamMath.alpha(5, 5, 0.0F), 1e-6);
    }

    @Test
    void alphaIsClampedAndSafeForZeroLifetime() {
        assertEquals(0.0F, BeamMath.alpha(9, 5, 0.5F), 0.0F);
        assertEquals(1.0F, BeamMath.alpha(-1, 5, 0.0F), 0.0F);
        assertEquals(0.0F, BeamMath.alpha(0, 0, 0.0F), 0.0F);
    }

    @Test
    void flickerStaysInBandAndIsDeterministic() {
        for (int seed = -3; seed < 40; seed++) {
            for (int age = 0; age < 5; age++) {
                float f = BeamMath.flicker(seed, age);
                assertTrue(f >= 1.0F - BeamMath.FLICKER - 1e-6F && f <= 1.0F, "flicker " + f);
                assertEquals(f, BeamMath.flicker(seed, age), 0.0F);
            }
        }
    }

    @Test
    void flickerVariesWithSeed() {
        assertNotEquals(BeamMath.flicker(1, 0), BeamMath.flicker(2, 0));
    }

    @Test
    void spawnDataRoundTrips() {
        ByteBuf buf = Unpooled.buffer();
        new BeamSpawnData(37.5F, 5, -12345, 1).write(buf);
        BeamSpawnData read = BeamSpawnData.read(buf);
        assertEquals(37.5F, read.length(), 0.0F);
        assertEquals(5, read.lifetime());
        assertEquals(-12345, read.seed());
        assertEquals(1, read.style());
        assertEquals(0, buf.readableBytes());
    }
}
