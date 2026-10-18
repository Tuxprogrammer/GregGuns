package com.gregguns.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.gtnewhorizon.gtnhlib.network.base.IPacket;

/** GGS-01-07: the registered packet lists. */
class GregGunsNetworkTest {

    @Test
    void everyRegisteredPacketExtendsTheRightValidatedBase() {
        for (IPacket packet : GregGunsNetwork.clientboundPackets()) {
            assertTrue(packet instanceof ClientboundPacket, packet.getClass() + " must extend ClientboundPacket");
        }
        for (IPacket packet : GregGunsNetwork.serverboundPackets()) {
            assertTrue(packet instanceof ServerboundPacket, packet.getClass() + " must extend ServerboundPacket");
        }
    }

    @Test
    void noPacketClassIsRegisteredTwice() {
        Set<Class<?>> seen = new HashSet<>();
        List<IPacket> all = new ArrayList<>(GregGunsNetwork.clientboundPackets());
        all.addAll(GregGunsNetwork.serverboundPackets());
        for (IPacket packet : all) {
            assertTrue(seen.add(packet.getClass()), packet.getClass() + " registered twice");
        }
        assertEquals(all.size(), seen.size());
    }

    @Test
    void channelNameFitsFmlLimit() {
        assertTrue(GregGunsNetwork.CHANNEL_NAME.length() <= 20);
    }

    @Test
    void channelIsUnavailableBeforePreInit() {
        assertThrows(IllegalStateException.class, GregGunsNetwork::channel);
    }
}
