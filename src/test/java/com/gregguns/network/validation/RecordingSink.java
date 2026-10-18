package com.gregguns.network.validation;

import java.util.ArrayList;
import java.util.List;

/** A {@link RejectionSink} that remembers what it was told. */
public final class RecordingSink implements RejectionSink {

    public final List<Violation> rejections = new ArrayList<>();
    public final List<RuntimeException> failures = new ArrayList<>();
    public long suppressedTotal;

    @Override
    public void rejected(String packet, String origin, Violation violation) {
        rejections.add(violation);
    }

    @Override
    public void handlerFailed(String packet, String origin, RuntimeException error) {
        failures.add(error);
    }

    @Override
    public void suppressed(long count) {
        suppressedTotal += count;
    }
}
