package com.gregguns.armor.flight;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.io.IOException;

import net.minecraft.network.PacketBuffer;

import org.junit.jupiter.api.Test;

import com.gregguns.armor.flight.logic.BackKind;
import com.gregguns.armor.flight.logic.FlightStatus;

import io.netty.buffer.Unpooled;

/** The wire form of the flight status: two bytes, kind then flags. */
class PacketFlightStatusTest {

    @Test
    void encodesKindThenFlags() throws IOException {
        PacketBuffer buf = new PacketBuffer(Unpooled.buffer());
        new PacketFlightStatus(new FlightStatus(BackKind.ANTI_GRAVITY, true, true)).encode(buf);
        assertEquals(2, buf.readableBytes());
        assertEquals(BackKind.ANTI_GRAVITY.ordinal(), buf.readByte());
        assertEquals(3, buf.readByte());
    }

    @Test
    void flagsAreIndependentBits() throws IOException {
        PacketBuffer buf = new PacketBuffer(Unpooled.buffer());
        new PacketFlightStatus(new FlightStatus(BackKind.GLIDER, true, false)).encode(buf);
        buf.readByte();
        assertEquals(1, buf.readByte());
        buf = new PacketBuffer(Unpooled.buffer());
        new PacketFlightStatus(new FlightStatus(BackKind.JUMP_PACK, false, true)).encode(buf);
        buf.readByte();
        assertEquals(2, buf.readByte());
    }

    @Test
    void statusEqualityCoversEveryField() {
        FlightStatus a = new FlightStatus(BackKind.GLIDER, true, true);
        assertEquals(a, new FlightStatus(BackKind.GLIDER, true, true));
        assertNotEquals(a, new FlightStatus(BackKind.GLIDER, false, true));
        assertNotEquals(a, new FlightStatus(BackKind.GLIDER, true, false));
        assertNotEquals(a, new FlightStatus(BackKind.JUMP_PACK, true, true));
    }
}
