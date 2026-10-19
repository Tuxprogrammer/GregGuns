package com.gregguns.fx;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import net.minecraft.network.PacketBuffer;

import org.junit.jupiter.api.Test;

import com.gregguns.network.ClientboundPacket;
import com.gregguns.network.GregGunsNetwork;
import com.gtnewhorizon.gtnhlib.network.base.IPacket;

import io.netty.buffer.Unpooled;

class FxPacketsTest {

    @Test
    void pointPacketRoundTrips() throws IOException {
        PacketSpawnParticle sent = new PacketSpawnParticle("LaserFlareEnd", 1.5, -70.25, 1.0E6, 0.5, 0, -0.5);
        PacketBuffer buf = new PacketBuffer(Unpooled.buffer());
        sent.encode(buf);
        PacketSpawnParticle got = new PacketSpawnParticle();
        got.decode(buf);
        assertEquals("LaserFlareEnd", got.name());
        assertArrayEquals(new double[] { 1.5, -70.25, 1.0E6 }, got.position(), 0.0);
        assertEquals(0, buf.readableBytes());
    }

    @Test
    void entityPacketRoundTrips() throws IOException {
        PacketSpawnParticleOnEntity sent = new PacketSpawnParticleOnEntity("BioGunTrail", 123456);
        PacketBuffer buf = new PacketBuffer(Unpooled.buffer());
        sent.encode(buf);
        PacketSpawnParticleOnEntity got = new PacketSpawnParticleOnEntity();
        got.decode(buf);
        assertEquals("BioGunTrail", got.name());
        assertEquals(123456, got.entityId());
    }

    @Test
    void namesTravelAsUtf8NotThePlatformCharset() throws IOException {
        // Techguns sends the name in the platform charset (CORE-11).
        PacketSpawnParticleOnEntity sent = new PacketSpawnParticleOnEntity("Flareé中", 1);
        PacketBuffer buf = new PacketBuffer(Unpooled.buffer());
        sent.encode(buf);
        PacketSpawnParticleOnEntity got = new PacketSpawnParticleOnEntity();
        got.decode(buf);
        assertEquals("Flareé中", got.name());
        PacketBuffer buf2 = new PacketBuffer(Unpooled.buffer());
        new PacketSpawnParticle("Flareu00e9u4e2d", 0, 0, 0, 0, 0, 0).encode(buf2);
        PacketSpawnParticle got2 = new PacketSpawnParticle();
        got2.decode(buf2);
        assertEquals("Flareu00e9u4e2d", got2.name());
    }

    @Test
    void aNameLongerThanTheLimitFailsToDecodeInsteadOfAllocating() throws IOException {
        PacketBuffer buf = new PacketBuffer(Unpooled.buffer());
        buf.writeStringToBuffer(repeat('a', PacketSpawnParticle.MAX_NAME + 1));
        buf.writeInt(1);
        assertThrows(Exception.class, () -> new PacketSpawnParticleOnEntity().decode(buf));
    }

    @Test
    void bothPacketsAreRegisteredClientBoundAndAppendedAfterTheEarlierOnes() {
        int point = -1;
        int entity = -1;
        int i = 0;
        for (IPacket p : GregGunsNetwork.clientboundPackets()) {
            assertTrue(p instanceof ClientboundPacket);
            if (p instanceof PacketSpawnParticle) {
                point = i;
            }
            if (p instanceof PacketSpawnParticleOnEntity) {
                entity = i;
            }
            i++;
        }
        assertTrue(point >= 3 && entity == point + 1, "point " + point + ", entity " + entity);
    }

    private static String repeat(char c, int n) {
        char[] cs = new char[n];
        java.util.Arrays.fill(cs, c);
        return new String(cs);
    }
}
