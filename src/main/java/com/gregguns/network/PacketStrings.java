package com.gregguns.network;

import java.io.IOException;

import net.minecraft.network.PacketBuffer;

/**
 * The only way GregGuns packets read and write strings (GGS-01-12): vanilla {@link PacketBuffer} string methods, which
 * use UTF-8 and a length cap (in encoded bytes on write). This avoids Techguns' platform-charset decoding (CORE-11).
 *
 * Side: both.
 */
public final class PacketStrings {

    private PacketStrings() {}

    /**
     * Writes a string.
     *
     * @param buf   the buffer
     * @param value the text; at most 32767 bytes once encoded as UTF-8 (32767 ASCII characters, fewer for accented,
     *              CJK or emoji text). A longer text throws {@link IOException}
     */
    public static void write(PacketBuffer buf, String value) throws IOException {
        buf.writeStringToBuffer(value);
    }

    /**
     * Reads a string, rejecting anything longer than {@code maxLength} characters.
     *
     * @param buf       the buffer
     * @param maxLength the longest text this field may hold
     */
    public static String read(PacketBuffer buf, int maxLength) throws IOException {
        return buf.readStringFromBuffer(maxLength);
    }
}
