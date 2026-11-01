package com.gregguns.compat.vending;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.UUID;

import com.gregguns.api.Tier;

/**
 * The GregGuns quest a trade waits for, by quest key, and its BetterQuesting quest ID. The quest line builder gives
 * every quest the name-based UUID (version 5, SHA-1) of its key in the line's namespace, so the ID follows from the
 * key alone. A tier's trades wait for the tier's chapter intro ({@code gg.<tier>.intro}); the tier skip triggers
 * complete the intros too, so a player who skips ahead unlocks the shop as well. Pure logic, both sides.
 */
// Spec: docs/gregguns-spec/06-items/coins-and-vending.md (Gates); tools/questbook/build_quests.py (qid: uuid5 of the
// key)
public final class QuestGate {

    /** The GregGuns quest line's UUID namespace (tools/questbook, {@code namespace}). A unit test keeps them equal. */
    public static final UUID NAMESPACE = UUID.fromString("6f1f3c52-9d0a-4b6e-8a53-2c7e0f4b91d8");

    private QuestGate() {}

    /**
     * @param tier a tier
     * @return the key of the tier's chapter intro quest, e.g. {@code gg.lv.intro}
     */
    public static String introKey(Tier tier) {
        return "gg." + tier.name()
            .toLowerCase(Locale.ROOT) + ".intro";
    }

    /**
     * @param key a quest key
     * @return the quest's BetterQuesting ID: the version 5 UUID of {@code key} in {@link #NAMESPACE}
     */
    public static UUID questId(String key) {
        return uuid5(NAMESPACE, key);
    }

    /**
     * RFC 4122 name-based UUID, version 5 (SHA-1), the same as Python's {@code uuid.uuid5}.
     *
     * @param namespace the namespace UUID
     * @param name      the name, encoded as UTF-8
     * @return the UUID
     */
    static UUID uuid5(UUID namespace, String name) {
        MessageDigest sha1;
        try {
            sha1 = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 is required by every Java runtime", e);
        }
        sha1.update(toBytes(namespace.getMostSignificantBits()));
        sha1.update(toBytes(namespace.getLeastSignificantBits()));
        sha1.update(name.getBytes(StandardCharsets.UTF_8));
        byte[] h = sha1.digest();
        h[6] = (byte) ((h[6] & 0x0F) | 0x50);
        h[8] = (byte) ((h[8] & 0x3F) | 0x80);
        return new UUID(toLong(h, 0), toLong(h, 8));
    }

    private static byte[] toBytes(long v) {
        byte[] b = new byte[8];
        for (int i = 7; i >= 0; i--) {
            b[i] = (byte) v;
            v >>>= 8;
        }
        return b;
    }

    private static long toLong(byte[] b, int off) {
        long v = 0;
        for (int i = 0; i < 8; i++) {
            v = (v << 8) | (b[off + i] & 0xFF);
        }
        return v;
    }
}
