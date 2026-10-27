package com.gregguns.icbm.tools;

/** What a use of the defuser came to. Each value has one chat line, {@code gregguns.icbm.defuser.<key>}. */
public enum DefuseResult {

    /** A lit warhead fuse was put out. */
    FUSE_OUT("fuse_out"),
    /** An unlaunched missile was taken apart. */
    MISSILE_DISARMED("missile_disarmed"),
    /** The warhead is not lit, so there is nothing to defuse. */
    NOT_LIT("not_lit"),
    /** The missile's countdown is running: abort it at the Launch Control first. */
    COUNTING_DOWN("counting_down"),
    /** The missile has ignited or flies: too late. */
    IGNITED("ignited"),
    /** The target is not something the defuser works on. */
    NOT_A_TARGET("not_a_target");

    private final String key;

    DefuseResult(String key) {
        this.key = key;
    }

    /** @return the lang key of the chat line */
    public String langKey() {
        return "gregguns.icbm.defuser." + key;
    }

    /** @return true when the use succeeded and costs a use of the tool */
    public boolean success() {
        return this == FUSE_OUT || this == MISSILE_DISARMED;
    }
}
