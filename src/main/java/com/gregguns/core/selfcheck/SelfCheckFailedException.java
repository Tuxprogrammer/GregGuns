package com.gregguns.core.selfcheck;

/**
 * Thrown by the dev self-check to stop the dev server; the message lists every violation. Both sides.
 */
public final class SelfCheckFailedException extends IllegalStateException {

    private static final long serialVersionUID = 1L;

    /**
     * @param message every violation, one per line
     */
    public SelfCheckFailedException(String message) {
        super(message);
    }
}
