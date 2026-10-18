package com.gregguns.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.PacketBuffer;

import org.junit.jupiter.api.Test;

import com.gregguns.network.validation.FakeSender;
import com.gregguns.network.validation.PacketOutcome;
import com.gregguns.network.validation.RecordingSink;
import com.gregguns.network.validation.Rejection;
import com.gregguns.network.validation.Sender;
import com.gregguns.network.validation.SenderRules;
import com.gtnewhorizon.gtnhlib.network.base.IPacket;

import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

class ServerboundPacketTest {

    /** A packet that opts into every rule, with fields set directly. */
    static class TestPacket extends ServerboundPacket {

        int slot = 0;
        int dimension = 0;
        int bx = 0;
        int by = 0;
        int bz = 0;
        Object target = "tile";
        boolean authorised = true;
        final List<String> order = new ArrayList<>();

        @Override
        public void encode(PacketBuffer buf) {
            buf.writeInt(slot);
        }

        @Override
        public void decode(PacketBuffer buf) {
            slot = buf.readInt();
        }

        @Override
        protected void validate(SenderRules rules) {
            order.add("validate");
            rules.sameDimension(dimension)
                .withinReachOfBlock(bx, by, bz)
                .indexInRange("slot", slot, 9)
                .targetOfType("tile", () -> target, CharSequence.class);
        }

        @Override
        protected void handle(EntityPlayerMP player) {}

        @Override
        protected boolean isAuthorised(Sender sender) {
            order.add("authorise");
            return authorised;
        }
    }

    private final FakeSender sender = new FakeSender();
    private final RecordingSink sink = new RecordingSink();
    private final AtomicInteger handled = new AtomicInteger();

    private PacketOutcome run(ServerboundPacket packet) {
        return packet.dispatch(sender, handled::incrementAndGet, sink);
    }

    @Test
    void acceptsAValidRequestAndRunsTheHandler() {
        assertTrue(run(new TestPacket()).isAccepted());
        assertEquals(1, handled.get());
    }

    @Test
    void rejectsOutOfRangeIndex() {
        TestPacket p = new TestPacket();
        p.slot = 9;
        assertEquals(Rejection.OUT_OF_RANGE, run(p).rejection());
        assertEquals(0, handled.get());
    }

    @Test
    void rejectsWrongDimension() {
        TestPacket p = new TestPacket();
        p.dimension = 7;
        assertEquals(Rejection.WRONG_DIMENSION, run(p).rejection());
        assertEquals(0, handled.get());
    }

    @Test
    void rejectsOutOfReach() {
        TestPacket p = new TestPacket();
        p.bx = 100;
        assertEquals(Rejection.OUT_OF_REACH, run(p).rejection());
    }

    @Test
    void rejectsMissingAndWrongTypeTargets() {
        TestPacket missing = new TestPacket();
        missing.target = null;
        TestPacket wrong = new TestPacket();
        wrong.target = Integer.valueOf(1);
        assertEquals(Rejection.TARGET_MISSING, run(missing).rejection());
        assertEquals(Rejection.WRONG_TARGET_TYPE, run(wrong).rejection());
    }

    @Test
    void rejectsUnauthorisedSender() {
        TestPacket p = new TestPacket();
        p.authorised = false;
        assertEquals(Rejection.UNAUTHORISED, run(p).rejection());
        assertEquals(0, handled.get());
    }

    @Test
    void authorisationRunsAfterTargetValidation() {
        TestPacket p = new TestPacket();
        run(p);
        assertEquals(Arrays.asList("validate", "authorise"), p.order);
        TestPacket bad = new TestPacket();
        bad.slot = -5;
        run(bad);
        assertEquals(Arrays.asList("validate"), bad.order, "owner checks must not run against an invalid target");
    }

    @Test
    void rejectsDeadSenderEvenWhenThePacketAddsNoSenderRules() {
        sender.alive = false;
        assertEquals(Rejection.SENDER_INACTIVE, run(new TestPacket()).rejection());
        assertEquals(0, handled.get());
    }

    @Test
    void rejectsSpectatorLikeSender() {
        sender.spectatorLike = true;
        assertEquals(Rejection.SENDER_SPECTATOR, run(new TestPacket()).rejection());
    }

    @Test
    void validatorExceptionIsContainedAndHandlerSkipped() {
        TestPacket p = new TestPacket() {

            @Override
            protected void validate(SenderRules rules) {
                throw new IllegalStateException("bug in a validator");
            }
        };
        assertEquals(PacketOutcome.Status.FAILED, run(p).status());
        assertEquals(0, handled.get());
    }

    @Test
    void handlerExceptionIsContained() {
        PacketOutcome outcome = new TestPacket()
            .dispatch(sender, () -> { throw new IllegalStateException("handler bug"); }, sink);
        assertEquals(PacketOutcome.Status.FAILED, outcome.status());
        assertEquals(1, sink.failures.size());
    }

    @Test
    void truncatedPacketBytesNeverThrowIntoNettyAndAreDropped() {
        TestPacket p = new TestPacket();
        // readInt on an empty buffer throws IndexOutOfBoundsException; fromBytes must swallow it into a drop.
        p.fromBytes(Unpooled.buffer(0));
        assertEquals(Rejection.MALFORMED, run(p).rejection());
        assertEquals(0, handled.get());
    }

    @Test
    void wellFormedBytesDecodeAndAreAccepted() {
        TestPacket source = new TestPacket();
        source.slot = 4;
        ByteBuf buf = Unpooled.buffer();
        source.toBytes(buf);
        TestPacket decoded = new TestPacket();
        decoded.fromBytes(buf);
        assertEquals(4, decoded.slot);
        assertTrue(run(decoded).isAccepted());
    }

    @Test
    void rejectionsAreReportedToTheSink() {
        TestPacket p = new TestPacket();
        p.slot = 99;
        run(p);
        assertEquals(1, sink.rejections.size());
        assertTrue(sink.failures.isEmpty());
    }

    @Test
    void doesNotReproduceCORE05_threadingIsLeftToGtnhlibAndEntryPointIsFinal() throws Exception {
        // GTNHLib's IPacket.onMessage reschedules onto the main thread before executeServer. The base class must not
        // replace it, and subclasses must not be able to bypass the validated entry point.
        Method onMessage = ServerboundPacket.class.getMethod("onMessage", IPacket.class, MessageContext.class);
        assertEquals(IPacket.class, onMessage.getDeclaringClass());
        Method executeServer = ServerboundPacket.class.getMethod("executeServer", NetHandlerPlayServer.class);
        assertTrue(Modifier.isFinal(executeServer.getModifiers()));
        assertEquals(ServerboundPacket.class, executeServer.getDeclaringClass());
    }

    @Test
    void doesNotReproduceCORE06_arbitraryGuiIdIsRejectedBeforeAnyWorldAccess() {
        TestPacket p = new TestPacket();
        p.slot = 12345; // a GUI id nothing defines
        assertEquals(Rejection.OUT_OF_RANGE, run(p).rejection());
        assertEquals(0, handled.get());
    }

    @Test
    void doesNotReproduceCORE08_distantCoordinatesAndMissingTilesAreRejected() {
        TestPacket far = new TestPacket();
        far.bx = 30_000_000;
        far.target = null; // reach fails first, so the target lookup (and any chunk load) never happens
        assertEquals(Rejection.OUT_OF_REACH, run(far).rejection());
        TestPacket missing = new TestPacket();
        missing.target = null;
        assertEquals(Rejection.TARGET_MISSING, run(missing).rejection());
    }
}
