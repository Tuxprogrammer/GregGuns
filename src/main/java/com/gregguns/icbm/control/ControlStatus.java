package com.gregguns.icbm.control;

/**
 * Everything the launch-control block can say about a shot, from "ready" to each reason it refuses. One lang key per
 * value, {@code gregguns.icbm.control.status.<name lowercased>}, so no raw key ever shows (ICBM-04-16, 04-17). No
 * Minecraft types, both sides.
 */
// Spec: docs/gregguns-icbm-spec/04-launch-control/launch-sequence.md (Pre-check)
public enum ControlStatus {

    /** Everything checks out. */
    READY(true),
    /** No target is set (ICBM-04-06). */
    NO_TARGET(false),
    /** No Galacticraft launch pad within reach. */
    NO_PAD(false),
    /** The block holds less than one shot of EU. */
    NO_POWER(false),
    /** The pad holds no missile. */
    NO_MISSILE(false),
    /** A body and warhead are stacked but not armed. */
    NOT_ARMED(false),
    /** The tank is not full. */
    NOT_FUELED(false),
    /** The target is inside the minimum range. */
    TOO_CLOSE(false),
    /** The target is beyond the missile's range. */
    OUT_OF_RANGE(false),
    /** The in-flight cap is reached. */
    OVER_CAP(false),
    /** The target is not a valid coordinate. */
    BAD_TARGET(false),
    /** The missile is already flying, or the chunk is outside the border. */
    BUSY(false),
    /** A roof or solid block stands in the climb column above the pad. */
    BLOCKED(false),
    /** Forge refused the missile a chunk ticket. */
    NO_TICKET(false),
    SILO_TIER(false),
    /** A silo-only body stands on a pad that is not a formed silo (D8 step 5, D70). */
    SILO_ONLY(false),
    /** The block is cooling down from the last shot. */
    COOLDOWN(false),
    /** A countdown is running; it can be aborted. */
    COUNTDOWN(true),
    /** The engines have ignited; there is no abort. */
    IGNITED(true),
    /** The missile was launched. */
    FIRED(true);

    private final boolean good;

    ControlStatus(boolean good) {
        this.good = good;
    }

    /** @return true for {@link #READY} and {@link #FIRED}, which the GUI shows in a good color */
    public boolean isGood() {
        return good;
    }

    /** @return the lang key of the status line */
    public String langKey() {
        return "gregguns.icbm.control.status." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
