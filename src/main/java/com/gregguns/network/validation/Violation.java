package com.gregguns.network.validation;

/**
 * One failed validation rule: the {@link Rejection} category plus a short human-readable detail for the log.
 *
 * Side: both.
 */
public final class Violation {

    private final Rejection rejection;
    private final String detail;

    /**
     * @param rejection the category
     * @param detail    what was wrong, for example {@code "slot=12 not in [0, 8]"}
     */
    public Violation(Rejection rejection, String detail) {
        this.rejection = rejection;
        this.detail = detail;
    }

    /** Returns the category. */
    public Rejection rejection() {
        return rejection;
    }

    /** Returns the log detail. */
    public String detail() {
        return detail;
    }

    @Override
    public String toString() {
        return rejection + " (" + detail + ")";
    }
}
