package com.gregguns.icbm.missile;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.lang.reflect.Field;

import net.minecraft.network.PacketBuffer;

import org.junit.jupiter.api.Test;

import io.netty.buffer.Unpooled;

/** The client state packet carries the T3 casing and the nuclear flag. The site factors' NBT is in icbm.silo.tiers. */
class MissileSiteStateTest {

    private static void set(Object target, String field, Object value) throws ReflectiveOperationException {
        Field f = target.getClass()
            .getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }

    @Test
    void statePacketCarriesCasingThreeAndTheNuclearFlag() throws IOException, ReflectiveOperationException {
        PacketMissileState p = new PacketMissileState();
        set(p, "entityId", 77);
        set(p, "phase", 2);
        set(p, "casing", 3);
        set(p, "fuel", 8000);
        set(p, "nuclear", 1);
        PacketBuffer buf = new PacketBuffer(Unpooled.buffer());
        p.encode(buf);
        assertEquals(11, buf.readableBytes());
        PacketMissileState q = new PacketMissileState();
        q.decode(new PacketBuffer(buf.copy()));
        assertEquals(77, buf.readInt());
        assertEquals(2, buf.readByte());
        assertEquals(3, buf.readByte());
        assertEquals(8000, buf.readInt());
        assertEquals(1, buf.readByte());
        for (String f : new String[] { "entityId", "phase", "casing", "fuel", "nuclear" }) {
            Field field = PacketMissileState.class.getDeclaredField(f);
            field.setAccessible(true);
            assertEquals(field.getInt(p), field.getInt(q), f);
        }
    }
}
