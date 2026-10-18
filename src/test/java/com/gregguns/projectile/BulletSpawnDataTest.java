package com.gregguns.projectile;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

class BulletSpawnDataTest {

    @Test
    void roundTripsEveryField() {
        ByteBuf buf = Unpooled.buffer();
        new BulletSpawnData(27, 30, 0.015F).write(buf);
        BulletSpawnData read = BulletSpawnData.read(buf);
        assertEquals(27, read.ticksToLive());
        assertEquals(30, read.lifetime());
        assertEquals(0.015F, read.gravity(), 0.0F);
        assertEquals(0, buf.readableBytes(), "the reader must consume exactly what the writer wrote");
    }

    @Test
    void doesNotReproducePFX34_gravityIsSentToTheClient() {
        // Techguns never synced gravity, so a client copy used a hard-coded default. Here it travels in the spawn data.
        ByteBuf buf = Unpooled.buffer();
        new BulletSpawnData(10, 10, 0.05F).write(buf);
        assertEquals(
            0.05F,
            BulletSpawnData.read(buf)
                .gravity(),
            0.0F);
    }
}
