package com.gregguns.icbm.component;

/**
 * The answer of a computer call that may be refused: {@code true} when it went, otherwise {@code false} and the reason
 * a player would read on the block's screen (GGI-01-04). No Minecraft or OpenComputers types, both sides.
 */
// Spec: docs/gregguns-icbm-spec/01-core/golden-dome.md (GGI-01-04)
public final class Reply {

    private static final Reply OK = new Reply(true, "");

    private final boolean ok;
    private final String reason;

    private Reply(boolean ok, String reason) {
        this.ok = ok;
        this.reason = reason;
    }

    /** @return the success reply */
    public static Reply ok() {
        return OK;
    }

    /**
     * @param reason why the call was refused: a short machine-readable word
     * @return a refusal
     */
    public static Reply refused(String reason) {
        return new Reply(false, reason);
    }

    /** @return true when the call went */
    public boolean isOk() {
        return ok;
    }

    /** @return the refusal reason; empty on success */
    public String reason() {
        return reason;
    }
}
