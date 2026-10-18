package com.gregguns.network.validation;

/**
 * What happened to a packet in {@link PacketPipeline}.
 *
 * Side: both.
 */
public final class PacketOutcome {

    /** How the packet ended. */
    public enum Status {

        /** Validation passed and the handler ran. */
        ACCEPTED,
        /** Validation failed; the handler did not run. */
        REJECTED,
        /** Validation passed but the handler threw; the exception was contained. */
        FAILED
    }

    private final Status status;
    private final Violation violation;

    private PacketOutcome(Status status, Violation violation) {
        this.status = status;
        this.violation = violation;
    }

    /** Returns the accepted outcome. */
    public static PacketOutcome accepted() {
        return new PacketOutcome(Status.ACCEPTED, null);
    }

    /** Returns a rejected outcome. */
    public static PacketOutcome rejected(Violation violation) {
        return new PacketOutcome(Status.REJECTED, violation);
    }

    /** Returns a failed outcome. */
    public static PacketOutcome failed() {
        return new PacketOutcome(Status.FAILED, null);
    }

    /** Returns the status. */
    public Status status() {
        return status;
    }

    /** Returns {@code true} if the handler ran to completion. */
    public boolean isAccepted() {
        return status == Status.ACCEPTED;
    }

    /** Returns the violation for a rejected packet, otherwise {@code null}. */
    public Violation violation() {
        return violation;
    }

    /** Returns the rejection category, or {@code null} unless rejected. */
    public Rejection rejection() {
        return violation == null ? null : violation.rejection();
    }

    @Override
    public String toString() {
        return status + (violation == null ? "" : " " + violation);
    }
}
