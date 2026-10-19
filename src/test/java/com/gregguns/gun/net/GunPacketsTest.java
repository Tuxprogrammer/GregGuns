package com.gregguns.gun.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;

import org.junit.jupiter.api.Test;

import com.gregguns.network.GregGunsNetwork;
import com.gregguns.network.validation.ClientRules;
import com.gregguns.network.validation.FakeSender;
import com.gregguns.network.validation.PacketOutcome;
import com.gregguns.network.validation.PacketPipeline;
import com.gregguns.network.validation.RecordingSink;
import com.gregguns.network.validation.Rejection;
import com.gregguns.network.validation.Sender;
import com.gregguns.network.validation.SenderRules;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

class GunPacketsTest {

    /** A gun intent whose held item is faked, since a real player needs a running game. */
    static final class TestIntent extends GunIntentPacket {

        int heldSlot = 3;
        boolean holdsGun = true;
        int handled;

        TestIntent(int dimension, int slot) {
            super(dimension, slot);
        }

        @Override
        protected GunPacketChecks.Held held(Sender sender) {
            return new GunPacketChecks.Held(heldSlot, holdsGun);
        }

        @Override
        protected void handle(EntityPlayerMP player) {}
    }

    private final FakeSender sender = new FakeSender();
    private final RecordingSink sink = new RecordingSink();

    /** The same pipeline the server base class runs: sender checks first, then the packet's own rules. */
    private PacketOutcome run(TestIntent packet) {
        return PacketPipeline.run("TestIntent", sender.name(), new SenderRules(sender), rules -> {
            rules.senderMayAct();
            packet.validate(rules);
        }, () -> packet.handled++, sink);
    }

    @Test
    void acceptsAnIntentForTheGunInHand() {
        TestIntent packet = new TestIntent(0, 3);
        assertTrue(run(packet).isAccepted());
        assertEquals(1, packet.handled);
    }

    @Test
    void rejectsAnOutOfRangeSlot() {
        for (int slot : new int[] { -1, 9, 127 }) {
            TestIntent packet = new TestIntent(0, slot);
            packet.heldSlot = slot;
            assertEquals(Rejection.OUT_OF_RANGE, run(packet).rejection(), "slot " + slot);
            assertEquals(0, packet.handled);
        }
    }

    @Test
    void rejectsASlotThatIsNotTheSelectedOne() {
        TestIntent packet = new TestIntent(0, 5);
        assertEquals(Rejection.OUT_OF_RANGE, run(packet).rejection());
        assertEquals(0, packet.handled);
    }

    @Test
    void rejectsWhenNoGunIsHeld() {
        TestIntent packet = new TestIntent(0, 3);
        packet.holdsGun = false;
        assertEquals(Rejection.TARGET_MISSING, run(packet).rejection());
        assertEquals(0, packet.handled);
    }

    @Test
    void rejectsADeadPlayer() {
        sender.alive = false;
        TestIntent packet = new TestIntent(0, 3);
        assertEquals(Rejection.SENDER_INACTIVE, run(packet).rejection());
        assertEquals(0, packet.handled);
    }

    @Test
    void rejectsTheWrongDimension() {
        sender.dimension = -1;
        TestIntent packet = new TestIntent(0, 3);
        assertEquals(Rejection.WRONG_DIMENSION, run(packet).rejection());
        assertEquals(0, packet.handled);
    }

    @Test
    void checkReportsSlotMismatchBeforeMissingGun() {
        assertNull(GunPacketChecks.check(2, 2, true));
        assertEquals(
            Rejection.OUT_OF_RANGE,
            GunPacketChecks.check(1, 2, false)
                .rejection());
    }

    @Test
    void doesNotReproduceGUN12_theZoomPacketCarriesNoClientFlag() {
        // Dimension (4 bytes) and slot (1 byte): nothing else, so the client cannot assert a zoom state.
        ByteBuf buf = Unpooled.buffer();
        new PacketGunZoom(0, 2).toBytes(buf);
        assertEquals(5, buf.readableBytes());
    }

