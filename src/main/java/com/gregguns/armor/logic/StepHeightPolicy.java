package com.gregguns.armor.logic;

/**
 * Decides a player's step height from the step-assist bonus without ever overriding another source. GregGuns raises the
 * height only when it is lower than the assist height, remembers what it replaced, and puts back only a value it set
 * itself. Anything else (another mod's step assist, a value set after ours) is left alone.
 *
 * <p>
 * Fixes CORE-14/ARM-07: Techguns reset any step height above 0.5 to 0.5 every tick whenever its own assist was off.
 * Plain Java, both sides.
 */
// Spec: docs/techguns-spec/01-core/index.md CORE-14; docs/techguns-spec/05-armor/index.md ARM-07
public final class StepHeightPolicy {

    /** The step height step assist gives, in blocks. */
    public static final double ASSIST_HEIGHT = 1.0;

    private static final double EPS = 1e-6;

    /** What this policy remembers about one player: whether it owns the current step height, and what it replaced. */
    public static final class State {

        /** A player the policy has not touched. */
        public static final State UNTOUCHED = new State(false, 0.0);

        private final boolean owned;
        private final double previous;

        State(boolean owned, double previous) {
            this.owned = owned;
            this.previous = previous;
        }

        /** @return true when the current step height was set by this policy */
        public boolean owned() {
            return owned;
        }

        /** @return the step height this policy replaced; meaningful only when {@link #owned()} */
        public double previous() {
            return previous;
        }
    }

    /** The decision for one tick. */
    public static final class Decision {

        private final boolean set;
        private final double height;
        private final State state;

        Decision(boolean set, double height, State state) {
            this.set = set;
            this.height = height;
            this.state = state;
        }

        /** @return true when the caller must write {@link #height()} to the player */
        public boolean set() {
            return set;
        }

        /** @return the step height to write */
        public double height() {
            return height;
        }

        /** @return the state to keep for the next tick */
        public State state() {
            return state;
        }
    }

    private StepHeightPolicy() {}

    /**
     * @param current the player's step height now
     * @param wanted  whether worn gear grants step assist
     * @param state   the state returned by the previous call, {@link State#UNTOUCHED} at first
     * @return what to write (if anything) and the state to keep
     */
    public static Decision decide(double current, boolean wanted, State state) {
        if (wanted) {
            if (!state.owned) {
                if (current >= ASSIST_HEIGHT - EPS) {
                    return new Decision(false, current, State.UNTOUCHED);
                }
                return new Decision(true, ASSIST_HEIGHT, new State(true, current));
            }
            if (Math.abs(current - ASSIST_HEIGHT) < EPS) {
                return new Decision(false, current, state);
            }
            if (current < ASSIST_HEIGHT) {
                // Something lowered it (a respawn, another mod); take it back up and keep the original to restore.
                return new Decision(true, ASSIST_HEIGHT, state);
            }
            // Another source raised it higher: it is theirs now.
            return new Decision(false, current, State.UNTOUCHED);
        }
        if (!state.owned) {
            return new Decision(false, current, State.UNTOUCHED);
        }
        if (Math.abs(current - ASSIST_HEIGHT) < EPS) {
            return new Decision(true, state.previous, State.UNTOUCHED);
        }
        return new Decision(false, current, State.UNTOUCHED);
    }
}
