package com.gregguns.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.World;

import org.junit.jupiter.api.Test;

import com.gregguns.network.validation.ClientRules;
import com.gregguns.network.validation.PacketOutcome;
import com.gregguns.network.validation.PacketPipeline;
import com.gregguns.network.validation.RecordingSink;
import com.gregguns.network.validation.Rejection;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.Unpooled;

class ClientboundPacketTest {

    enum DeathType {
        GORE,
        DISSOLVE
    }

    static final class TestPacket extends ClientboundPacket {

        int ordinal = 0;
        Object entity = "entity";

        @Override
        public void encode(PacketBuffer buf) {
            buf.writeByte(ordinal);
        }

        @Override
        public void decode(PacketBuffer buf) {
            ordinal = buf.readByte();
        }

        @Override
        protected void validate(ClientRules rules, World world) {
            rules.ordinalInRange("deathType", ordinal, DeathType.class)
                .targetOfType("entity", () -> entity, CharSequence.class);
        }

        @Override
        protected void handle(World world, EntityPlayer localPlayer) {}
    }

    private final RecordingSink sink = new RecordingSink();
    private final AtomicInteger handled = new AtomicInteger();

    /** Runs the packet's own validation through the shared pipeline, with a counting stand-in for the handler. */
    private PacketOutcome run(TestPacket packet) {
        return PacketPipeline.run(
            "TestPacket",
            "server",
            new ClientRules(),
            r -> packet.validate(r, null),
            handled::incrementAndGet,
            sink);
    }

    @Test
    void acceptsAValidSync() {
        assertTrue(run(new TestPacket()).isAccepted());
        assertEquals(1, handled.get());
    }

    @Test
    void ignoresOutOfRangeEnumOrdinal() {
        TestPacket p = new TestPacket();
        p.ordinal = 2;
        assertEquals(Rejection.OUT_OF_RANGE, run(p).rejection());
        assertEquals(0, handled.get());
    }

    @Test
    void ignoresMissingOrWrongTypeEntity() {
        TestPacket missing = new TestPacket();
        missing.entity = null;
        TestPacket wrong = new TestPacket();
        wrong.entity = Integer.valueOf(4);
        assertEquals(Rejection.TARGET_MISSING, run(missing).rejection());
        assertEquals(Rejection.WRONG_TARGET_TYPE, run(wrong).rejection());
    }

    @Test
    void ignoresPacketWhenThereIsNoClientWorld() {
        PacketOutcome outcome = new TestPacket().dispatch(null, null, sink);
        assertEquals(Rejection.TARGET_MISSING, outcome.rejection());
    }

    @Test
    void corruptBytesNeverThrowAndAreIgnored() {
        TestPacket p = new TestPacket();
        p.fromBytes(Unpooled.buffer(0));
        assertEquals(
            Rejection.MALFORMED,
            p.dispatch(null, null, sink)
                .rejection());
    }

    @Test
    void doesNotReproduceCORE40_uncheckedEntityCastAndEnumIndex() {
        TestPacket p = new TestPacket();
        p.ordinal = -1;
        p.entity = Integer.valueOf(1); // an entity that is not the expected type
        PacketOutcome outcome = run(p);
        assertFalse(outcome.isAccepted());
        assertEquals(0, handled.get());
        assertEquals(Rejection.OUT_OF_RANGE, outcome.rejection());
    }

    @Test
    void clientEntryPointIsSideOnlyAndFinal() throws Exception {
        Method m = ClientboundPacket.class.getMethod("executeClient", NetHandlerPlayClient.class);
        SideOnly sideOnly = m.getAnnotation(SideOnly.class);
        assertNotNull(sideOnly);
        assertEquals(Side.CLIENT, sideOnly.value());
        assertTrue(Modifier.isFinal(m.getModifiers()));
    }
}
