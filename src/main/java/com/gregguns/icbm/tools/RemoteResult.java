package com.gregguns.icbm.tools;

/** What a press of the remote detonator came to. Each value has one chat line, {@code gregguns.icbm.remote.<key>}. */
public enum RemoteResult {

    /** The fuse is lit. */
    LIT("lit"),
    /** The detonator is not linked to a warhead. */
    NOT_LINKED("not_linked"),
    /** The linked warhead is in another dimension. */
    WRONG_DIMENSION("wrong_dimension"),
    /** The warhead is farther than the detonator's range. */
    OUT_OF_RANGE("out_of_range"),
    /** The warhead's chunk is not loaded; the signal never goes through it. */
    UNLOADED("unloaded"),
    /** No placed warhead stands at the linked position any more. */
    GONE("gone"),
    /** The warhead belongs to someone else. */
    NOT_OWNER("not_owner"),
    /** The fuse is already lit. */
    ALREADY_LIT("already_lit");

    private final String key;

    RemoteResult(String key) {
        this.key = key;
    }

    /** @return the lang key of the chat line */
    public String langKey() {
        return "gregguns.icbm.remote." + key;
    }
}
