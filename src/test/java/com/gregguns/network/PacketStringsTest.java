package com.gregguns.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.network.PacketBuffer;

import org.junit.jupiter.api.Test;

import com.gtnewhorizon.gtnhlib.network.base.IPacket;

import io.netty.buffer.Unpooled;

/** GGS-01-12: strings cross the wire as UTF-8 through PacketBuffer, never the platform charset (fixes CORE-11). */
class PacketStringsTest {

    /** The wire limit: PacketBuffer.writeStringToBuffer rejects more than this many UTF-8 bytes. */
    private static final int MAX = 32767;

    private static String roundTrip(String text, int maxLength) throws IOException {
        PacketBuffer buf = new PacketBuffer(Unpooled.buffer());
        PacketStrings.write(buf, text);
        String read = PacketStrings.read(buf, maxLength);
        assertEquals(0, buf.readableBytes(), "the read must consume exactly what the write produced");
        return read;
    }

    @Test
    void emptyStringRoundTrips() throws IOException {
        assertEquals("", roundTrip("", 16));
    }

    @Test
    void asciiRoundTrips() throws IOException {
        assertEquals("pistol_9mm", roundTrip("pistol_9mm", 64));
    }

    @Test
    void nonAsciiRoundTripsWithoutPlatformCharsetLoss() throws IOException {
        // Two-byte (e-acute, Cyrillic), three-byte (CJK, snowman) and four-byte (a surrogate pair) UTF-8 sequences.
        for (String text : new String[] { "café", "Пистолет", "拘銃", "gun 🔫!", "ßüñ ☃" }) {
            assertEquals(text, roundTrip(text, 64), text);
        }
    }

    @Test
    void maximumLengthStringRoundTrips() throws IOException {
        // The wire limit is 32767 UTF-8 bytes, not characters.
        char[] ascii = new char[MAX];
        Arrays.fill(ascii, 'a');
        assertEquals(new String(ascii), roundTrip(new String(ascii), MAX));

        char[] twoByte = new char[MAX / 2];
        Arrays.fill(twoByte, 'é');
        assertEquals(new String(twoByte), roundTrip(new String(twoByte), MAX));

        StringBuilder pairs = new StringBuilder();
        for (int i = 0; i < MAX / 4; i++) {
            pairs.append("🔫");
        }
        assertEquals(pairs.toString(), roundTrip(pairs.toString(), MAX));
    }

    @Test
    void overlongWriteIsRejectedByBytesNotCharacters() {
        char[] ascii = new char[MAX + 1];
        Arrays.fill(ascii, 'a');
        PacketBuffer buf = new PacketBuffer(Unpooled.buffer());
        assertThrows(IOException.class, () -> PacketStrings.write(buf, new String(ascii)));
        // 16384 two-byte characters are only 16384 chars but 32768 bytes.
        char[] twoByte = new char[MAX / 2 + 1];
        Arrays.fill(twoByte, 'é');
        assertThrows(IOException.class, () -> PacketStrings.write(buf, new String(twoByte)));
    }

    @Test
    void readRejectsTextLongerThanTheFieldAllows() throws IOException {
        PacketBuffer buf = new PacketBuffer(Unpooled.buffer());
        PacketStrings.write(buf, "this is longer than ten characters");
        assertThrows(IOException.class, () -> PacketStrings.read(buf, 10));
    }

    @Test
    void textAtExactlyTheFieldLimitIsAccepted() throws IOException {
        assertEquals("0123456789", roundTrip("0123456789", 10));
    }

    /** Packets that carry text. A packet with a String field must be listed here and round-tripped above. */
    private static final Set<Class<?>> STRING_PACKETS = new HashSet<>();

    @Test
    void everyPacketWithAStringFieldIsCoveredByARoundTrip() {
        // Today none of the registered packets (PacketGunTrigger, PacketGunZoom, PacketGunCharge, PacketGunEvent)
        // carries text. This
        // fails the day one does, so its non-ASCII round trip gets written with it (GGS-01-12).
        List<IPacket> all = new ArrayList<>(GregGunsNetwork.serverboundPackets());
        all.addAll(GregGunsNetwork.clientboundPackets());
        for (IPacket packet : all) {
            // The two base classes keep a decode-error string that is never on the wire; skip their fields.
            for (Class<?> c = packet.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
                if (c == ServerboundPacket.class || c == ClientboundPacket.class) {
                    continue;
                }
                for (Field f : c.getDeclaredFields()) {
                    if (f.getType() == String.class && !Modifier.isStatic(f.getModifiers())) {
                        assertTrue(
                            STRING_PACKETS.contains(packet.getClass()),
                            packet.getClass()
                                .getSimpleName() + "."
                                + f.getName()
                                + " is a String: add a non-ASCII round-trip test");
                    }
                }
            }
        }
    }
}