    @Test
    void doesNotReproduceGUN12_theTriggerPacketCarriesOnlyTheButtonEdge() {
        ByteBuf buf = Unpooled.buffer();
        new PacketGunTrigger(0, 2, true).toBytes(buf);
        assertEquals(6, buf.readableBytes());
    }

    @Test
    void triggerPacketRoundTrips() {
        ByteBuf buf = Unpooled.buffer();
        new PacketGunTrigger(-1, 7, true).toBytes(buf);
        byte[] original = new byte[buf.readableBytes()];
        buf.getBytes(0, original);
        PacketGunTrigger decoded = new PacketGunTrigger();
        decoded.fromBytes(buf);
        ByteBuf again = Unpooled.buffer();
        decoded.toBytes(again);
        byte[] copy = new byte[again.readableBytes()];
        again.getBytes(0, copy);
        assertTrue(java.util.Arrays.equals(original, copy));
    }

    @Test
    void chargePacketRoundTripsAndIsRegistered() {
        ByteBuf buf = Unpooled.buffer();
        new PacketGunCharge(0, 2, true).toBytes(buf);
        assertEquals(6, buf.readableBytes());
        PacketGunCharge decoded = new PacketGunCharge();
        decoded.fromBytes(buf);
        ByteBuf again = Unpooled.buffer();
        decoded.toBytes(again);
        assertEquals(6, again.readableBytes());
        assertTrue(
            GregGunsNetwork.serverboundPackets()
                .stream()
                .anyMatch(p -> p instanceof PacketGunCharge));
    }

    // ----- the client-bound event ------------------------------------------------------------------------------

    private static PacketGunEvent eventPacket(int entityId, int kind, int argument) {
        ByteBuf buf = Unpooled.buffer();
        PacketBuffer out = new PacketBuffer(buf);
        out.writeInt(entityId);
        out.writeByte(kind);
        out.writeShort(argument);
        PacketGunEvent packet = new PacketGunEvent();
        packet.fromBytes(buf);
        return packet;
    }

    private PacketOutcome runEvent(PacketGunEvent packet) {
        return PacketPipeline
            .run("PacketGunEvent", "server", new ClientRules(), r -> packet.validate(r, null), () -> {}, sink);
    }

    @Test
    void doesNotReproduceCORE40_aBadKindIndexIsIgnored() {
        assertEquals(Rejection.OUT_OF_RANGE, runEvent(eventPacket(5, GunEventKind.values().length, 0)).rejection());
        assertEquals(Rejection.OUT_OF_RANGE, runEvent(eventPacket(5, 120, 0)).rejection());
    }

    @Test
    void anAbsurdReloadLengthIsIgnored() {
        assertEquals(
            Rejection.OUT_OF_RANGE,
            runEvent(eventPacket(5, GunEventKind.RELOAD_STARTED.ordinal(), PacketGunEvent.MAX_ARGUMENT + 1))
                .rejection());
    }

    @Test
    void doesNotReproduceCORE40_aMissingEntityIsIgnored() {
        // No client world in the test, so every lookup finds nothing.
        assertEquals(
            Rejection.TARGET_MISSING,
            runEvent(eventPacket(5, GunEventKind.SHOT_FIRED.ordinal(), 0)).rejection());
    }

    @Test
    void theGunPacketsAreRegistered() {
        assertTrue(
            GregGunsNetwork.clientboundPackets()
                .stream()
                .anyMatch(p -> p instanceof PacketGunEvent));
        assertTrue(
            GregGunsNetwork.serverboundPackets()
                .stream()
                .anyMatch(p -> p instanceof PacketGunTrigger));
        assertTrue(
            GregGunsNetwork.serverboundPackets()
                .stream()
                .anyMatch(p -> p instanceof PacketGunZoom));
    }
}
