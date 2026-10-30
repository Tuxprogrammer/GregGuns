package com.gregguns.icbm.control;

import java.util.List;

/**
 * Picks one launch site when a free-standing Launch Control touches more than one pad: the one with a launch under way
 * (so Abort and the countdown follow it), then one whose missile is ready (armed and full), then one that is only
 * armed, then one with anything standing on it, then an empty one. Among equals the first in the caller's order wins,
 * and the caller passes them in {@link PadAdjacency#CENTER_OFFSETS} order, so the choice never flips between two
 * equal pads. No Minecraft types, both sides.
 */
// Spec: docs/gregguns-icbm-spec/04-launch-control/launch-control-block.md (Finding the pad or the silo)
public final class SiteChoice {

    /** A launch is counting down or climbing out. */
    public static final int IN_PROGRESS = 4;
    /** An armed missile with a full tank stands there. */
    public static final int READY = 3;
    /** An armed missile stands there, not yet full. */
    public static final int ARMED = 2;
    /** Something stands there: an unarmed stack or body. */
    public static final int PRESENT = 1;
    /** Nothing stands there. */
    public static final int EMPTY = 0;

    private SiteChoice() {}

    /**
     * @param inProgress whether a countdown runs or the missile is climbing out
     * @param present    whether a missile or a stack stands there
     * @param armed      whether it is armed
     * @param fueled     whether its tank is full
     * @return the site's rank, higher is preferred
     */
    public static int rank(boolean inProgress, boolean present, boolean armed, boolean fueled) {
        if (inProgress) {
            return IN_PROGRESS;
        }
        if (!present) {
            return EMPTY;
        }
        if (!armed) {
            return PRESENT;
        }
        return fueled ? READY : ARMED;
    }

    /**
     * @param ranks the rank of each candidate, in the caller's fixed order
     * @return the index of the chosen candidate: the highest rank, the first among equals; -1 for an empty list
     */
    public static int best(List<Integer> ranks) {
        int best = -1;
        int bestRank = Integer.MIN_VALUE;
        for (int i = 0; i < ranks.size(); i++) {
            int r = ranks.get(i);
            if (r > bestRank) {
                bestRank = r;
                best = i;
            }
        }
        return best;
    }
}
