package com.gregguns.armor.logic;

/**
 * Decides when armor grants or removes Night Vision, without touching a potion's. Armor refreshes a short effect every
 * tick while it is wanted. It removes the effect only when it granted it and the remaining duration is still the short
 * one; a longer effect (a brewed potion, a beacon) is never removed or refreshed.
 *
 * <p>
 * Fixes CORE-15/ARM-29: Techguns removed any Night Vision effect when its toggle went off, including a potion's. Plain
 * Java, both sides.
 */
// Spec: docs/techguns-spec/01-core/index.md CORE-15; docs/techguns-spec/05-armor/index.md ARM-29
public final class NightVisionPolicy {

    /** Duration, in ticks, of the effect armor grants and refreshes. */
    public static final int REFRESH_TICKS = 220;

    /** What to do to the player's effects. */
    public enum Action {
        /** Leave the effects alone. */
        NONE,
        /** Apply (or refresh) a {@link #REFRESH_TICKS} Night Vision effect. */
        GRANT,
        /** Remove the Night Vision effect. */
        REMOVE
    }

    /** The decision for one tick. */
    public static final class Decision {

        private final Action action;
        private final boolean owned;

        Decision(Action action, boolean owned) {
            this.action = action;
            this.owned = owned;
        }

        /** @return the action */
        public Action action() {
            return action;
        }

        /** @return whether armor owns the player's Night Vision effect after this tick */
        public boolean owned() {
            return owned;
        }
    }

    private NightVisionPolicy() {}

    /**
     * @param wanted           whether worn gear grants Night Vision
     * @param owned            the {@link Decision#owned()} of the previous tick, false at first
     * @param existingDuration remaining ticks of the player's Night Vision effect, or -1 when there is none
     * @return the action and the new ownership flag
     */
    public static Decision decide(boolean wanted, boolean owned, int existingDuration) {
        boolean none = existingDuration < 0;
        boolean ours = owned && !none && existingDuration <= REFRESH_TICKS;
        if (wanted) {
            if (none || ours) {
                return new Decision(Action.GRANT, true);
            }
            return new Decision(Action.NONE, false);
        }
        if (ours) {
            return new Decision(Action.REMOVE, false);
        }
        return new Decision(Action.NONE, false);
    }
}
