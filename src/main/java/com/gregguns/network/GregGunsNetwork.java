package com.gregguns.network;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.gregguns.GregGuns;
import com.gregguns.gun.net.PacketGunEvent;
import com.gregguns.gun.net.PacketGunTrigger;
import com.gregguns.gun.net.PacketGunZoom;
import com.gregguns.network.validation.LimitedRejectionSink;
import com.gregguns.network.validation.RateLimiter;
import com.gregguns.network.validation.RejectionSink;
import com.gtnewhorizon.gtnhlib.network.base.IPacket;
import com.gtnewhorizon.gtnhlib.network.base.NetworkChannel;

/**
 * Holder for GregGuns' single network channel (GGS-01-07).
 *
 * How to write a packet (M1 and later):
 * <ol>
 * <li>Extend {@link ServerboundPacket} (client to server) or {@link ClientboundPacket} (server to client), with a
 * public
 * no-argument constructor. Implement {@code encode} and {@code decode} with {@code PacketBuffer}; use
 * {@link PacketStrings} for text.</li>
 * <li>Declare validation in {@code validate(...)}: ranges, dimension, reach, target type, permission. Only the rules
 * you declare run, plus the always-on sender checks for server-bound packets.</li>
 * <li>Put the work in {@code handle(...)}. It runs on the main thread after every rule passed, so it may touch the
 * world.</li>
 * <li>Add one instance to {@link #serverboundPackets()} or {@link #clientboundPackets()}. <b>Append only</b>: the
 * registration order is the wire id, and both sides must register in the same order.</li>
 * <li>Add a unit test for the rules, like the ones under {@code src/test/java/com/gregguns/network}.</li>
 * </ol>
 *
 * Spec: GGS-01-07, GGS-01-08 (docs/gregguns-spec/01-core/networking-gtnhlib.md).
 * Source: GTNHLib 0.11.52,
 * https://github.com/GTNewHorizons/GTNHLib/blob/0.11.52/src/main/java/com/gtnewhorizon/gtnhlib/network/base/NetworkChannel.java#L26-L31
 * (toClient/toServer assign ids in call order) and
 * https://github.com/GTNewHorizons/GTNHLib/blob/0.11.52/src/main/java/com/gtnewhorizon/gtnhlib/network/base/IPacket.java#L26-L48
 * (onMessage reschedules onto the main thread before executeClient/executeServer).
 *
 * Side: both. Registered in {@code CommonProxy.preInit}.
 */
public final class GregGunsNetwork {

    /** Channel name; at most 20 characters (FML limit). */
    public static final String CHANNEL_NAME = GregGuns.MODID;

    /** Longest permitted burst of rejection reports per window. */
    private static final int REPORTS_PER_WINDOW = 20;
    private static final long WINDOW_NANOS = TimeUnit.SECONDS.toNanos(10);

    // Shared rate-limited log sink: a registry-like singleton, not gameplay state.
    private static final RejectionSink REJECTIONS = new LimitedRejectionSink(
        new LogRejectionSink(GregGuns.LOG),
        new RateLimiter(REPORTS_PER_WINDOW, WINDOW_NANOS, System::nanoTime));

    // Set once in preInit; the channel is a registry.
    private static NetworkChannel channel;

    private GregGunsNetwork() {}

    /**
     * Creates the channel and registers every packet. Call once from preInit, on both sides.
     *
     * Registration order is the wire id: all {@link #clientboundPackets()} first, then all
     * {@link #serverboundPackets()}, each in list order.
     */
    public static synchronized void init() {
        if (channel != null) {
            throw new IllegalStateException("GregGuns network channel already initialized");
        }
        NetworkChannel created = new NetworkChannel(CHANNEL_NAME);
        for (IPacket packet : clientboundPackets()) {
            created.toClient(packet);
        }
        for (IPacket packet : serverboundPackets()) {
            created.toServer(packet);
        }
        channel = created;
        GregGuns.LOG.info(
            "Network channel '{}' registered: {} client-bound, {} server-bound packets",
            CHANNEL_NAME,
            clientboundPackets().size(),
            serverboundPackets().size());
    }

    /**
     * Returns the channel for sending.
     *
     * @throws IllegalStateException if called before {@link #init()}
     */
    public static NetworkChannel channel() {
        if (channel == null) {
            throw new IllegalStateException("GregGuns network channel used before preInit");
        }
        return channel;
    }

    /** Returns the shared, rate-limited sink that packet base classes report drops to. */
    static RejectionSink rejections() {
        return REJECTIONS;
    }

    /** Returns one instance of each server-to-client packet, in wire order. Append only. */
    public static List<IPacket> clientboundPackets() {
        return Collections.<IPacket>unmodifiableList(Arrays.<IPacket>asList(new PacketGunEvent()));
    }

    /** Returns one instance of each client-to-server packet, in wire order. Append only. */
    public static List<IPacket> serverboundPackets() {
        return Collections
            .<IPacket>unmodifiableList(Arrays.<IPacket>asList(new PacketGunTrigger(), new PacketGunZoom()));
    }
}
