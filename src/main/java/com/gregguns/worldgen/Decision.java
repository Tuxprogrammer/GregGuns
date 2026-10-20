package com.gregguns.worldgen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * What the first populate of a plan's overlapping windows decided: build it (with the ground Y it measured and any camp
 * flag positions) or skip it for a stated reason. Immutable; later windows obey it instead of re-deciding.
 */
// Spec: docs/gregguns-spec/11-worldgen/structure-placement.md (GGS-11-10); PLAN-M8 Q3
public final class Decision {

    /** Y stored on a rejection, which has no ground. */
    public static final int NO_Y = -1;

    private final boolean placed;
    private final String reason;
    private final int groundY;
    private final List<FlagPos> flags;

    private Decision(boolean placed, String reason, int groundY, List<FlagPos> flags) {
        this.placed = placed;
        this.reason = reason;
        this.groundY = groundY;
        this.flags = Collections.unmodifiableList(new ArrayList<FlagPos>(flags));
    }

    /**
     * @param groundY the Y the structure's floor sits on, decided from a column in the deciding window
     */
    public static Decision placed(int groundY) {
        return new Decision(true, "", groundY, Collections.<FlagPos>emptyList());
    }

    /**
     * @param reason a short machine-readable tag such as {@code water} or {@code biome}; blank becomes {@code unknown}
     */
    public static Decision rejected(String reason) {
        String r = reason == null || reason.trim()
            .isEmpty() ? "unknown" : reason;
        return new Decision(false, r, NO_Y, Collections.<FlagPos>emptyList());
    }

    /** Rebuilds a placed decision with its flags, for the serializer. */
    static Decision placed(int groundY, List<FlagPos> flags) {
        return new Decision(true, "", groundY, flags);
    }

    public boolean isPlaced() {
        return placed;
    }

    /** Empty when placed. */
    public String reason() {
        return reason;
    }

    /** {@link #NO_Y} when rejected. */
    public int groundY() {
        return groundY;
    }

    /** Camp flag positions, in the order they were recorded; unmodifiable. */
    public List<FlagPos> flags() {
        return flags;
    }

    /** A copy with one more flag. A rejected decision, or a flag already present, returns this. */
    Decision withFlag(FlagPos pos) {
        if (!placed || flags.contains(pos)) {
            return this;
        }
        List<FlagPos> next = new ArrayList<FlagPos>(flags);
        next.add(pos);
        return new Decision(true, "", groundY, next);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Decision)) {
            return false;
        }
        Decision d = (Decision) o;
        return placed == d.placed && groundY == d.groundY && reason.equals(d.reason) && flags.equals(d.flags);
    }

    @Override
    public int hashCode() {
        return ((placed ? 1 : 0) * 31 + groundY) * 31 + reason.hashCode() + flags.hashCode();
    }

    @Override
    public String toString() {
        return placed ? "placed y=" + groundY + " flags=" + flags : "rejected " + reason;
    }
}
