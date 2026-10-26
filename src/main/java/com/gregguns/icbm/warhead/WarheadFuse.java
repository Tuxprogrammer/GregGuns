package com.gregguns.icbm.warhead;

/**
 * The fuse of a placed warhead, as pure logic: an int tick count (ICBM's byte overflowed above 127), one step per
 * server tick. Reuses the shape of the explosive charge fuse: a beep each second and a detonation at zero.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/warhead-blocks.md (Use as a demolition charge; GGS-ICBM-02-14)
public final class WarheadFuse {

    /** The fuse of a warhead lit by redstone or by hand. */
    public static final int FUSE_TICKS = 100;
    /** The fuse of a warhead primed by a nearby explosion. */
    public static final int CHAIN_TICKS = 10;

    /** What a step does. */
    public enum Action {
        /** Nothing. */
        NONE,
        /** Beep. */
        BEEP,
        /** Go off now. */
        DETONATE
    }

    /** The result of one step. */
    public static final class Step {

        /** Ticks left. */
        public final int remaining;
        /** What to do this tick. */
        public final Action action;

        Step(int remaining, Action action) {
            this.remaining = remaining;
            this.action = action;
        }
    }

    private WarheadFuse() {}

    /**
     * @param fuse ticks left before this tick
     * @return the new count and the action
     */
    public static Step advance(int fuse) {
        int next = Math.max(0, fuse - 1);
        if (next == 0) {
            return new Step(0, Action.DETONATE);
        }
        return new Step(next, next % 20 == 0 ? Action.BEEP : Action.NONE);
    }

    /**
     * @param current the current fuse, 0 when not armed
     * @param ticks   a requested fuse
     * @return the fuse to use: an armed warhead never gets a longer fuse than it has
     */
    public static int arm(int current, int ticks) {
        return current <= 0 ? ticks : Math.min(current, ticks);
    }
}
