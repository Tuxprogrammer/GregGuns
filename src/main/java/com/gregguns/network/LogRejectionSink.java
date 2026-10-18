package com.gregguns.network;

import org.apache.logging.log4j.Logger;

import com.gregguns.network.validation.RejectionSink;
import com.gregguns.network.validation.Violation;

/**
 * Reports dropped packets to a log. Rejections are logged at debug level (GGS-01-09); a handler failure is a bug
 * rather than an attack, so it is logged at error. {@link GregGunsNetwork} wraps this in a rate limiter.
 *
 * Side: both.
 */
final class LogRejectionSink implements RejectionSink {

    private final Logger log;

    LogRejectionSink(Logger log) {
        this.log = log;
    }

    @Override
    public void rejected(String packet, String origin, Violation violation) {
        log.debug("Dropped {} from {}: {}", packet, origin, violation);
    }

    @Override
    public void handlerFailed(String packet, String origin, RuntimeException error) {
        log.error("Handler for {} from {} threw; packet dropped", packet, origin, error);
    }

    @Override
    public void suppressed(long count) {
        log.debug("{} further packet reports suppressed by the rate limiter", count);
    }
}
