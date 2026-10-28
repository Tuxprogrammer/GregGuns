package com.gregguns.icbm.missile;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.lang.reflect.Field;

import net.minecraft.network.PacketBuffer;

import org.junit.jupiter.api.Test;

import com.gregguns.icbm.warhead.WarheadBlast;

import io.netty.buffer.Unpooled;

/**
 * The client state packet carries the T3 casing, the warhead blast and the warm-up clock (D88). The site factors' NBT
 * is
 * in icbm.silo.tiers.
 */
class MissileSiteStateTest {

    private static void set(Object target, String field, Object value) throws ReflectiveOperationException {
        Field f = target.getClass()
            .getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }

    @Test
    void statePacketCarriesCasingThreeAndTheWarheadBlast() throws IOException, ReflectiveOperationException {
        PacketMissileState p = new PacketMissileState();
        set(p, "entityId", 77);
        set(p, "phase", 2);
        set(p, "casing", 3);
        set(p, "fuel", 8000);
        set(p, "blast", 3);
        set(p, "tick", 61);
        set(p, "lead", 40);
        set(p, "total", 240);
        PacketBuffer buf = new PacketBuffer(Unpooled.buffer());
        p.encode(buf);
        assertEquals(23, buf.readableBytes());
        PacketMissileState q = new PacketMissileState();
        q.decode(new PacketBuffer(buf.copy()));
        assertEquals(77, buf.readInt());
        assertEquals(2, buf.readByte());
        assertEquals(3, buf.readByte());
        assertEquals(8000, buf.readInt());
        assertEquals(3, buf.readByte());
        assertEquals(61, buf.readInt());
        assertEquals(40, buf.readInt());
        assertEquals(240, buf.readInt());
        for (String f : new String[] { "entityId", "phase", "casing", "fuel", "blast", "tick", "lead", "total" }) {
            Field field = PacketMissileState.class.getDeclaredField(f);
            field.setAccessible(true);
            assertEquals(field.getInt(p), field.getInt(q), f);
        }
    }

    @Test
    void everyBlastSurvivesTheRoundTripAndAppliesToTheClientMissile() throws IOException, ReflectiveOperationException {
        for (WarheadBlast b : WarheadBlast.values()) {
            PacketMissileState p = new PacketMissileState();
            set(p, "blast", b.ordinal() + 1);
            PacketBuffer buf = new PacketBuffer(Unpooled.buffer());
            p.encode(buf);
            PacketMissileState q = new PacketMissileState();
            q.decode(buf);
            Field field = PacketMissileState.class.getDeclaredField("blast");
            field.setAccessible(true);
            assertEquals(b.ordinal() + 1, field.getInt(q), b.id());
            assertEquals(b, WarheadBlast.values()[field.getInt(q) - 1], b.id());
        }
    }
}
