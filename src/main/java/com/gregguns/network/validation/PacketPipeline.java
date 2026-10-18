package com.gregguns.network.validation;

import java.util.function.Consumer;

/**
 * The shared "validate, then handle, never throw" pipeline behind both packet base classes. Plain Java.
 *
 * Side: both.
 */
public final class PacketPipeline {

    private PacketPipeline() {}

    /**
     * Declares and runs the rules, then runs the handler only if every rule passed. Exceptions from rule
     * declaration, rule evaluation or the handler are contained and reported; nothing propagates to the caller.
     *
     * @param packet  packet name, for the log
     * @param origin  who sent it, for the log
     * @param rules   an empty rule set to fill
     * @param declare fills {@code rules}; runs inside the guard
     * @param handler the packet's own handling, run only when validation passed
     * @param sink    where rejections are reported
     * @param <R>     the rule set type
     * @return what happened
     */
    public static <R extends Rules<?>> PacketOutcome run(String packet, String origin, R rules, Consumer<R> declare,
        Runnable handler, RejectionSink sink) {
        try {
            declare.accept(rules);
            Violation violation = rules.firstViolation();
            if (violation != null) {
                sink.rejected(packet, origin, violation);
                return PacketOutcome.rejected(violation);
            }
            handler.run();
            return PacketOutcome.accepted();
        } catch (RuntimeException e) {
            sink.handlerFailed(packet, origin, e);
            return PacketOutcome.failed();
        }
    }
}
