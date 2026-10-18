package com.gregguns.network.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class PacketPipelineTest {

    private final RecordingSink sink = new RecordingSink();
    private final AtomicInteger handled = new AtomicInteger();

    @Test
    void validPacketRunsHandlerOnce() {
        PacketOutcome outcome = PacketPipeline
            .run("P", "tester", new ClientRules(), r -> r.indexInRange("i", 1, 3), handled::incrementAndGet, sink);
        assertTrue(outcome.isAccepted());
        assertEquals(1, handled.get());
        assertTrue(sink.rejections.isEmpty());
    }

    @Test
    void invalidPacketNeverReachesHandlerAndIsReported() {
        PacketOutcome outcome = PacketPipeline
            .run("P", "tester", new ClientRules(), r -> r.indexInRange("i", 7, 3), handled::incrementAndGet, sink);
        assertEquals(PacketOutcome.Status.REJECTED, outcome.status());
        assertEquals(Rejection.OUT_OF_RANGE, outcome.rejection());
        assertEquals(0, handled.get());
        assertEquals(1, sink.rejections.size());
        assertSame(outcome.violation(), sink.rejections.get(0));
    }

    @Test
    void handlerExceptionIsContainedNotThrown() {
        PacketOutcome outcome = PacketPipeline
            .run("P", "tester", new ClientRules(), r -> {}, () -> { throw new IllegalStateException("boom"); }, sink);
        assertEquals(PacketOutcome.Status.FAILED, outcome.status());
        assertFalse(outcome.isAccepted());
        assertEquals(1, sink.failures.size());
    }

    @Test
    void ruleDeclarationAndLookupExceptionsAreContainedAndHandlerSkipped() {
        PacketOutcome declareFails = PacketPipeline.run(
            "P",
            "tester",
            new ClientRules(),
            r -> { throw new NullPointerException("bad declaration"); },
            handled::incrementAndGet,
            sink);
        PacketOutcome lookupFails = PacketPipeline.run(
            "P",
            "tester",
            new ClientRules(),
            r -> r.targetExists("t", () -> { throw new IllegalStateException("lookup"); }),
            handled::incrementAndGet,
            sink);
        assertEquals(PacketOutcome.Status.FAILED, declareFails.status());
        assertEquals(PacketOutcome.Status.FAILED, lookupFails.status());
        assertEquals(0, handled.get());
    }
}
