package com.gregguns.worldgen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * What the first populate of a plan's overlapping windows decided: build it (with the ground Y it measured and any camp
 * flag positions) or skip it for a stated reason. Immutable; later windows obey it instead of re-deciding.
 *
 * <p>
 * A placed decision may also carry what its writer fixed at decision time, so a later config change cannot split one
 * structure between two answers (D304): a {@link #variant()} (the silo complex's activity level) and a {@link #sink()}
 * (how much deeper than its rule the complex lies, because a village will stand over it, D302).
 */
// Spec: docs/gregguns-spec/11-worldgen/structure-placement.md (GGS-11-10); PLAN-M8 Q3
public final class Decision {

    /** Y stored on a rejection, which has no ground. */
    public static final int NO_Y = -1;
    /** Variant stored when none was recorded (other structures, and complexes decided before D304). */
    public static final int NO_VARIANT = -1;

    private final boolean placed;
    private final String reason;
    private final int groundY;
    private final List<FlagPos> flags;
    private final int variant;
    private final int sink;

    private Decision(boolean placed, String reason, int groundY, List<FlagPos> flags, int variant, int sink) {
        this.placed = placed;
        this.reason = reason;
        this.groundY = groundY;
        this.flags = Collections.unmodifiableList(new ArrayList<FlagPos>(flags));
        this.variant = placed && variant >= 0 ? variant : NO_VARIANT;
        this.sink = placed ? Math.max(0, sink) : 0;
    }

    /**
     * @param groundY the Y the structure's floor sits on, decided from a column in the deciding window
     */
    public static Decision placed(int groundY) {
        return placed(groundY, NO_VARIANT, 0);
    }

    /**
     * @param groundY the Y the structure's floor sits on
     * @param variant what the writer rolled when it decided (a complex's activity level ordinal), or
     *                {@link #NO_VARIANT}; negatives become {@link #NO_VARIANT}
     * @param sink    blocks the structure lies deeper than its own depth rule, 0 or more; negatives become 0
     */
    public static Decision placed(int groundY, int variant, int sink) {
        return new Decision(true, "", groundY, Collections.<FlagPos>emptyList(), variant, sink);
    }

    /**
     * @param reason a short machine-readable tag such as {@code water} or {@code biome}; blank becomes {@code unknown}
     */
    public static Decision rejected(String reason) {
        String r = reason == null || reason.trim()
            .isEmpty() ? "unknown" : reason;
        return new Decision(false, r, NO_Y, Collections.<FlagPos>emptyList(), NO_VARIANT, 0);
    }

    /** Rebuilds a placed decision with everything it recorded, for the serializer. */
    static Decision placed(int groundY, List<FlagPos> flags, int variant, int sink) {
        return new Decision(true, "", groundY, flags, variant, sink);
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

    /** @return the variant recorded at decision time, or {@link #NO_VARIANT} (always when rejected) */
    public int variant() {
        return variant;
    }

    /** @return blocks the structure lies deeper than its depth rule; 0 when rejected or not sunk */
    public int sink() {
        return sink;
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
        return new Decision(true, "", groundY, next, variant, sink);
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
        return placed == d.placed && groundY == d.groundY
            && variant == d.variant
            && sink == d.sink
            && reason.equals(d.reason)
            && flags.equals(d.flags);
    }

    @Override
    public int hashCode() {
        return (((placed ? 1 : 0) * 31 + groundY) * 31 + variant) * 31 + sink + reason.hashCode() + flags.hashCode();
    }

    @Override
    public String toString() {
        return placed ? "placed y=" + groundY + " flags=" + flags + " variant=" + variant + " sink=" + sink
            : "rejected " + reason;
    }
}
