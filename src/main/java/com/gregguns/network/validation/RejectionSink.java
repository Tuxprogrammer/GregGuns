package com.gregguns.network.validation;

/**
 * Where dropped packets are reported. The real implementation logs at debug level and rate-limits, so a hostile
 * client cannot flood the log.
 *
 * Side: both.
 */
public interface RejectionSink {

    /** A packet failed validation and was dropped. */
    void rejected(String packet, String origin, Violation violation);

    /** A validated packet's handler threw; the exception was contained. */
    void handlerFailed(String packet, String origin, RuntimeException error);

    /** The rate limiter swallowed {@code count} reports since the last one. */
    void suppressed(long count);
}
